# 04: Terminal Sanitization Enforcement, Closed-Loop BMU Bed Release, and Audit Compliance

**What to build:**
Complete the closed-loop circular bed capacity lifecycle by enforcing strict bed state machine invariants, certifying terminal sanitization, and immediately releasing clean beds back into BMU allocation algorithms. When housekeeping completes sanitization, tapping "Sign-Off Clean" invokes `POST /api/v1/patients/beds/{bedId}/clean`. The service validates that the bed is currently in `EMPTY_PENDING_CLEANING`; calling clean on any bed in an invalid state returns RFC 7807 `400 Bad Request`. Upon valid clean sign-off, the bed transitions to `EMPTY_CLEANED` (`White`), records `lastCleanedAt`, clears `currentPatient`, and calculates `ElapsedCleaningMins` and `Within30mSla` for structured `CLEAN_BED` audit logging (KPI 22). Newly cleaned `White` beds instantly become scored and available in live BMU recommendation queries for waiting ED patients, and an end-to-end integration test verifies the complete closed loop.

**Blocked by:** 03: Patient Bed Vacate and Dynamic 30-Minute EVS Turnover SLA Countdown

**Status:** ready-for-agent

- [ ] `POST /api/v1/patients/beds/{bedId}/clean` validates that the target bed is strictly in `EMPTY_PENDING_CLEANING` status, rejecting invalid attempts with RFC 7807 `400 Bad Request`.
- [ ] Valid clean sign-off transitions bed status to `EMPTY_CLEANED` (`White`), sets `lastCleanedAt`, and dissociates the discharged patient (`currentPatient = null`).
- [ ] Clean sign-off emits structured audit log `CLEAN_BED` capturing `BedNumber`, `HousekeeperId`, `ElapsedCleaningMins`, and `Within30mSla` boolean compliance (KPI 22).
- [ ] BMU live candidate recommendation queries immediately score and include newly cleaned `White` beds for waiting ED admissions without latency or restart.
- [ ] Ward console updates in real time, removing cleaned beds from the EVS turnover queue and rendering them as available (White) beds.
- [ ] End-to-end closed-loop integration tests verify the complete journey: morning discharge sign-off -> bedside meds -> nurse vacate -> 30-minute turnover tracking -> housekeeping sign-off -> instant BMU candidate re-allocation.
