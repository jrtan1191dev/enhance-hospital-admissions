# 01: Direct Admission Flow, Baseline Override Auditing & ED Board Separation

**What to build:**
Enable Emergency Department attending physicians to review unassessed patients with synthesized EHR vitals and lab panels, adjust clinical chips with structured override tracking, and submit a Direct Admission (`requiresSpecialistConsult = false`) that dispatches directly to the BMU queue in `BED_REQUESTED` status. The moment an admission decision is submitted, the patient is removed from the active "Awaiting Assessment" list and transitions to a dedicated "Assessed Admissions" tracker tab displaying real-time admission progress.

**Blocked by:** None (can start immediately)

**Status:** completed

- [x] `GET /api/v1/clinicians/ed/patients` returns only active ED patients awaiting initial assessment (excluding patients with existing `AdmissionRequest` records).
- [x] ED intake UI displays pre-populated EHR diagnostic baselines (vital signs, Troponin/WBC/Hb, pending scan badges, and ward class preference).
- [x] Modifying any pre-populated parameter captures structured field-level deltas (`ClinicalBaselineOverride[]`) in `EdAssessmentSubmitRequest`.
- [x] Submitting with `requiresSpecialistConsult = false` persists an `AdmissionRequest` with `status = BED_REQUESTED`, `effectiveAcuityTier = primaryAcuityTier`, and `effectiveTelemetry = primaryTelemetry`.
- [x] If overrides are present, `isRecommendationAccepted` is set to `false` and an `OVERRIDE_CLINICAL_BASELINE` audit event is emitted via `AuditLogger`.
- [x] Submitted patient disappears immediately from the "Awaiting Assessment" queue and appears in the "Assessed Admissions" tab via `GET /api/v1/clinicians/ed/admissions`.
- [x] Web and service layer tests verify validation, status transitions, override auditing, and queue filtering.
