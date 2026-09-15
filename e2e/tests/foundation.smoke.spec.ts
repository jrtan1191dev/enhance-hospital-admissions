// @ts-check
/**
 * Foundation smoke test — login (persona), navigation, and reset baseline.
 *
 * This spec proves the harness foundation works end to end:
 *   1. LOGIN:      seed a role persona (header-driven auth) and load the SPA.
 *   2. NAVIGATION: the ED persona lands on the ED intake work area, and the
 *                  persona switcher can move to the BMU work area.
 *   3. RESET:      the isolated lifecycle (started by global setup via the
 *                  harness command, NOT a private endpoint) presents the known
 *                  deterministic seed baseline — a seeded ED patient is visible.
 *
 * Video/trace are forced by the runner config, so this spec asserts behaviour
 * only; it never toggles evidence capture itself.
 */
import { test, expect } from '../fixtures/personas';

test.describe('foundation smoke', () => {
  test('ED persona logs in, sees seeded baseline, and navigates', async ({ page, loginAs }) => {
    // 1. LOGIN as ED attending (header-driven persona via localStorage).
    await loginAs('ED_ATTENDING');

    // 2. NAVIGATION: root redirects to the ED work area landing heading.
    await expect(
      page.getByRole('heading', { name: /Emergency Department \(ED\) Clinical Intake/i })
    ).toBeVisible();

    // 3. RESET baseline: the freshly seeded lifecycle shows a known seed patient
    //    on the ED intake board. "Encik Azman" (P108) is a deterministically
    //    seeded awaiting-assessment patient present on every JVM boot.
    await expect(page.getByText('Encik Azman').first()).toBeVisible();

    // 2b. NAVIGATION via persona switcher to the BMU coordinator work area.
    const roleSelect = page.getByLabel('Select role persona');
    await expect(roleSelect).toBeVisible();
    await roleSelect.selectOption('BMU_COORDINATOR');
    await expect(page).toHaveURL(/\/bmu/);
  });
});
