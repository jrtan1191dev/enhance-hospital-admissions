// @ts-check
/**
 * Story 1.1.1 — Clinical Parameter Ingestion & Acuity Synthesis (Epic 1 > Feature 1.1).
 * AC1 — successful clinical parameter synthesis for an acute medical patient.
 *
 * Expectations are DERIVED from the story and exercised against the reachable
 * synthesized-baseline ED patient whose completed diagnostics drive the engine.
 * The story literal "P101" (Tan Ah Meng) already carries an admission request and
 * is excluded from the awaiting-assessment board; the reachable troponin-positive
 * synthesis patient is Chua Wee Kiat (Q-P120: Troponin 180 ng/L, WBC 13.2). The
 * Diagnostic Synthesis Engine pre-populates the Smart Assessment Dossier when the
 * ED Attending selects that patient.
 *
 * Verdict: FAIL. Tier 2 Acute Urgent + Continuous Telemetry + Fall Risk are
 * reachable and satisfied, but the story requires the suspected diagnosis to be
 * pre-populated as "Non-ST Elevation Myocardial Infarction (NSTEMI)" and the
 * product renders "Acute Coronary Syndrome (troponin-positive chest pain)" — the
 * NSTEMI literal appears nowhere. A reachable surface that contradicts a
 * documented clause is FAIL; the assertion is NOT weakened to the product phrasing.
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
  'AC1: Given patient "P101" has received completed lab reports (Troponin 150 ng/L, elevated WBC), an ECG report, and recent vital signs (BP 95/60, SpO2 93%) in the EHR When the Diagnostic Synthesis Engine processes the new diagnostic arrivals Then the engine compiles an objective clinical summary for patient "P101" And calculates an initial acuity recommendation of "Tier 2: Acute Urgent" And pre-populates suspected diagnosis as "Non-ST Elevation Myocardial Infarction (NSTEMI)" And flags care requirements as "Continuous Telemetry" and "Fall Risk Precautions"';

void STORY;

const REASON =
  'Failure: the story requires the Diagnostic Synthesis Engine to pre-populate the suspected diagnosis as ' +
  '"Non-ST Elevation Myocardial Infarction (NSTEMI)", but the ED Smart Assessment Dossier renders ' +
  '"Acute Coronary Syndrome (troponin-positive chest pain)" for the troponin-positive patient and the ' +
  'NSTEMI text appears nowhere in the product. The acuity tier (Tier 2 Acute Urgent), Continuous Telemetry, ' +
  'and Fall Risk directives are pre-populated correctly. Likely: the synthesized clinical baseline seeds a ' +
  'generic ACS diagnosis string rather than the specific NSTEMI classification the story specifies.';

test.describe('Story 1.1.1 — Clinical Parameter Ingestion & Acuity Synthesis (AC1)', () => {
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
        '- Seeded troponin-positive awaiting-assessment patient Chua Wee Kiat (Q-P120, Troponin 180 ng/L, WBC 13.2) from DataInitializer.',
        '- Oracle: the rendered Smart Assessment Dossier fields (acuity tier select value, telemetry/fall-risk checkbox state, suspected-diagnosis banner text).',
        'Isolation:',
        '- Isolated E2E stack: harness-owned single Spring Boot JAR (prototype profile), H2 create-drop reseeded on boot.',
        '- Selection is client-side only; no global state is mutated before assertion.',
        'Steps:',
        '1. Sign in as the ED Attending persona and open the ED Clinical Intake board.',
        '2. Select the troponin-positive waiting patient (Chua Wee Kiat) to open the Smart Assessment Dossier.',
        '3. Read the pre-populated acuity tier, care directives, and suspected-diagnosis banner.',
        'Visible outcomes:',
        '- Urgency Acuity Tier is pre-populated to Tier 2: Acute Urgent (Telemetry / High Monitoring).',
        '- The Continuous Telemetry Monitoring and High Fall Risk Precautions care directives are pre-checked.',
        '- The suspected-diagnosis banner reads "Non-ST Elevation Myocardial Infarction (NSTEMI)".',
        'Acceptance mapping:',
        '- AC1 | Story clause: calculates an initial acuity recommendation of "Tier 2: Acute Urgent" | Actor: ED Attending Physician | Visible outcome: Urgency Acuity Tier is pre-populated to Tier 2: Acute Urgent (Telemetry / High Monitoring).',
        '- AC1 | Story clause: flags care requirements as "Continuous Telemetry" and "Fall Risk Precautions" | Actor: ED Attending Physician | Visible outcome: The Continuous Telemetry Monitoring and High Fall Risk Precautions care directives are pre-checked.',
        '- AC1 | Story clause: pre-populates suspected diagnosis as "Non-ST Elevation Myocardial Infarction (NSTEMI)" | Actor: ED Attending Physician | Visible outcome: The suspected-diagnosis banner reads "Non-ST Elevation Myocardial Infarction (NSTEMI)".',
      ].join('\n'),
      'text/plain',
    );

    // 1. Authenticate as the ED Attending and land on the ED intake board.
    await loginAs('ED_ATTENDING');
    await expect(
      page.getByRole('heading', { name: /Emergency Department \(ED\) Clinical Intake/i }),
    ).toBeVisible();

    // 2. Select the troponin-positive waiting patient to run the synthesis engine.
    const acuteRow = page.getByRole('row', { name: /Chua Wee Kiat/ });
    await expect(acuteRow).toBeVisible();
    await acuteRow.click();
    await expect(page.getByText('Reviewing clinical parameters for Chua Wee Kiat')).toBeVisible();

    // 3a. AC1 — acuity recommendation "Tier 2: Acute Urgent" (pre-populated select).
    const tierSelect = page.getByRole('combobox').filter({ hasText: 'Tier 2: Acute Urgent' });
    await allure.step(
      'Assertion: [AC1] actor: ED Attending Physician | acuity tier pre-populated to Tier 2: Acute Urgent',
      async () => {
        const expected = 'TIER_2_ACUTE_URGENT';
        const observed = await tierSelect.inputValue();
        await allure.step(`Evidence: expected: ${expected} | observed: ${observed}`, async () => {
          expect(observed).toBe(expected);
        });
      },
    );

    // 3b. AC1 — care requirements: Continuous Telemetry + Fall Risk pre-checked.
    const telemetryCheckbox = page.getByRole('checkbox', { name: /Continuous Telemetry Monitoring/ });
    const fallRiskCheckbox = page.getByRole('checkbox', { name: /High Fall Risk Precautions/ });
    await allure.step(
      'Assertion: [AC1] actor: ED Attending Physician | care requirements pre-flagged (Continuous Telemetry + Fall Risk Precautions)',
      async () => {
        const observedTelemetry = await telemetryCheckbox.isChecked();
        const observedFallRisk = await fallRiskCheckbox.isChecked();
        await allure.step(
          `Evidence: expected: telemetry=true, fallRisk=true | observed: telemetry=${observedTelemetry}, fallRisk=${observedFallRisk}`,
          async () => {
            expect(observedTelemetry).toBe(true);
            expect(observedFallRisk).toBe(true);
          },
        );
      },
    );

    // 3c. AC1 — suspected diagnosis pre-populated as "Non-ST Elevation Myocardial Infarction (NSTEMI)".
    // Scope to the dossier banner (the diagnosis also appears in the waiting table),
    // uniquely identified by its "Suspected:" label container.
    const suspectedBanner = page.getByText('Suspected:').locator('..');
    await allure.step(
      'Assertion: [AC1] actor: ED Attending Physician | suspected diagnosis pre-populated as "Non-ST Elevation Myocardial Infarction (NSTEMI)"',
      async () => {
        const expected = 'Non-ST Elevation Myocardial Infarction (NSTEMI)';
        const observed = (await suspectedBanner.textContent())?.trim() ?? '';
        await allure.step(`Evidence: expected: ${expected} | observed: ${observed}`, async () => {
          await expect(suspectedBanner).toContainText('Non-ST Elevation Myocardial Infarction (NSTEMI)');
        });
      },
    );
  });
});
