// Seeds the WARD_NURSE persona before the SPA bootstraps.
export default async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('admissions_role_persona', 'WARD_NURSE');
  });
};
