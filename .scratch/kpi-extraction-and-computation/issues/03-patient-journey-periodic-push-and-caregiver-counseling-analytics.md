# 03: Patient Journey, Periodic Push, and Caregiver Counseling Analytics

**What to build:**
Measure digital patient engagement, communication consistency, and caregiver support counseling. Extend `KpiMetricsService` and the analytics API to compute Epic 3 metrics: public patient milestone tracker access rate (percentage of admitted patients who access their token URL, KPI 14), 2-hour periodic update delivery rate for patients boarding $\ge 120$ minutes in queue (or $\ge 5$ minutes in prototype mode, KPI 15), prolonged-wait delay communication rate (percentage of long-waiting patients with recorded delay reason tags), and early caregiver counseling connect rates (1-click calls to MSW and Financial Counseling among recommended diversion candidates, KPI 18). In the `/analytics` dashboard, render the "Patient & Family Milestone Tracking" domain section with engagement gauges, notification delivery compliance badges, and counseling demand indicators.

**Blocked by:** 01: Analytics Platform Foundation & ED Clinical Intake Metrics

**Status:** ready-for-agent

- [ ] `KpiMetricsService` computes Epic 3 metrics: `patientTrackerAccessRatePct` (KPI 14), `twoHourPeriodicUpdateDeliveryPct` (KPI 15), `prolongedWaitCommunicationRatePct`, and caregiver counseling connect rates (KPI 18).
- [ ] Access rate calculation identifies distinct admitted requests with `firstTrackerAccessedAt IS NOT NULL` over total dispatched bed requests.
- [ ] Periodic update delivery evaluates patients waiting in queue $\ge 120$ minutes (or prototype threshold) who received at least one periodic refresh broadcast.
- [ ] Counseling connect rate tracks 1-click calls recorded in `patient_audit_interactions` for MSW and Financial Counseling hotlines among diversion candidates.
- [ ] `/analytics` dashboard renders the Patient & Family Milestone Tracking domain section featuring engagement rate stat cards and compliance badges.
- [ ] Integration tests verify access rate calculations, periodic update tracking queries, and counseling interaction aggregation.
