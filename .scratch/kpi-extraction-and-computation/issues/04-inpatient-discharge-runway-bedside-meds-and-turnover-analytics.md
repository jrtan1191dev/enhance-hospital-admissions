# 04: Inpatient Discharge Runway, Bedside Meds, and Turnover Analytics

**What to build:**
Benchmark inpatient discharge efficiency, advance runway establishment, and bed turnover turnaround speed. Extend `KpiMetricsService` and the analytics summary API to compute Epic 4 metrics: percentage of patients vacating inpatient beds before 12:00 PM (KPI 19), advance runway establishment rate (percentage of admitted patients with an Estimated Date of Discharge recorded at least 48 hours prior to departure, KPI 20), bedside discharge medication delivery adoption rate (KPI 21), average housekeeping turnover cleaning latency in minutes, and 30-minute housekeeping SLA compliance percentage (KPI 22). In the `/analytics` dashboard, render the "Inpatient Discharge Runway & Rapid Turnover" domain section with morning discharge gauges, advance runway adoption cards, and room cleaning turnaround performance statistics.

**Blocked by:** 01: Analytics Platform Foundation & ED Clinical Intake Metrics

**Status:** completed

- [x] `KpiMetricsService` computes Epic 4 metrics: `dischargeBeforeNoonRatePct` (KPI 19), `advanceRunwayEstablishmentRatePct` (KPI 20), `bedsideMedicationDeliveryAdoptionPct` (KPI 21), `housekeepingTurnoverAvgMinutes`, and `housekeeping30mSlaCompliancePct` (KPI 22).
- [x] Discharge before noon evaluates `EXTRACT(HOUR FROM dischargedAt) < 12` over all completed discharges.
- [x] Advance runway establishment calculates the proportion of admitted/discharged patients who had an EDD recorded $\ge 48$ hours prior to discharge.
- [x] Turnover metrics compute average cleaning duration in minutes (`lastCleanedAt - cleaningStartedAt`) and the percentage completed within the 30-minute SLA threshold.
- [x] `/analytics` dashboard renders the Inpatient Discharge Runway & Rapid Turnover domain section with executive stat cards, target benchmarks (e.g. Target: $\ge 40\%$ before noon, SLA: $\le 30$ mins), and health status badges.
- [x] Integration tests verify noon discharge arithmetic, advance EDD evaluation, and cleaning latency calculations against test datasets.
