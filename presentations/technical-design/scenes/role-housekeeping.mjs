// Seeds the HOUSEKEEPING persona before the SPA bootstraps.
// Shares the /ward route with WARD_NURSE; the view is gated by role, not path.
export default async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('admissions_role_persona', 'HOUSEKEEPING');
  });
};
