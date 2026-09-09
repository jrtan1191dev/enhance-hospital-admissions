# 06: Acuity Dwell SLA Tracking & Operational Delay Communication

**What to build:**
Deliver continuous dwell-time monitoring against acuity-tier SLAs and establish closed-loop operational delay communication between BMU and ED floor nurses. Active admission requests in the BMU queue display live dwell timers measuring elapsed minutes since `requestedAt`, with visual escalation badges whenever wait time breaches clinical acuity thresholds (e.g. 60 minutes for Tier 2 Urgent, 120 minutes for Tier 3 Stable). BMU coordinators possess exclusive operational authority to attach structured delay reason tags (`HOUSEKEEPING_DELAY`, `BED_SHORTAGE`, `SPECIALIZED_ISOLATION_CLEANING`, `SURGE_TRAUMA_EVENT`) with explanatory notes. The ED triage console immediately surfaces these delay tags alongside canned family talking points so floor nurses can proactively reassure waiting patients without phone calls. When a bed is allocated (`BED_ALLOCATED`), active delay tags are automatically archived into historical audit reporting. Additionally, specialist consult amendments that increase acuity tier or add continuous telemetry dynamically elevate queue positioning and trigger high-visibility `CLINICAL_CONDITION_UPDATED` alerts.

**Blocked by:** 01: Two-Tier Constraint Validation & Override Workflow

**Status:** ready-for-agent

- [ ] BMU queue calculates elapsed dwell minutes from `requestedAt` and renders color-coded dwell timers for each waiting patient.
- [ ] Visual escalation badges highlight patients exceeding acuity-tier dwell SLA thresholds (Tier 1: immediate, Tier 2: >60 mins, Tier 3: >120 mins).
- [ ] Endpoint `POST /api/v1/bmu/requests/{requestId}/delay-tag` allows BMU coordinators to attach a structured `DelayReasonCode` and explanatory operational note.
- [ ] Non-BMU personas attempting to submit delay reason tags are rejected with HTTP 403 Forbidden.
- [ ] ED triage board displays attached delay reason tags on patient rows along with contextual family talking points.
- [ ] When an admission request transitions to `BED_ALLOCATED`, active delay tags are automatically archived from the active alert view and preserved in audit records.
- [ ] Specialist consult amendments that increase acuity or require telemetry automatically re-sort queue rank and display a prominent `CLINICAL_CONDITION_UPDATED` badge.
- [ ] Structured audit event `TAG_DELAY_REASON` is emitted recording coordinator identity, delay code, dwell minutes, and patient acuity tier.
- [ ] Unit and integration tests verify dwell timer calculations, SLA breach alerts, coordinator-exclusive delay tagging, auto-archival on allocation, and consult amendment queue elevation.
