// @ts-check
/**
 * Playwright global setup.
 *
 * Starts ONE isolated harness lifecycle for the whole run by invoking the
 * harness-owned command (`harness.mjs start`) — never a private/reset endpoint.
 * The chosen baseURL and lifecycle id are written to a run file that
 * global-teardown reads to tear down only what this run created.
 */
import { execFileSync } from 'node:child_process';
import { writeFileSync, mkdirSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const E2E_ROOT = path.resolve(__dirname, '..');
const HARNESS = path.join(E2E_ROOT, 'scripts', 'harness.mjs');
const RUN_STATE = path.join(E2E_ROOT, 'test-results', '.run-lifecycle.json');

export default async function globalSetup() {
  const out = execFileSync('node', [HARNESS, 'start', '--json'], {
    encoding: 'utf8',
    cwd: E2E_ROOT,
  });
  const record = JSON.parse(out.trim().split('\n').pop());

  // Expose baseURL to the test config/use for this process's spawned workers.
  process.env.E2E_BASE_URL = record.baseUrl;
  process.env.E2E_LIFECYCLE_ID = record.lifecycleId;

  mkdirSync(path.dirname(RUN_STATE), { recursive: true });
  writeFileSync(RUN_STATE, JSON.stringify(record, null, 2));

  // eslint-disable-next-line no-console
  console.log(`[e2e] lifecycle ${record.lifecycleId} up at ${record.baseUrl} (mode=${record.startupMode})`);
}
