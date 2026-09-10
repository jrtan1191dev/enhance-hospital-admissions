# 02: BMU Bed Capacity, Heuristic Acceptance, and Diversion Analytics

**What to build:**
Quantify the operational performance of central bed management, algorithmic matching, and alternative care diversions. Extend `KpiMetricsService` and the `HospitalKpiSummaryDto` API to calculate Epic 2 capacity indicators: BMU recommendation acceptance rate (1-click approvals vs. manual overrides), total manual override count with a structured breakdown by reason code, total diversion count and overall diversion rate percentage, bilateral sister hospital SLA compliance rate (within the 30-minute transfer window), and batch holding ward adoption rate. In the frontend `/analytics` dashboard, render the "BMU Capacity Orchestration & Diversions" domain section with executive stat cards, override reason distributions, and visual transfer SLA compliance indicators.

**Blocked by:** 01: Analytics Platform Foundation & ED Clinical Intake Metrics

**Status:** completed

- [x] `KpiMetricsService` computes Epic 2 metrics: `bmuSuggestionAcceptanceRatePct`, `bmuManualOverrideCount`, `totalDiversionCount`, `diversionRatePct`, `sisterHospitalSlaCompliancePct`, and `batchHoldingWardAdoptionRatePct`.
- [x] Override metrics extract structured breakdown counts across reason codes (`GOVERNMENT_SUBSIDY_CLASS_UPGRADE`, `EMERGENCY_PORTABLE_TELEMETRY_DEPLOYED`, `NON_TOP_RANK_SELECTION`).
- [x] Transfer SLA compliance evaluates elapsed minutes between `referralDispatchedAt` and completion against the 30-minute SLA window.
- [x] `/analytics` dashboard displays the BMU Capacity Orchestration & Diversions domain section with stat cards, target benchmarks, and health badges.
- [x] Dashboard renders a visual breakdown of structured override reasons and alternative care diversion channels (Community Hospitals vs. MIC@Home).
- [x] Integration tests verify metric computations across varied allocation states, overrides, sister hospital referrals, and holding ward batches.
