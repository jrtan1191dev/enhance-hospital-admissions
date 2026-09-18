#!/usr/bin/env node
/**
 * record-runs.mjs — execute the film's terminal evidence for real and freeze it.
 *
 * WHY THIS EXISTS (supersedes D18)
 * --------------------------------
 * The storyline cannot express a `vhs_terminal` scene, so the plan routed terminal
 * takes through `footage` + VHS-produced mp4s. VHS is installed here (0.12.0, with
 * ttyd 1.7.7) and **silently produces nothing**: it exits 0, prints "Creating
 * <file>...", never invokes ffmpeg (verified with an ffmpeg shim: zero calls) and
 * writes no file in any format, including raw frames. Its browser phase fails and
 * the error is swallowed.
 *
 * So the terminal takes are produced the same way as the seam diagram: real output,
 * captured once, rendered by a page we own, filmed by Playwright.
 *
 * WHAT MAKES THIS HONEST
 * ----------------------
 * The bytes on screen are the unmodified stdout/stderr of a command that actually
 * ran, together with its exit code and the wall-clock duration. Nothing is
 * hand-written, and a failing command stays failing — 09e's coverage gate is
 * recorded red because it IS red. What is synthetic is only the presentation: the
 * lines are revealed on a timer instead of being typed live. That is a weaker
 * artifact than a screen recording of a live shell, and the plan says so out loud.
 *
 * The JSON files are committed. They are the frozen input, exactly as a generated
 * footage file would have been, and they are what makes the film re-renderable
 * without a working VHS.
 *
 * Usage (from the repository root):
 *   node video-generation-technical/tools/record-runs.mjs            # all runs
 *   node video-generation-technical/tools/record-runs.mjs 09b-audit-log
 *
 * ORDERING: 09b-audit-log must be recorded AFTER capture.mjs has driven the
 * workflows, because a freshly started JVM has zero audit lines. Everything else is
 * order-independent.
 */

import { execFile } from 'node:child_process';
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

const ROOT = path.resolve(import.meta.dirname, '..', '..');
const OUT = path.resolve(import.meta.dirname, '..', 'pages', 'runs');

/**
 * Every command here was dry-run verified on 18 Sep 2026 before being committed.
 * `cwd` is repository-root-relative.
 */
