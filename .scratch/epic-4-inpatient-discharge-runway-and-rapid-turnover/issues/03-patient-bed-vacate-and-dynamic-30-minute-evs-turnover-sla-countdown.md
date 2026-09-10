# 03: Patient Bed Vacate and Dynamic 30-Minute EVS Turnover SLA Countdown

**What to build:**
Accelerate the transition from patient departure to bed sanitization through automated turnover tracking. When a patient departs, the ward nurse executes a 1-click "Patient Vacated" action on the ward console (`POST /api/v1/patients/beds/{bedId}/vacate`), transitioning the bed from `OCCUPIED_TAKEN` (`Grey`) to `EMPTY_PENDING_CLEANING` (`Mustard Yellow`), setting `cleaningStartedAt`, and marking the admission request as `DISCHARGED`. The backend calculates dynamic 30-minute turnover tasks (`TurnoverTaskDto`) exposed via `GET /api/v1/ward/turnover-tasks`, tracking live remaining minutes and SLA status: `ON_TRACK` (> 10m remaining), `APPROACHING_SLA` (<= 10m remaining), and `BREACHED` (< 0m, overdue). The EVS Housekeeping console displays these tasks with real-time countdown timers, visual urgency color-coding, and overdue alerts, while emitting `VACATE_PATIENT` structured audit logs capturing vacate hour and `DischargedBeforeNoon` (KPI 19).

**Blocked by:** 02: Automated Morning Discharge Sign-Off and Bedside Medication Delivery Loop

**Status:** completed

- [x] Executing `POST /api/v1/patients/beds/{bedId}/vacate` transitions bed status to `EMPTY_PENDING_CLEANING`, records `cleaningStartedAt`, and sets `dischargedAt` on the associated `AdmissionRequest`.
- [x] Vacate action emits structured audit log `VACATE_PATIENT` capturing `BedNumber`, `VacateTimestamp`, `VacateHour`, and boolean `DischargedBeforeNoon` (KPI 19).
- [x] `GET /api/v1/ward/turnover-tasks` returns active vacated beds with `remainingMinutes` and `slaStatus` (`ON_TRACK`, `APPROACHING_SLA`, `BREACHED`) computed dynamically against the 30-minute cleaning SLA.
- [x] EVS Housekeeping Turnover Queue on the ward console displays active turnover tasks with dynamic real-time countdown timers and urgency badges.
- [x] Visual alerts highlight turnover tasks approaching SLA threshold (<= 10m) or breached (< 0m).
- [x] Integration tests verify vacate state changes, timestamp persistence, dynamic SLA countdown boundaries, and audit log emissions.
