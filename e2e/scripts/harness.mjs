#!/usr/bin/env node
// @ts-check
/**
 * Harness-owned test lifecycle for the Hospital Admissions prototype.
 *
 * WHY PROCESS-LEVEL RESET:
 *   The app runs the `prototype` Spring profile with an in-memory H2 database
 *   (`ddl-auto: create-drop`) that is deterministically reseeded by
 *   DataInitializer on every JVM boot. There is NO in-app reset endpoint, and
 *   a story spec must never call reset/hidden APIs itself. The only sound,
 *   harness-owned reset is therefore process-level: boot a fresh backend JVM on
 *   a unique free port (a uniquely identified lifecycle), which yields a known
 *   seed baseline, then tear down only that process at the end.
 *
 * CONTRACT (per foundation.md section 0.2):
 *   - start: allocate a unique port + lifecycle id, launch the JAR, wait for
 *            /actuator/health = UP, run a harness-owned READ-BACK check that the
 *            declared seed baseline is present, then print baseURL + lifecycleId.
 *   - stop:  tear down ONLY the process this harness created (by recorded pid),
 *            and remove only the lifecycle record it wrote.
 *   - status: report on a recorded lifecycle.
 *   State-changing commands exit non-zero only on a REAL failure; benign tool
 *   diagnostics (e.g. an already-dead pid on stop) are reported but not fatal.
 *
 * This script resets only its designated in-memory test data (the fresh JVM's
 * own H2 instance). It touches no shared/developer data and no external API.
 *
 * Usage:
 *   node e2e/scripts/harness.mjs start [--json]
 *   node e2e/scripts/harness.mjs stop --lifecycle <id> | --port <port> [--json]
 *   node e2e/scripts/harness.mjs status --lifecycle <id> [--json]
 */
import { spawn } from 'node:child_process';
import { createServer } from 'node:net';
import { setTimeout as delay } from 'node:timers/promises';
import { mkdirSync, writeFileSync, readFileSync, rmSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(__dirname, '..', '..');
const BACKEND_DIR = path.join(REPO_ROOT, 'backend');
const JAR = path.join(BACKEND_DIR, 'target', 'admissions-0.0.1-SNAPSHOT.jar');
// Lifecycle bookkeeping lives under generated (git-ignored) evidence, never in shared source.
const RUN_DIR = path.join(REPO_ROOT, 'artifacts', 'e2e', '.lifecycles');
const HEALTH_TIMEOUT_MS = 90_000;
const HEALTH_POLL_MS = 1_000;

/** Parse `--flag value` and `--flag` style args. */
function parseArgs(argv) {
  const out = {};
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a.startsWith('--')) {
      const key = a.slice(2);
      const next = argv[i + 1];
      if (next && !next.startsWith('--')) { out[key] = next; i++; } else { out[key] = true; }
    }
  }
  return out;
}

/** Reserve a free TCP port by opening then closing an ephemeral server. */
function findFreePort() {
  return new Promise((resolve, reject) => {
    const srv = createServer();
    srv.on('error', reject);
    srv.listen(0, '127.0.0.1', () => {
      const { port } = /** @type {import('node:net').AddressInfo} */ (srv.address());
      srv.close(() => resolve(port));
    });
  });
}

function newLifecycleId() {
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  const rand = Math.random().toString(36).slice(2, 8);
  return `lc-${stamp}-${rand}`;
}

function lifecycleFile(id) {
  return path.join(RUN_DIR, `${id}.json`);
}

async function httpText(url) {
  const res = await fetch(url, { headers: { 'X-User-Role': 'ED_ATTENDING' } });
  return { status: res.status, body: await res.text() };
}

/** Poll /actuator/health until UP or timeout. */
async function waitForHealth(baseUrl) {
  const deadline = Date.now() + HEALTH_TIMEOUT_MS;
  let last = 'no response';
  while (Date.now() < deadline) {
    try {
      const { status, body } = await httpText(`${baseUrl}/actuator/health`);
      if (status === 200 && /"status"\s*:\s*"UP"/.test(body)) return true;
      last = `status=${status} body=${body.slice(0, 120)}`;
    } catch (err) {
      last = /** @type {Error} */ (err).message;
    }
    await delay(HEALTH_POLL_MS);
  }
  throw new Error(`health never reached UP within ${HEALTH_TIMEOUT_MS}ms (last: ${last})`);
}

/**
 * Harness-owned READ-BACK of the declared state: the fresh JVM must have
 * reseeded the known baseline. We assert the deterministic seed is present
 * (seeded patient token Q-P101 exists) rather than trusting health alone.
 * `/api/v1/patients/tokens` lists every seeded patient token, so it is a
 * stable baseline probe independent of per-workflow status filtering.
 */
async function readBackSeedBaseline(baseUrl) {
  const res = await fetch(`${baseUrl}/api/v1/patients/tokens`, {
    headers: { 'X-User-Role': 'PATIENT' },
  });
  const body = await res.text();
  if (res.status !== 200) throw new Error(`seed read-back failed: HTTP ${res.status}`);
  if (!body.includes('Q-P101')) {
    throw new Error('seed read-back failed: expected seeded patient token Q-P101 not found');
  }
  return true;
}

function ensureJarExists() {
  if (!existsSync(JAR)) {
    throw new Error(
      `Backend JAR not found at ${JAR}. Build it first:\n` +
      `  (cd backend && ./mvnw clean package -DskipTests)`
    );
  }
}

