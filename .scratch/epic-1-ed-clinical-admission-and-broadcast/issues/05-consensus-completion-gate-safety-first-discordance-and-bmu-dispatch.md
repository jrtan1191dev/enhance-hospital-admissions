# 05: Consensus Completion Gate, Safety-First Discordance & BMU Queue Dispatch

**What to build:**
Enforce the Consensus Completion Gate: an admission request held in `ASSESSMENT_PENDING` advances to `BED_REQUESTED` only when **all** primary and chained broadcasts reach `COMPLETED`. When the gate opens, the Discordance Engine compares evaluations, elevates `effectiveAcuityTier` to the highest acuity across the ED attending and all consulting specialists, enforces continuous telemetry (`effectiveTelemetry = true`) if flagged by any participant, evaluates discordance (`isDiscordant`), and dispatches the request to the BMU queue ordered strictly by `effective_acuity_tier` and `requested_at` with high-visibility discordance badges and side-by-side comparative notes.

**Blocked by:** 
- 03: Specialist Consult Evaluation, Consult Chaining & Structured Diversion Endorsement
- 04: Acuity-Driven SLA Countdown Timers & Cluster-Scoped Auto-Escalation

**Status:** completed

- [x] Consensus Completion Gate verifies all active and chained broadcasts linked to the admission request are `COMPLETED` before advancing status from `ASSESSMENT_PENDING` to `BED_REQUESTED`.
- [x] `effectiveAcuityTier` is computed as the highest acuity (lowest numerical ordinal) between the primary ED tier and all completed secondary specialist tiers, and persisted on `AdmissionRequest`.
- [x] `effectiveTelemetry` is persisted as `true` if either `primaryTelemetry` or any completed broadcast's `secondaryTelemetry` is `true`.
- [x] If any specialist acuity or telemetry diverges from the ED assessment, `isDiscordant` is set to `true`.
- [x] The completed admission request appears immediately in `GET /api/v1/bmu/queue`, sorted strictly by `effectiveAcuityTier` ASC, then `requestedAt` ASC.
- [x] BMU queue displays high-visibility `Discordance Flagged` badges, effective tier badges, and a modal/drawer showing side-by-side comparative notes from the ED attending and all specialists.
- [x] Tests verify multi-broadcast consensus gating, acuity escalation invariants, telemetry union logic, and queue ordering.
