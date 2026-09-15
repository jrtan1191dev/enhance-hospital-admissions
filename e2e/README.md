# E2E Test Harness — Hospital Admissions Prototype

Trustworthy Playwright + Allure 3 end-to-end evidence for the Intelligent
Patient Flow & Bed Capacity Orchestration System.

This directory is the **foundation** (runnable harness). Generated per-campaign
evidence lives under `../artifacts/e2e/` and is git-ignored.

---

## Project profile

| Aspect | Value |
| --- | --- |
| **App under test** | Single Spring Boot JAR (`backend/target/admissions-0.0.1-SNAPSHOT.jar`) that serves the React SPA at `/` and the REST API at `/api/v1` on one port. |
| **Profile** | `prototype` (required; provides H2 in-memory DB, mock gateways, header auth). |
| **Install / build** | `npm --prefix e2e install` (harness deps). App JAR: `(cd backend && ./mvnw clean package -DskipTests)`. |
| **Start (test lifecycle)** | `node e2e/scripts/harness.mjs start --json` — boots a fresh JVM on a unique free port, waits for health, reseeds a known baseline, prints `baseUrl` + `lifecycleId`. |
| **Health check** | `GET {baseUrl}/actuator/health` → `{"status":"UP"}` (~3–5 s boot). |
| **Browser base URL** | Printed by `harness.mjs start` (e.g. `http://127.0.0.1:<port>`). Playwright reads it from `E2E_BASE_URL`, set by global setup. |
| **Auth / test accounts** | No login page. Header-driven `X-User-Role`. The SPA reads the active role from `localStorage['admissions_role_persona']` and sends it as `X-User-Role`. The `loginAs(role)` fixture seeds that key before the SPA bootstraps. |
| **Roles** | `ED_ATTENDING`, `SPECIALIST`, `BMU_COORDINATOR`, `PATIENT`, `WARD_NURSE`, `HOUSEKEEPING` (see `PrototypeSecurityFilter`). |
| **Designated test environment** | Ephemeral local JVM per lifecycle (unique port, in-memory H2). No shared/staging environment is touched. |
| **Report directories** | Local (foundation): `e2e/allure-results/`, `e2e/allure-report/`, `e2e/test-results/`. Campaign: campaign-owned dirs under `../artifacts/e2e/campaigns/<id>/` via `ALLURE_RESULTS_DIR` / `PLAYWRIGHT_OUTPUT_DIR`. All git-ignored. |

---

## Reset / isolation contract

The app uses an in-memory H2 database with `ddl-auto: create-drop`, and
`DataInitializer` (active only under the `prototype` profile) **deterministically
reseeds** the full dataset (wards `8A/8B/9A/9B/10A/10B/11A`, patients
`P101`–`P118`) on **every JVM boot**. There is **no in-app reset endpoint**, and
a story spec must never call reset/hidden APIs itself.

The reset is therefore **process-level and harness-owned** — `e2e/scripts/harness.mjs`:

- **`start`** — allocates a unique free port + `lifecycleId`, launches the JAR,
  waits for `/actuator/health` = `UP`, then runs a harness-owned **read-back**
  (asserts seeded token `Q-P101` via `/api/v1/patients/tokens`) before
  printing `baseUrl` + `lifecycleId`. Fails closed and tears down on any failure.
- **`stop --lifecycle <id>`** — tears down **only** the process this harness
  created (by recorded PID group) and removes only its own lifecycle record.
- **`status --lifecycle <id>`** — reports a recorded lifecycle's health.

State-changing commands exit non-zero only on a **real** failure; benign
diagnostics (e.g. stopping an already-dead lifecycle) are reported, not fatal.
Lifecycle bookkeeping lives in `../artifacts/e2e/.lifecycles/` (git-ignored).

Playwright's `global-setup.mjs` starts one lifecycle for the run and exposes
`E2E_BASE_URL`; `global-teardown.mjs` stops only that lifecycle.

### Alternate startup mode

The selected startup mode is **single-jar** (recorded in every lifecycle
record's `startupMode` field). Building the JAR also compiles the frontend via
`frontend-maven-plugin`, so the SPA and API are always in sync.

---

## Commands

```bash
# One-time: install harness deps and browser
npm --prefix e2e install
npx --prefix e2e --no-install playwright install chromium

# One-time (or after backend changes): build the app JAR
(cd backend && ./mvnw clean package -DskipTests)

# Run the foundation smoke test (starts + tears down an isolated lifecycle)
npx --prefix e2e --no-install playwright test foundation.smoke.spec.ts

# Manually drive a lifecycle
node e2e/scripts/harness.mjs start --json
node e2e/scripts/harness.mjs status --lifecycle <id> --json
node e2e/scripts/harness.mjs stop --lifecycle <id> --json

# Generate an Allure report from results (note: NOT --clean; unsupported)
npx --prefix e2e --no-install allure awesome -o e2e/allure-report e2e/allure-results
```

## Evidence capture policy

Video, trace, and screenshots are forced **ON** for every test from
`playwright.config.ts` (`use: { trace, video, screenshot }`) — never a per-spec
opt-in. In campaign mode (`E2E_CAMPAIGN_SUITE` set), Allure results and
Playwright output go to **empty, campaign-owned** directories supplied via
`ALLURE_RESULTS_DIR` / `PLAYWRIGHT_OUTPUT_DIR`; the harness never copies shared
results into them, and `--only`/`forbidOnly` is enforced.

## Trust boundary

Shared foundation files are protected by `e2e-foundation.json` (sha256). Story
runs verify it with:

```bash
python3 ${CLAUDE_SKILL_DIR}/scripts/verify_allure_e2e.py --verify-foundation e2e/e2e-foundation.json
```

Story agents consume these files and report drift; they do not repair them
mid-campaign.
