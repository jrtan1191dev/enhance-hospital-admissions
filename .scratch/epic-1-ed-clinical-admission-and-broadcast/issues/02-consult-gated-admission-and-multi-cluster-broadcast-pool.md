# 02: Consult-Gated Admission & Multi-Cluster Broadcast Pool with Atomic Claiming

**What to build:**
Enable ED attending physicians to submit Consult-Gated Admissions (`requiresSpecialistConsult = true`) selecting one or more target specialty clusters (`targetClusters: Set<SpecialtyCluster>`). The admission request is held in `ASSESSMENT_PENDING` (hidden from the BMU queue) while concurrent `AssessmentBroadcast` records are published to specialty cluster feeds. Inpatient specialists can filter broadcasts by cluster, view demand badges, and claim cases atomically with optimistic locking (`@Version`) that prevents concurrent race conditions by returning HTTP 409 Conflict.

**Blocked by:** 01: Direct Admission Flow, Baseline Override Auditing & ED Board Separation

**Status:** ready-for-agent

- [ ] `EdAssessmentSubmitRequest` accepts `requiresSpecialistConsult = true` and a non-empty `targetClusters` set.
- [ ] Submitting a consult-gated admission sets `AdmissionRequest.status = ASSESSMENT_PENDING` and persists an `AssessmentBroadcast` in `OPEN` status for each requested specialty cluster.
- [ ] Active consult-gated requests do not appear in the active BMU bed allocation queue (`GET /api/v1/bmu/queue`).
- [ ] `GET /api/v1/clinicians/specialist/broadcasts` retrieves broadcasts with cluster filtering and urgency indicators.
- [ ] `POST /api/v1/clinicians/specialist/broadcasts/{id}/claim` transitions broadcast to `CLAIMED` and assigns `claimedBySpecialistId`.
- [ ] `AssessmentBroadcast` uses targeted `@Version` optimistic locking; concurrent claims on the same broadcast fail fast with HTTP 409 Conflict and RFC 7807 Problem Details.
- [ ] Successful claim emits `CLAIM_BROADCAST` audit log with specialist identity and elapsed pick-up minutes.
- [ ] End-to-end integration and concurrency tests verify multi-cluster publication and race-condition prevention.
