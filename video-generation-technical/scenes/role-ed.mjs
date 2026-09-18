// Seeds the prototype role persona BEFORE the SPA bootstraps.
// The app reads localStorage['admissions_role_persona'] on first render and
// sends it as the X-User-Role header on every API call. No credentials.
export default async ({ page }) => {
  await page.addInitScript(() => {
    localStorage.setItem('admissions_role_persona', 'ED_ATTENDING');
  });
};
