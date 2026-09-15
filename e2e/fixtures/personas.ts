// @ts-check
/**
 * Reusable persona (auth) fixture for the Hospital Admissions prototype.
 *
 * AUTH MODEL: There is no login page. The SPA reads the active role from
 * localStorage key `admissions_role_persona` (see frontend/src/services/api.ts)
 * and sends it as the `X-User-Role` header on every API call via an axios
 * interceptor. The prototype backend's PrototypeSecurityFilter maps that header
 * to a Spring Security principal. So "log in as a role" == set that localStorage
 * key before the SPA bootstraps, then navigate.
 *
 * Valid roles (PrototypeSecurityFilter): ED_ATTENDING, SPECIALIST,
 * BMU_COORDINATOR, PATIENT, WARD_NURSE, HOUSEKEEPING.
 */
import { test as base, expect } from '@playwright/test';

const ROLE_STORAGE_KEY = 'admissions_role_persona';

/** @typedef {'ED_ATTENDING'|'SPECIALIST'|'BMU_COORDINATOR'|'PATIENT'|'WARD_NURSE'|'HOUSEKEEPING'} RolePersona */

export const test = base.extend({
  /**
   * loginAs: establishes a role persona by seeding localStorage on the app
   * origin BEFORE any app code runs, then reloading so the SPA picks it up.
   * @type {import('@playwright/test').Fixture<(role: RolePersona) => Promise<void>>}
   */
  loginAs: async ({ page, baseURL }, use) => {
    /** @param {RolePersona} role */
    const loginAs = async (role) => {
      // Seed the persona at the app origin before the SPA bootstraps.
      await page.addInitScript(
        ([key, value]) => window.localStorage.setItem(key, value),
        [ROLE_STORAGE_KEY, role]
      );
      await page.goto(baseURL ?? '/');
      // Confirm the SPA rendered (title/root present).
      await expect(page).toHaveTitle(/.+/);
    };
    await use(loginAs);
  },
});

export { expect };
