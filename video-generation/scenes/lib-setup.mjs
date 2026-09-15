/**
 * Shared capture setup for the Hospital Admissions SPA.
 *
 * Two jobs, both required for deterministic deep-link capture:
 *
 *  1. SPA fallback shim. The Spring Boot single-JAR serves the built SPA at "/"
 *     but has no catch-all fallback: a hard GET to /bmu, /specialist, etc.
 *     returns a 404 ProblemDetail JSON (verified). TanStack Router is a browser
 *     router, so the fix is to let the deep URL load index.html and let the
 *     client router take over. We intercept top-level HTML document requests for
 *     app routes and respond with the real index.html fetched from "/".
 *
 *  2. Role seeding. The app reads localStorage key 'admissions_role_persona'
 *     on bootstrap and sends it as the X-User-Role header. Seeding it before
 *     navigation makes each scene render as the intended persona deterministically.
 *     No credentials are involved — this is a prototype header-driven role switch.
 */

const APP_ROUTES = ['/ed', '/specialist', '/bmu', '/patient', '/ward', '/analytics'];

export function makeSetup(role) {
  return async ({ page, context }) => {
    // 1. Seed the persona before the SPA bootstraps.
    await page.addInitScript((r) => {
      try {
        localStorage.setItem('admissions_role_persona', r);
      } catch {
        /* storage unavailable — ignore */
      }
    }, role);

    // 2. SPA fallback: serve index.html for deep-link document navigations so
    //    the client router can mount on the requested route.
    let indexHtml = null;
    await context.route('**/*', async (route) => {
      const req = route.request();
      const url = new URL(req.url());
      const isDocument = req.resourceType() === 'document';
      const isAppRoute = APP_ROUTES.some(
        (p) => url.pathname === p || url.pathname.startsWith(`${p}/`),
      );

      if (isDocument && isAppRoute) {
        if (indexHtml === null) {
          const res = await context.request.get(`${url.origin}/`);
          indexHtml = await res.text();
        }
        await route.fulfill({
          status: 200,
          contentType: 'text/html; charset=utf-8',
          body: indexHtml,
        });
        return;
      }
      await route.continue();
    });
  };
}
