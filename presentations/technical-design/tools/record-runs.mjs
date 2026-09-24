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
 *   node presentations/technical-design/tools/record-runs.mjs            # all runs
 *   node presentations/technical-design/tools/record-runs.mjs 09b-audit-log
 *
 * ORDERING: 09b-audit-log must be recorded AFTER capture.mjs has driven the
 * workflows, because a freshly started JVM has zero audit lines. Everything else is
 * order-independent.
 */

import { execFile } from 'node:child_process';
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

const ROOT = path.resolve(import.meta.dirname, '..', '..', '..');
const OUT = path.resolve(import.meta.dirname, '..', 'pages', 'runs');

/**
 * Every command here was dry-run verified on 18 Sep 2026 before being committed.
 * `cwd` is repository-root-relative.
 */
const RUNS = [
  {
    // Beat 1 — consensus before eligibility. The gate stays closed under PARTIAL
    // completion: one consult back, one still open, request still ASSESSMENT_PENDING.
    id: '05b-consensus-gate',
    title: 'The consensus gate stays closed until every consult is in',
    steps: [
      { cmd: `grep -h '@DisplayName' backend/src/test/java/com/hospital/admissions/service/ClinicianServiceTest.java | grep -i 'consensus' | sed 's/.*("/  - /; s/")//'` },
      { cmd: "cd backend && ./mvnw test -Dtest='ClinicianServiceTest#testSubmitConsult_ConsensusGate_PartialCompletion_RemainsAssessmentPending+testSubmitConsult_ConsensusGate_AllCompleted_AdvancesToBedRequested_AndElevatesAcuityAndTelemetry' 2>&1 | grep -E 'ASSESSMENT_PENDING|Tests run:|BUILD'" },
    ],
  },
  {
    // Beat 2 — disagreement resolves to safety. Highest acuity wins, telemetry
    // unioned. Same test class asserts effectiveAcuityTier elevation + isDiscordant.
    id: '06a-discordance',
    title: 'Disagreement resolves to the higher acuity, and the queue sorts on it',
    steps: [
      { cmd: `grep -n 'effectiveAcuityTier' backend/src/main/java/com/hospital/admissions/entity/AdmissionRequest.java` },
      { cmd: `grep -n 'setEffectiveAcuityTier\\|highestAcuity' backend/src/main/java/com/hospital/admissions/service/ClinicianService.java | head -4` },
      { cmd: "cd backend && ./mvnw test -Dtest='ClinicianServiceTest#testSubmitConsult_ConsensusGate_AllCompleted_AdvancesToBedRequested_AndElevatesAcuityAndTelemetry,BmuServiceTest#testGetPrioritizedQueue_SortsByEffectiveAcuityTierOverPrimaryAcuityTier' 2>&1 | grep -E 'Tests run:|BUILD'" },
    ],
  },
  {
    // Beat 4 — concurrency. Two writers, one winner; the loser gets a 409 with a
    // problem-detail body. Proved in tests because a browser can't stage a real race.
    id: '08b-conflict-409',
    title: 'Optimistic locking, proved where the UI cannot honestly stage it',
    steps: [
      { cmd: `grep -h '@DisplayName' backend/src/test/java/com/hospital/admissions/controller/ClinicianControllerTest.java backend/src/test/java/com/hospital/admissions/controller/GlobalExceptionHandlerTest.java | grep -i 'conflict\\|409' | sed 's/.*("/  - /; s/")//'` },
      { cmd: "cd backend && ./mvnw test -Dtest='ClinicianControllerTest#testClaimBroadcast_ConflictReturns409,GlobalExceptionHandlerTest' 2>&1 | grep -E '409|Tests run:|BUILD'" },
    ],
  },
  {
    // Beat 5 — provable. Both pathways computed, a test fails if they disagree.
    id: '09b-parity',
    title: 'Parity between the two pathways is asserted, not eyeballed',
    steps: [
      { cmd: "cd backend && ./mvnw test -Dtest=PathBDualPathwayReconciliationIntegrationTest 2>&1 | grep -E 'Tests run:|BUILD'" },
    ],
  },
  {
    // Beat 5 — fails loud where unbuilt. The three production adapters throw on
    // invocation instead of returning fake data.
    id: '09c-stub-contract',
    title: 'The three production adapters are asserted to fail fast',
    steps: [
      { cmd: `grep -h '@DisplayName' backend/src/test/java/com/hospital/admissions/gateway/GatewaysAndSolversTest.java | sed 's/.*("/  - /; s/")//'` },
      { cmd: "cd backend && ./mvnw test -Dtest=GatewaysAndSolversTest 2>&1 | grep -E 'UnsupportedOperationException|Tests run:|BUILD'" },
    ],
  },
  {
    // Beat 5 — the honest red gate. Four thresholds declared at 90; branches come
    // back at 74.84 because nothing runs them automatically.
    id: '09e-gate-red',
    title: 'A coverage gate that nothing runs, and that is currently red',
    steps: [
      { cmd: "grep -A6 thresholds frontend/vitest.config.ts" },
      { cmd: "cd frontend && npm run test:coverage 2>&1 | tail -9" },
    ],
  },
  {
    // Close — the specifications preceded the code, and it is countable.
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
    $recorded: 'Real command output, captured once and frozen. Regenerate: node presentations/technical-design/tools/record-runs.mjs',
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
