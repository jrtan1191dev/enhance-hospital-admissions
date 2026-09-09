# 05: Path B Automated Audit Log Extraction CLI Scripts and Verification

**What to build:**
Deliver the Path B operational log extraction toolchain and verify cross-pathway mathematical parity between relational SQL analytics (Path A) and structured audit logs (Path B). Implement standalone, portable CLI shell scripts (`scripts/kpi-extract-all.sh`, `scripts/kpi-extract-audit.sh`) leveraging standard Unix utilities (`grep`, `jq`, `awk`) to parse structured `[AUDIT]` log lines from application server logs, calculating all 21 operational KPIs directly from disk without database queries. Provide an automated reconciliation integration test that executes an end-to-end clinical workflow scenario, compares the relational API output of `GET /api/v1/analytics/kpis/summary` with the CLI script extraction output from the generated log file, and asserts 100% mathematical consistency and non-repudiation.

**Blocked by:** 01: Analytics Platform Foundation & ED Clinical Intake Metrics, 02: BMU Bed Capacity, Heuristic Acceptance, and Diversion Analytics, 03: Patient Journey, Periodic Push, and Caregiver Counseling Analytics, 04: Inpatient Discharge Runway, Bedside Meds, and Turnover Analytics

**Status:** ready-for-agent

- [ ] Shell script `scripts/kpi-extract-all.sh` accepts a log file path argument and parses `[AUDIT]` entries across all four operational domains using `grep`, `jq`, and `awk`.
- [ ] Script computes formatted metrics matching the 21 KPI definitions (averages, P95 percentiles, rates, counts, SLA compliance percentages) directly from structured log key-value pairs in `details`.
- [ ] Script gracefully handles empty log files or absent action tags, returning clean 0-counts and formatted zero-percentages.
- [ ] Integration test generates an end-to-end workflow scenario capturing all 18 standard audit event tags to an active log file.
- [ ] Test executes the log extraction script and compares its calculated KPI metrics against Path A relational query results from `KpiAnalyticsController`.
- [ ] Automated assertions verify exact mathematical agreement across both extraction pathways, proving non-repudiation and auditability.
