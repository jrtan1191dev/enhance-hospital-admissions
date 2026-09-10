# 02: Ward-Class Partitioned Queue Position, Non-FIFO Clinical Urgency Banner, and Acuity Ordering

**What to build:**
Provide patients with accurate, transparent queue expectations while removing misconceptions about first-come, first-served admissions. In the backend, dynamic queue position is calculated by partitioning active bed requests by the patient's matching ward class (`requestedWardClass`) and ordering by clinical priority (`effectiveAcuityTier`, where higher urgency precedes lower urgency) and request timestamp (`requestedAt`), yielding the patient's queue rank and count of patients ahead. In the mobile tracker interface, the patient sees their exact position and patients ahead within their ward category, accompanied by a compassionate educational banner explaining that hospital bed allocation is determined by acute clinical urgency and infection control safety rather than arrival time.

**Blocked by:** 01: Quiescent Triage State, 3-Stage Milestone Stepper, and Token Access Auditing

**Status:** completed

- [x] Queue position calculator evaluates active `BED_REQUESTED` admissions partitioned by the patient's requested ward class (`requestedWardClass`).
- [x] Queue ranking sorts patients by `effectiveAcuityTier` (highest acuity first) and secondarily by `requestedAt` timestamp (earliest request first).
- [x] `PatientMilestoneResponse` payload includes `queuePosition` (1-indexed position in ward queue) and `patientsAhead` (`queuePosition - 1`), returning 0 once allocated or admitted.
- [x] Mobile tracker interface displays the count of patients ahead scoped to the patient's ward class category.
- [x] Mobile tracker interface prominently displays an empathetic non-FIFO clinical priority banner explaining that admissions are prioritized by acute clinical urgency and infection prevention rather than first-come-first-served sequence.
- [x] Unit and integration tests verify queue position and patients ahead calculations across multiple ward classes and differing acuity tiers.