const RUNS = [
  {
    id: '05b-profile-fence',
    title: 'The production profile refuses everything but the health check',
    steps: [
      { cmd: "grep -m1 'profile is active' logs/app-production.log" },
      { cmd: "curl -s -o /dev/null -w 'health          -> %{http_code}\\n' http://127.0.0.1:8081/actuator/health" },
      { cmd: "curl -s -o /dev/null -w 'GET /api/v1/bmu/queue -> %{http_code}\\n' http://127.0.0.1:8081/api/v1/bmu/queue" },
      { cmd: "curl -s -o /dev/null -w 'GET / (the app itself) -> %{http_code}\\n' http://127.0.0.1:8081/" },
    ],
  },
  {
    id: '05c-stub-contract',
    title: 'The three production adapters are asserted to fail fast',
    steps: [
      { cmd: `grep -h '@DisplayName' backend/src/test/java/com/hospital/admissions/gateway/GatewaysAndSolversTest.java | sed 's/.*("/  - /; s/")//'` },
      { cmd: "cd backend && ./mvnw test -Dtest=GatewaysAndSolversTest 2>&1 | grep -E 'Tests run:|BUILD'" },
    ],
  },
  {
    id: '06c-solver-pinned',
    title: 'The heuristic is pinned by tests, which is what makes it replaceable',
    steps: [
      { cmd: "cd backend && ./mvnw test -Dtest=HeuristicBedAllocationSolverTest 2>&1 | grep -E 'Tests run:|BUILD'" },
    ],
  },
  {
    id: '07a-absolute-tier',
    title: 'The absolute tier is proved by elimination, not by a screenshot',
    steps: [
      { cmd: `grep -h '@DisplayName' backend/src/test/java/com/hospital/admissions/solver/HeuristicBedAllocationSolverTest.java | sed 's/.*("/  - /; s/")//'` },
      { cmd: "cd backend && ./mvnw test -Dtest=HeuristicBedAllocationSolverTest 2>&1 | grep -E 'Tests run:'" },
    ],
  },
  {
    id: '08b-conflict-409',
    title: 'Optimistic locking, proved where the UI cannot honestly stage it',
    steps: [
      { cmd: `grep -h '@DisplayName' backend/src/test/java/com/hospital/admissions/controller/ClinicianControllerTest.java backend/src/test/java/com/hospital/admissions/controller/GlobalExceptionHandlerTest.java | grep -i 'conflict' | sed 's/.*("/  - /; s/")//'` },
      { cmd: "cd backend && ./mvnw test -Dtest='ClinicianControllerTest#testClaimBroadcast_ConflictReturns409,GlobalExceptionHandlerTest' 2>&1 | grep -E 'Tests run:|BUILD'" },
    ],
  },
  {
    id: '09b-audit-log',
    title: 'Pathway B: the structured audit log, with the acting user on every line',
    steps: [
      { cmd: "grep -c AUDIT logs/app.log" },
      { cmd: "grep AUDIT logs/app.log | tail -6 | cut -c1-170" },
    ],
  },
  {
    id: '09c-parity',
    title: 'Parity between the two pathways is asserted, not eyeballed',
    steps: [
      { cmd: "cd backend && ./mvnw test -Dtest=PathBDualPathwayReconciliationIntegrationTest 2>&1 | grep -E 'Tests run:|BUILD'" },
    ],
  },
  {
    id: '09e-gate-red',
    title: 'A coverage gate that nothing runs, and that is currently red',
    steps: [
      { cmd: "grep -A6 thresholds frontend/vitest.config.ts" },
      { cmd: "cd frontend && npm run test:coverage 2>&1 | tail -9" },
    ],
  },
  {
    id: '10a-artifacts',
    title: 'The specifications preceded the code, and it is countable',
    steps: [
      { cmd: "printf 'wayfinder tickets : %s\\n' $(ls .wayfinder/tickets | wc -l)" },
      { cmd: "printf 'scratch issues    : %s\\n' $(ls .scratch/*/issues/*.md | wc -l)" },
      { cmd: "printf 'specifications    : %s\\n' $(ls specs | wc -l)" },
      { cmd: "printf 'decision records  : %s\\n' $(ls docs/adr | wc -l)" },
    ],
  },
];

function run(cmd) {
  return new Promise((resolve) => {
    const started = Date.now();
    execFile('/bin/bash', ['-lc', cmd], { cwd: ROOT, maxBuffer: 32 * 1024 * 1024 }, (err, stdout, stderr) => {
      resolve({
        cmd,
        // stderr is kept: a command that warns should look like it warned.
        output: `${stdout ?? ''}${stderr ?? ''}`.replace(/\s+$/, ''),
        exitCode: err?.code ?? 0,
        durationMs: Date.now() - started,
      });
    });
  });
}

const only = process.argv.slice(2);
await mkdir(OUT, { recursive: true });

for (const spec of RUNS) {
  if (only.length && !only.includes(spec.id)) continue;
  const steps = [];
  for (const s of spec.steps) steps.push(await run(s.cmd));
  const record = {
    $recorded: 'Real command output, captured once and frozen. Regenerate: node video-generation-technical/tools/record-runs.mjs',
    id: spec.id,
    title: spec.title,
    recordedAt: new Date().toISOString(),
    steps,
  };
  await writeFile(path.join(OUT, `${spec.id}.json`), `${JSON.stringify(record, null, 2)}\n`);
  const lines = steps.reduce((n, s) => n + s.output.split('\n').length, 0);
  const codes = steps.map((s) => s.exitCode).join(',');
  console.log(`${spec.id.padEnd(20)} steps=${steps.length}  lines=${String(lines).padStart(3)}  exit=[${codes}]`);
}
