# 01: Quiescent Triage State, 3-Stage Milestone Stepper, and Token Access Auditing

**What to build:**
Deliver the core token-activated public tracking experience for patients and families. When a patient opens their personal tracking URL without needing credentials, the interface reflects their admission journey through a clean 3-stage milestone stepper (`Admission Decision Confirmed` -> `Bed Assigned & Preparing Room` -> `Admitted to Inpatient Ward Bed`), or indicates that clinical evaluations are in progress (`ED Clinical Assessment in Progress`) when the admission is still pending decision. If a bed has been assigned, the tracker displays the patient's inpatient destination following the Level -> Ward -> Bed spatial hierarchy. Each access of the public tracker updates audit tracking timestamps and counters and emits structured access audit logs. In the evaluation simulator, reviewers can seamlessly switch simulated patient tokens from a quick-picker dropdown and observe automatic updates via live background polling.

**Blocked by:** None (can start immediately)

**Status:** completed

- [x] Public endpoint `GET /api/v1/patients/track/{token}` returns patient-safe milestone data without exposing internal clinical notes, diagnostic findings, or other patients' PII.
- [x] Looking up an invalid or non-existent token returns an RFC 7807 `404 Not Found` ProblemDetail.
- [x] Pre-admission triage state (`ASSESSMENT_PENDING`) renders a quiescent state indicating ED clinical assessment is ongoing before advancing to Milestone 1.
- [x] Active bed requests (`BED_REQUESTED`) activate Milestone 1 (`Admission Decision Confirmed & Bed Queued`).
- [x] Approved bed allocations (`BED_ALLOCATED`) automatically advance the tracker to Milestone 2 (`Bed Assigned & Preparing Room`) and display the assigned bed number, ward name, and floor level.
- [x] Bedside check-in (`ADMITTED_INPATIENT`) advances the tracker to Milestone 3 (`Admitted to Inpatient Ward Bed`).
- [x] Patient tracking access updates `first_tracker_accessed_at`, `last_tracker_accessed_at`, and increments `tracker_access_count` on `AdmissionRequest`, emitting a structured `TRACK_PATIENT_ACCESS` audit event (KPI 14).
- [x] Frontend mobile tracker displays the 3-stage visual milestone stepper inside a mobile phone mockup frame with 3-second background polling (`refetchInterval: 3000`).
- [x] Prototype interface includes a patient quick-picker dropdown populated by `GET /api/v1/patients/tokens` allowing one-click switching between simulated patient tokens.
- [x] Automated integration and web tests verify milestone transitions, token security, 404 handling, and access audit log emissions.
