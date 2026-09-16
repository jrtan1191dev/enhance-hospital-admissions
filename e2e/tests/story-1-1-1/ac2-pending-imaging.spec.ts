// @ts-check
/**
 * Story 1.1.1 — Clinical Parameter Ingestion & Acuity Synthesis (Epic 1 > Feature 1.1).
 * AC2 — handling pending diagnostic imaging results.
 *
 * Expectations are DERIVED from the story. The story literal "P102" (Siti Rahmah)
 * already carries an admission request and is excluded from the awaiting-assessment
 * board; the reachable completed-labs synthesis patient is Nurul Huda (Q-P121,
 * completed labs, troponin Normal). Opening the Smart Assessment Dossier runs the
 * synthesis engine and compiles the admission dossier.
 *
 * Verdict: FAIL. The dossier compile action is reachable and the completed lab
 * values and vitals render, but the story requires a warning banner stating
 * "Diagnostic Imaging (CT Brain) Pending" and the product has no pending-imaging
 * warning banner anywhere (the Patient entity carries no imaging-status field and
 * the ED route renders no such banner). Missing required UI on a reachable surface
 * is FAIL, not SKIPPED; the assertion proves the banner and observes its absence.
 *
 * Evidence trace/video/screenshot are forced ON by playwright.config.ts. Auth is
 * header-driven persona via localStorage (loginAs fixture). Selection is
 * client-side only, so this scenario reads a fresh seeded stack without mutating
 * global state.
 */
import { test, expect } from '../../fixtures/personas';
import { allure } from 'allure-playwright';

const campaignSuite = process.env.E2E_CAMPAIGN_SUITE;
if (!campaignSuite) throw new Error('E2E_CAMPAIGN_SUITE is required');

// Full frozen story (intent + both acceptance criteria), verbatim from story-analysis.json.
const STORY =
  'As an Emergency Department (ED) Attending Physician, I want the system to ingest and synthesize real-time diagnostic scan reports, laboratory panels, vital signs, and clinical notes, So that I have an objective, consolidated clinical baseline and recommended admission acuity score without manually collating fragmented EHR reports.\n\n' +
  'AC1: Given patient "P101" has received completed lab reports (Troponin 150 ng/L, elevated WBC), an ECG report, and recent vital signs (BP 95/60, SpO2 93%) in the EHR When the Diagnostic Synthesis Engine processes the new diagnostic arrivals Then the engine compiles an objective clinical summary for patient "P101" And calculates an initial acuity recommendation of "Tier 2: Acute Urgent" And pre-populates suspected diagnosis as "Non-ST Elevation Myocardial Infarction (NSTEMI)" And flags care requirements as "Continuous Telemetry" and "Fall Risk Precautions"\n\n' +
  'AC2: Given patient "P102" has completed laboratory tests but an ordered CT brain scan is marked "In Progress" When the synthesis engine compiles the admission dossier Then the dossier reflects all completed lab values and vitals And displays a warning banner stating "Diagnostic Imaging (CT Brain) Pending" And allows the ED Attending to review interim findings without prematurely finalizing acuity';

const STORY_INTENT =
  'As an Emergency Department (ED) Attending Physician, I want the system to ingest and synthesize real-time diagnostic scan reports, laboratory panels, vital signs, and clinical notes, So that I have an objective, consolidated clinical baseline and recommended admission acuity score without manually collating fragmented EHR reports.';

const AC_DESCRIPTION =
  'AC2: Given patient "P102" has completed laboratory tests but an ordered CT brain scan is marked "In Progress" When the synthesis engine compiles the admission dossier Then the dossier reflects all completed lab values and vitals And displays a warning banner stating "Diagnostic Imaging (CT Brain) Pending" And allows the ED Attending to review interim findings without prematurely finalizing acuity';

void STORY;

const REASON =
  'Failure: the story requires the compiled dossier to display a warning banner stating ' +
  '"Diagnostic Imaging (CT Brain) Pending" while an ordered CT brain scan is In Progress, but the ED Smart ' +
  'Assessment Dossier renders no pending-imaging warning banner at all. The completed lab values and vitals ' +
  'do render. Likely: the Patient clinical baseline model has no imaging-status field and the ED route was ' +
  'never built to surface an "imaging pending" banner, so this diagnostic-imaging-pending requirement is unmet.';

