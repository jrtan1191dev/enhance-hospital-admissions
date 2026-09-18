#!/usr/bin/env node
/**
 * preflight.mjs — run every capture's pre-flight assertions against the live rig,
 * without spending a single second of TTS or a single recorded frame.
 *
 * WHY: capture.mjs calls assertSceneState immediately after page.goto and BEFORE
 * any step (capture.mjs:157), and a failed assertion aborts the whole run. Finding
 * that out during capture means discovering it after synthesis has already been
 * paid for. This reproduces exactly that check — same setupScript, same URL
 * resolution (`new URL(spec.url, baseUrl)`, so absolute URLs bypass baseUrl), same
 * getByText semantics — and reports every scene rather than stopping at the first.
 *
 * It deliberately does NOT run the steps: steps mutate the prototype's H2 state,
 * and a preflight that changes the data it is validating is worse than no
 * preflight. Post-click expectations belong in waitForText, which capture.mjs
 * enforces during the take.
 *
 * Usage (from the repository root, with the rig up):
 *   node video-generation-technical/tools/preflight.mjs
 */

import { readFileSync } from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const WORK = path.resolve(import.meta.dirname, '..');
const manifest = JSON.parse(readFileSync(path.join(WORK, 'storyboard.json'), 'utf8'));
const baseUrl = manifest.meta?.baseUrl ?? 'http://127.0.0.1:3000';

const { chromium } = await import(pathToFileURL(path.join(WORK, '.runtime', 'node_modules', 'playwright', 'index.mjs')).href);

const browser = await chromium.launch();
const scenes = manifest.scenes.filter((s) => s.visualEngine === 'playwright_web');
let failures = 0;

for (const scene of scenes) {
  const spec = scene.actionSpec ?? {};
  const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const page = await context.newPage();
  const problems = [];
  const deferred = [];

  try {
    if (manifest.meta?.fixedTime && page.clock) {
      await page.clock.install({ time: new Date(manifest.meta.fixedTime) });
      await page.clock.setFixedTime(new Date(manifest.meta.fixedTime));
    }
    if (spec.setupScript) {
      const mod = await import(pathToFileURL(path.resolve(WORK, spec.setupScript)).href);
      await mod.default?.({ page, context, scene });
    }
    const url = new URL(spec.url ?? '/', baseUrl).toString();
    await page.goto(url, { waitUntil: 'networkidle', timeout: 60_000 });

    for (const text of scene.assert?.visible ?? []) {
      try {
        await page.getByText(text, { exact: false }).first().waitFor({ state: 'visible', timeout: 12_000 });
      } catch {
        problems.push(`visible "${text}" NOT FOUND`);
      }
    }
    for (const text of scene.assert?.notVisible ?? []) {
      if ((await page.getByText(text, { exact: false }).count()) > 0) {
        problems.push(`notVisible "${text}" IS PRESENT`);
      }
    }

    // Selector availability. Only the FIRST acting step can be checked on the
    // landing page: later selectors legitimately appear as a consequence of
    // earlier clicks (07b's override button exists only once a request is
    // selected), so they are reported as deferred rather than failed.
    const acting = (spec.steps ?? []).filter((st) => st.selector);
    for (const [i, step] of acting.entries()) {
      const n = await page.locator(step.selector).count().catch(() => 0);
      if (n > 0) continue;
      if (i === 0) problems.push(`first step selector matches nothing on load: ${step.selector}`);
      else deferred.push(step.selector);
    }
  } catch (err) {
    problems.push(`navigation failed: ${err.message.split('\n')[0]}`);
  } finally {
    await context.close();
  }

  failures += problems.length ? 1 : 0;
  const label = problems.length ? 'FAIL' : 'pass';
  console.log(`${label}  ${scene.id.padEnd(32)} ${(spec.url ?? '/').slice(0, 58)}`);
  for (const p of problems) console.log(`        ${p}`);
  for (const d of deferred) console.log(`        deferred (appears after an earlier step): ${d}`);
}

await browser.close();
console.log(`\n${scenes.length - failures}/${scenes.length} capture scenes ready`);
process.exit(failures ? 1 : 0);
