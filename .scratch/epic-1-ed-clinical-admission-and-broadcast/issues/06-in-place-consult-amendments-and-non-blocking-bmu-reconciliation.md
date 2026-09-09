# 06: In-Place Consult Amendments & Non-Blocking BMU Clinical Reconciliation

**What to build:**
Allow consulting specialists to amend their consult impressions, secondary acuity, or telemetry in-place via `PUT .../consult` while the patient is awaiting bed allocation in the BMU queue without resetting queue dwell time or kicking the patient from the queue, surfacing a `Clinical Condition Updated Alert` in the BMU queue. Enable BMU coordinators to trigger 1-click asynchronous reconciliation prompts (`reconciliationRequested = true`) that alert clinicians on their dashboards without blocking tentative bed allocation under safety-first elevated constraints.

**Blocked by:** 05: Consensus Completion Gate, Safety-First Discordance & BMU Queue Dispatch

**Status:** completed

- [x] `PUT /api/v1/clinicians/specialist/broadcasts/{id}/consult` allows an authenticated specialist to append notes, revise secondary acuity, or update telemetry on a completed consult.
- [x] Submitting an amendment updates `effectiveAcuityTier` and `effectiveTelemetry` in-place, re-evaluates `isDiscordant`, preserves queue dwell time (`requestedAt`), and emits an `AMEND_SPECIALIST_CONSULT` audit log.
- [x] BMU queue displays a prominent `Clinical Condition Updated` notification badge when an amendment is recorded for a queued request.
- [x] `POST /api/v1/bmu/requests/{id}/reconcile` sets `reconciliationRequested = true` on `AdmissionRequest` and emits `REQUEST_CLINICAL_RECONCILIATION` audit event.
- [x] Reconciliation alert badges appear on the ED attending's "Assessed Admissions" tracker tab and the specialist portal.
- [x] BMU coordinators can continue with tentative bed allocation while reconciliation is pending.
- [x] If a clinician updates their assessment so tiers and telemetry align, `isDiscordant` automatically clears to `false`.
- [x] Integration tests verify amendment in-place mechanics, queue stability, and non-blocking reconciliation flow.