async function cmdStart(args) {
  ensureJarExists();
  mkdirSync(RUN_DIR, { recursive: true });
  const port = args.port ? Number(args.port) : await findFreePort();
  const lifecycleId = newLifecycleId();
  const baseUrl = `http://127.0.0.1:${port}`;

  const child = spawn(
    'java',
    [
      `-Dserver.port=${port}`,
      '-Dspring.profiles.active=prototype',
      '-jar', JAR,
    ],
    { cwd: BACKEND_DIR, stdio: ['ignore', 'pipe', 'pipe'], detached: true }
  );

  const logPath = path.join(RUN_DIR, `${lifecycleId}.log`);
  const logChunks = [];
  child.stdout.on('data', (d) => logChunks.push(d));
  child.stderr.on('data', (d) => logChunks.push(d));

  const record = {
    lifecycleId,
    pid: child.pid,
    port,
    baseUrl,
    profile: 'prototype',
    startupMode: 'single-jar',
    jar: path.relative(REPO_ROOT, JAR),
    startedAt: new Date().toISOString(),
  };
  writeFileSync(lifecycleFile(lifecycleId), JSON.stringify(record, null, 2));

  let exited = false;
  child.on('exit', (code) => {
    exited = true;
    try { writeFileSync(logPath, Buffer.concat(logChunks)); } catch { /* best effort */ }
    if (code !== 0 && code !== null) {
      // real failure surfaces via health wait below
    }
  });

  try {
    await waitForHealth(baseUrl);
    await readBackSeedBaseline(baseUrl);
  } catch (err) {
    try { writeFileSync(logPath, Buffer.concat(logChunks)); } catch { /* best effort */ }
    // Real failure: tear down what we started before failing closed, and
    // remove our own lifecycle record so no dangling state leaks.
    if (child.pid && !exited) { try { process.kill(-child.pid); } catch { /* ignore */ } }
    try { rmSync(lifecycleFile(lifecycleId), { force: true }); } catch { /* ignore */ }
    throw new Error(`lifecycle ${lifecycleId} failed to become ready: ${/** @type {Error} */ (err).message}. Log: ${logPath}`);
  }

  // Detach so the JVM keeps running after this CLI invocation returns.
  child.unref();
  return record;
}

function cmdStop(args) {
  let record;
  if (args.lifecycle) {
    const f = lifecycleFile(String(args.lifecycle));
    if (!existsSync(f)) {
      // Benign diagnostic: nothing to stop. Not a real failure.
      return { stopped: false, reason: 'no such lifecycle record', lifecycleId: args.lifecycle };
    }
    record = JSON.parse(readFileSync(f, 'utf8'));
  } else {
    throw new Error('stop requires --lifecycle <id>');
  }

  let killed = false;
  if (record.pid) {
    try {
      // Kill the detached process group we created.
      process.kill(-record.pid, 'SIGTERM');
      killed = true;
    } catch (err) {
      const code = /** @type {NodeJS.ErrnoException} */ (err).code;
      if (code === 'ESRCH') {
        // Already gone — benign, not a real failure.
        killed = false;
      } else {
        try { process.kill(record.pid, 'SIGTERM'); killed = true; } catch { /* ignore */ }
      }
    }
  }
  // Remove only the lifecycle record this harness wrote.
  try { rmSync(lifecycleFile(record.lifecycleId), { force: true }); } catch { /* ignore */ }
  return { stopped: true, killedProcess: killed, lifecycleId: record.lifecycleId, port: record.port };
}

async function cmdStatus(args) {
  if (!args.lifecycle) throw new Error('status requires --lifecycle <id>');
  const f = lifecycleFile(String(args.lifecycle));
  if (!existsSync(f)) return { lifecycleId: args.lifecycle, exists: false };
  const record = JSON.parse(readFileSync(f, 'utf8'));
  let health = 'unknown';
  try {
    const { status, body } = await httpText(`${record.baseUrl}/actuator/health`);
    health = status === 200 && /"UP"/.test(body) ? 'UP' : `status=${status}`;
  } catch (err) {
    health = `unreachable: ${/** @type {Error} */ (err).message}`;
  }
  return { ...record, exists: true, health };
}

async function main() {
  const [, , sub, ...rest] = process.argv;
  const args = parseArgs(rest);
  const json = Boolean(args.json);
  try {
    let result;
    switch (sub) {
      case 'start': result = await cmdStart(args); break;
      case 'stop': result = cmdStop(args); break;
      case 'status': result = await cmdStatus(args); break;
      default:
        process.stderr.write(
          'Usage: harness.mjs <start|stop|status> [--json] [--lifecycle <id>] [--port <port>]\n'
        );
        process.exit(2);
    }
    if (json) {
      process.stdout.write(JSON.stringify(result) + '\n');
    } else if (sub === 'start') {
      process.stdout.write(
        `lifecycleId=${result.lifecycleId} baseUrl=${result.baseUrl} pid=${result.pid} startupMode=${result.startupMode}\n`
      );
    } else {
      process.stdout.write(JSON.stringify(result, null, 2) + '\n');
    }
    process.exit(0);
  } catch (err) {
    process.stderr.write(`ERROR: ${/** @type {Error} */ (err).message}\n`);
    process.exit(1);
  }
}

main();
