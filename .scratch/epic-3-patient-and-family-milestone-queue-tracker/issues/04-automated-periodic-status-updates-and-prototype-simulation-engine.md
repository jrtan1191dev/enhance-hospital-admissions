# 04: Automated Periodic Status Updates and Prototype Simulation Engine

**What to build:**
Keep waiting patients and families informed automatically while reducing repetitive inquiries at nursing desks. A background notification scheduler identifies active bed requests waiting in queue $\ge 5$ minutes (prototype mode) or $\ge 2$ hours (production mode) and dispatches automated status refreshes, updating `last_periodic_update_sent_at` on the admission record and emitting structured `DISPATCH_PERIODIC_UPDATE` audit events. Under `@Profile("prototype")`, a dedicated REST endpoint `POST /api/v1/patients/simulate-periodic-update` and UI trigger allow evaluators to trigger the notification cycle on demand and observe simulated delivery confirmation without waiting for timer expiration.

**Blocked by:** 01: Quiescent Triage State, 3-Stage Milestone Stepper, and Token Access Auditing

**Status:** completed

- [x] Periodic update scheduler identifies active `BED_REQUESTED` admissions with dwell time $\ge 5$ minutes in prototype mode ($\ge 120$ minutes in production).
- [x] Dispatching updates sets `last_periodic_update_sent_at` on `AdmissionRequest` and emits structured audit log `DISPATCH_PERIODIC_UPDATE` capturing channel, token, milestone step, dwell minutes, and delivery status (KPI 15).
- [x] Endpoint `POST /api/v1/patients/simulate-periodic-update` is exposed under `@Profile("prototype")` to trigger a simulated notification broadcast cycle for all waiting patients.
- [x] Milestone progression triggers immediate notification dispatch / audit logging upon state advancement.
- [x] Prototype tracker UI includes a "Simulate Periodic Update" action button in the simulator controls with instant visual toast confirmation.
- [x] Automated tests verify periodic update selection criteria, timestamp updating, prototype endpoint execution, and structured audit log emissions.
