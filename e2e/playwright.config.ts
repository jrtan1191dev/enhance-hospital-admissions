// @ts-check
import { defineConfig, devices } from '@playwright/test';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

/**
 * Evidence capture policy (foundation-owned, NOT a per-spec convention):
 *   - Video and trace are forced ON for every test from config.
 *   - In campaign mode (E2E_CAMPAIGN_SUITE set), Allure results and the
 *     Playwright output (trace/video) go to campaign-owned directories that
 *     the campaign supplies via ALLURE_RESULTS_DIR / PLAYWRIGHT_OUTPUT_DIR.
 *     Those directories must be empty and campaign-owned; the harness never
 *     copies shared results into them.
 *   - Outside campaign mode (foundation smoke), results/output default to
 *     the git-ignored e2e/ local dirs.
 */
const isCampaign = Boolean(process.env.E2E_CAMPAIGN_SUITE);

const allureResultsDir = process.env.ALLURE_RESULTS_DIR
  ? path.resolve(process.env.ALLURE_RESULTS_DIR)
  : path.join(__dirname, 'allure-results');

const outputDir = process.env.PLAYWRIGHT_OUTPUT_DIR
  ? path.resolve(process.env.PLAYWRIGHT_OUTPUT_DIR)
  : path.join(__dirname, 'test-results');

export default defineConfig({
  testDir: path.join(__dirname, 'tests'),
  // Reject a stray --list in campaign mode: it must run and produce evidence.
  forbidOnly: isCampaign,
  fullyParallel: false,
  workers: 1,
  reporter: [
    ['line'],
    ['allure-playwright', { resultsDir: allureResultsDir, detail: true }],
  ],
  outputDir,
  timeout: 60_000,
  expect: { timeout: 15_000 },
  use: {
    // Forced evidence — never a per-spec opt-in.
    trace: 'on',
    video: 'on',
    screenshot: 'on',
    actionTimeout: 15_000,
    // baseURL is injected per-run by global setup via process.env.E2E_BASE_URL.
    baseURL: process.env.E2E_BASE_URL || 'http://127.0.0.1:8080',
  },
  globalSetup: path.join(__dirname, 'fixtures', 'global-setup.mjs'),
  globalTeardown: path.join(__dirname, 'fixtures', 'global-teardown.mjs'),
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
