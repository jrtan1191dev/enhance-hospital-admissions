# 03: Specialist Consult Evaluation, Consult Chaining & Structured Diversion Endorsement

**What to build:**
Enable claimed specialists to conduct clinical evaluations—submitting secondary acuity tiers, secondary telemetry constraints, consult impressions, and structured diversion pathways (`COMMUNITY_HOSPITAL` or `HOSPITAL_AT_HOME_MIC`). Reviewing specialists can also spawn chained secondary broadcasts to additional specialty clusters (`POST .../chain`) linked to the parent admission before concluding their review.

**Blocked by:** 02: Consult-Gated Admission & Multi-Cluster Broadcast Pool with Atomic Claiming

**Status:** ready-for-agent

- [ ] `SpecialistConsultRequest` accepts `secondaryAcuityTier`, `secondaryTelemetry`, `consultNotes`, and `diversionPathway`.
- [ ] Submitting a consult via `POST /api/v1/clinicians/specialist/broadcasts/{id}/consult` transitions the broadcast status to `COMPLETED` and emits `SUBMIT_SPECIALIST_CONSULT` audit log.
- [ ] Selecting `COMMUNITY_HOSPITAL` or `HOSPITAL_AT_HOME_MIC` recommends `TIER_4_SUBACUTE_DIVERSION` and populates diversion fields on the admission dossier.
- [ ] Reviewing specialist can chain a secondary consult via `POST /api/v1/clinicians/specialist/broadcasts/{id}/chain`, creating a new `OPEN` broadcast for the requested cluster linked by `parentBroadcastId`.
- [ ] Broadcast detail view displays linked chained consults and their real-time statuses.
- [ ] Service tests verify consult submission payloads, chained broadcast creation, and structured audit logs.