test.describe('Story 1.1.1 — Clinical Parameter Ingestion & Acuity Synthesis (AC2)', () => {
  test(AC_DESCRIPTION, async ({ page, loginAs }) => {
    await allure.parentSuite(campaignSuite);
    await allure.feature('Feature: Clinical Parameter Ingestion & Acuity Synthesis');
    await allure.suite('US1.1.1: Clinical Parameter Ingestion & Acuity Synthesis');
    await allure.subSuite(AC_DESCRIPTION);
    await allure.description(`${STORY_INTENT}\n\n${AC_DESCRIPTION}\n\n${REASON}`);
    await allure.parameter('persona', 'ED_ATTENDING');
    await allure.parameter('e2e_configuration', 'isolated E2E fixture (harness-owned single JAR, prototype profile)');

    await allure.attachment(
      'Test plan',
      [
        'Data:',
        '- Persona ED_ATTENDING via loginAs fixture (header-driven X-User-Role).',
        '- Seeded completed-labs awaiting-assessment patient Nurul Huda (Q-P121, troponin Normal, completed vitals) from DataInitializer.',
        '- Oracle: the rendered Smart Assessment Dossier (vitals banner text, and presence of a "Diagnostic Imaging (CT Brain) Pending" warning banner).',
        'Isolation:',
        '- Isolated E2E stack: harness-owned single Spring Boot JAR (prototype profile), H2 create-drop reseeded on boot.',
        '- Selection is client-side only; opening the dossier does not finalize or mutate server state.',
        'Steps:',
        '1. Sign in as the ED Attending persona and open the ED Clinical Intake board.',
        '2. Select the completed-labs waiting patient (Nurul Huda) to compile the admission dossier.',
        '3. Read the rendered completed vitals and check for the pending diagnostic-imaging warning banner.',
        '4. Confirm the patient remains on the waiting board with the finalize action available but not invoked.',
        'Visible outcomes:',
        '- The dossier reflects the completed lab values and vitals (BP 124/78, HR 84 bpm, SpO2 97%).',
        '- A warning banner stating "Diagnostic Imaging (CT Brain) Pending" is displayed.',
        '- The patient remains on the waiting board and the Confirm Direct Admission finalize action is available but not auto-invoked.',
        'Acceptance mapping:',
        '- AC2 | Story clause: the dossier reflects all completed lab values and vitals | Actor: ED Attending Physician | Visible outcome: The dossier reflects the completed lab values and vitals (BP 124/78, HR 84 bpm, SpO2 97%).',
        '- AC2 | Story clause: displays a warning banner stating "Diagnostic Imaging (CT Brain) Pending" | Actor: ED Attending Physician | Visible outcome: A warning banner stating "Diagnostic Imaging (CT Brain) Pending" is displayed.',
        '- AC2 | Story clause: allows the ED Attending to review interim findings without prematurely finalizing acuity | Actor: ED Attending Physician | Visible outcome: The patient remains on the waiting board and the Confirm Direct Admission finalize action is available but not auto-invoked.',
      ].join('\n'),
      'text/plain',
    );

    // 1. Authenticate as the ED Attending and land on the ED intake board.
    await loginAs('ED_ATTENDING');
    await expect(
      page.getByRole('heading', { name: /Emergency Department \(ED\) Clinical Intake/i }),
    ).toBeVisible();

    // 2. Select the completed-labs waiting patient to compile the admission dossier.
    const stableRow = page.getByRole('row', { name: /Nurul Huda/ });
    await expect(stableRow).toBeVisible();
    await stableRow.click();
    const dossier = page.getByText('Reviewing clinical parameters for Nurul Huda');
    await expect(dossier).toBeVisible();

    // 3a. AC2 — dossier reflects completed lab values and vitals.
    const vitalsGrid = page.getByText('HR:', { exact: false }).locator('..');
    await allure.step(
      'Assertion: [AC2] actor: ED Attending Physician | dossier reflects completed lab values and vitals',
      async () => {
        const observedVitals = (await vitalsGrid.textContent())?.trim() ?? '';
        await allure.step(
          `Evidence: expected: BP 124/78, HR 84 bpm, SpO2 97% | observed: ${observedVitals}`,
          async () => {
            await expect(vitalsGrid).toContainText('124/78');
            await expect(vitalsGrid).toContainText('84 bpm');
            await expect(vitalsGrid).toContainText('97%');
          },
        );
      },
    );

    // 3b. AC2 — no premature finalization: patient stays on the waiting board, finalize control available but not invoked.
    const finalizeButton = page.getByRole('button', { name: /Confirm Direct Admission/ });
    await allure.step(
      'Assertion: [AC2] actor: ED Attending Physician | patient stays on waiting board with finalize action available but not invoked',
      async () => {
        const rowVisible = await stableRow.isVisible();
        const finalizeVisible = await finalizeButton.isVisible();
        await allure.step(
          `Evidence: expected: waiting row present, finalize button available (not clicked) | observed: rowVisible=${rowVisible}, finalizeVisible=${finalizeVisible}`,
          async () => {
            await expect(stableRow).toBeVisible();
            await expect(finalizeButton).toBeVisible();
          },
        );
      },
    );

    // 3c. AC2 — warning banner "Diagnostic Imaging (CT Brain) Pending" must be displayed.
    const pendingImagingBanner = page.getByText('Diagnostic Imaging (CT Brain) Pending');
    await allure.step(
      'Assertion: [AC2] actor: ED Attending Physician | warning banner "Diagnostic Imaging (CT Brain) Pending" is displayed',
      async () => {
        const observedCount = await pendingImagingBanner.count();
        await allure.step(
          `Evidence: expected: banner "Diagnostic Imaging (CT Brain) Pending" visible | observed: matchingElements=${observedCount}`,
          async () => {
            await expect(pendingImagingBanner).toBeVisible();
          },
        );
      },
    );
  });
});
