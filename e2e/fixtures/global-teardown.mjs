// @ts-check
/**
 * Playwright global teardown.
 *
 * Tears down ONLY the harness lifecycle this run created, by reading the run
 * file written in global setup and invoking `harness.mjs stop --lifecycle`.
 * It removes no other resources.
 */
import { execFileSync } from 'node:child_process';
import { readFileSync, existsSync, rmSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const E2E_ROOT = path.resolve(__dirname, '..');
const HARNESS = path.join(E2E_ROOT, 'scripts', 'harness.mjs');
const RUN_STATE = path.join(E2E_ROOT, 'test-results', '.run-lifecycle.json');

export default async function globalTeardown() {
  if (!existsSync(RUN_STATE)) return;
  const record = JSON.parse(readFileSync(RUN_STATE, 'utf8'));
  try {
    const out = execFileSync(
      'node',
      [HARNESS, 'stop', '--lifecycle', record.lifecycleId, '--json'],
      { encoding: 'utf8', cwd: E2E_ROOT }
    );
    // eslint-disable-next-line no-console
    console.log(`[e2e] teardown: ${out.trim()}`);
  } finally {
    rmSync(RUN_STATE, { force: true });
  }
}
