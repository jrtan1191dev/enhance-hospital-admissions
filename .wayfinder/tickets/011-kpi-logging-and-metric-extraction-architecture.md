# 011: Operational KPI Logging & Metric Extraction Architecture (ADR-011)

- **Type**: `wayfinder:prototype`
- **Status**: `closed`
- **Assignee**: `antigravity`
- **Blocked by**: [001-data-model-and-domain-entities.md](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/001-data-model-and-domain-entities.md), [009-enterprise-security-rbac-and-im8-audit.md](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/009-enterprise-security-rbac-and-im8-audit.md)
- **Blocks**: none

## Question

How should all 21 operational KPIs defined in [`product-idea/pain-points.md`](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/product-idea/pain-points.md) across Epics 1 through 4 be instrumented, logged, and extracted so that downstream reporting, compliance audits, and analytics pipelines can reliably compute them? What is the recommended extraction mechanism (direct relational SQL vs. CLI/log-parsing script) for each metric?

---

## Resolution (ADR-011: Dual-Pathway KPI Logging & Extraction Architecture)

To ensure operational metrics can be computed effortlessly across both immediate prototype runs and enterprise production deployments, the system adopts a **Dual-Pathway KPI Extraction Architecture**:

1. **Path A (Relational Persistence & SQL Analytics)**: Every domain entity extends `AuditableEntity` (capturing `created_at`, `created_by`, `last_modified_at`, `last_modified_by`) and stores domain-specific lifecycle timestamps (`requested_at`, `allocated_at`, `admitted_at`, `discharged_at`, `cleaning_started_at`, `last_cleaned_at`) and decision flags (`is_discordant`, `is_recommendation_accepted`, `override_reason_code`, `delay_reason_tag`). This enables high-performance, deterministic SQL queries for batch reports, BI dashboards, and historical trend analysis.
2. **Path B (Structured SLF4J / MDC Audit Logging & CLI Log-Parsing)**: Every state-altering action emits an immutable `[AUDIT]` log record via `AuditLogger` with MDC context (`auditUser`, `auditAction`, `auditTarget`) and structured key-value attributes in `details`. This enables zero-database-overhead real-time log ingestion (e.g. AWS CloudWatch, Splunk, OpenSearch) and immediate command-line verification via `grep`, `jq`, and `awk`.

---

### 1. Unified KPI Logging & Extraction Specification

| # | Operational KPI (`pain-points.md`) | Target Spec | Relational Columns (Path A) | Emitted Audit Action (Path B) | Recommended Computation Mechanism |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | **Primary ED Assessment Turnaround** | `epic-1` | `requested_at - created_at` | `SUBMIT_ED_ASSESSMENT` | **SQL**: `AVG(requested_at - created_at)` in minutes.<br>**Log**: Extract `ElapsedMins` from `SUBMIT_ED_ASSESSMENT`. |
| **2** | **Specialist Pick-Up & Consult Latency** | `epic-1` | `claimed_at - created_at`,<br>`completed_at - claimed_at` | `CLAIM_BROADCAST`,<br>`SUBMIT_SPECIALIST_CONSULT` | **SQL**: Average elapsed time in minutes from `assessment_broadcasts`.<br>**Log**: Parse `ElapsedClaimMins`. |
| **3** | **Primary vs Specialist Concordance Rate** | `epic-1` | `primary_acuity_tier = secondary_acuity_tier` | `SUBMIT_SPECIALIST_CONSULT` | **SQL**: `SUM(concordant) / COUNT(*)` over consulted requests.<br>**Log**: Count `Concordant=true` vs total consult logs. |
| **4** | **Admissions via Recommendations** | `epic-1` | `is_recommendation_accepted` | `SUBMIT_ED_ASSESSMENT` | **SQL**: `SUM(is_recommendation_accepted) / COUNT(*)`.<br>**Log**: Count `RecommendedAccepted=true`. |
| **5** | **Accepted Requests by Severity Tier** | `epic-1` & `epic-2` | `primary_acuity_tier` | `SUBMIT_ED_ASSESSMENT` | **SQL**: `GROUP BY primary_acuity_tier`.<br>**Log**: Tally `PrimaryAcuity={tier}` occurrences. |
| **6** | **BMU Phone Call Reduction** | `epic-1` & `epic-2` | `COUNT(admission_requests)` | `SUBMIT_ED_ASSESSMENT` | **SQL**: Ratio of digital requests vs total admissions (100% digital). |
| **7** | **BMU Suggestion Acceptance Rate** | `epic-2` | `assigned_bed_id`, `override_reason_code` | `ALLOCATE_BED` vs `OVERRIDE_ALLOCATION` | **SQL**: `SUM(is_accepted) / COUNT(allocations)`.<br>**Log**: `allocs / (allocs + overrides) * 100`. |
| **8** | **Sister Hospital Diversion Rate** | `epic-2` | `diversion_recommended`, `sister_hospital_referral_id` | `DIVERSION_REFERRAL` | **SQL**: `SUM(diversion_recommended) / COUNT(*)`.<br>**Log**: Count of `DIVERSION_REFERRAL` events. |
| **9** | **Sister Hospital Bilateral SLA Latency** | `epic-2` | `referrals.bilateral_response_mins` | `DIVERSION_REFERRAL` | **SQL**: Percentage of referrals with response $\le 30\text{ mins}$. |
| **10** | **Capacity Utilization Gain** | `epic-2` | `beds.status = 'OCCUPIED_TAKEN'` | `ALLOCATE_BED` | **SQL**: Delta in occupied bed hours vs baseline capacity. |
| **11** | **Ghost Bed Reduction** | `epic-2` | `locked_gender`, `locked_infection_status` | `ALLOCATE_BED` | **SQL**: Recovered bed-hours from multi-bed consolidation packing. |
| **12** | **Batch Holding Room Adoption** | `epic-2` | `placed_via_batch = true` | `APPROVE_BATCH_HOLDING_WARD` | **SQL**: `SUM(batch_placed) / COUNT(total_placed)`.<br>**Log**: Sum of `BatchSize` across batch approval logs. |
| **13** | **Cohort-Swap Optimization Yield** | `epic-2` | `swap_audit_log.unlocked_capacity` | `APPROVE_COHORT_SWAP` | **Log**: Count of `APPROVE_COHORT_SWAP` occurrences. |
| **14** | **Patient Portal Access Rate** | `epic-3` | `first_tracker_accessed_at IS NOT NULL` | `TRACK_PATIENT_ACCESS` | **SQL**: `SUM(accessed) / COUNT(dispatched)`.<br>**Log**: Unique `PatientToken` access count vs dispatched. |
| **15** | **2-Hour Periodic Update Delivery** | `epic-3` | `last_periodic_update_sent_at` | `DISPATCH_PERIODIC_UPDATE` | **SQL**: Delivery compliance for requests boarding $\ge 120\text{ mins}$.<br>**Log**: Total `DISPATCH_PERIODIC_UPDATE` events. |
| **16** | **Prolonged-Wait Communication Rate** | `epic-2` & `epic-3` | `delay_reason_tag IS NOT NULL` | `TAG_DELAY_REASON` | **SQL**: Tagged cases divided by cases with dwell time $> 60\text{ mins}$. |
| **17** | **Reduction in Nursing Desk Inquiries** | `epic-3` | `nursing_inquiry_log` | `LOG_NURSING_DESK_INQUIRY` | **Log/SQL**: Reduction in desk status inquiries vs historical baseline. |
| **18** | **Early MSW & Financial Counseling Connect**| `epic-3` | `patient_audit_interactions` | `CONNECT_MSW_HOTLINE`, `CONNECT_FINANCIAL_COUNSELING` | **SQL/Log**: Total 1-click calls initiated from FYI explainer card. |
| **19** | **Discharge Before 12:00 PM** | `epic-4` | `EXTRACT(HOUR FROM discharged_at) < 12` | `VACATE_PATIENT` | **SQL**: `SUM(discharged_before_noon) / COUNT(discharges)`.<br>**Log**: Count `DischargedBeforeNoon=true`. |
| **20** | **Early Caregiver Engagement Rate** | `epic-4` | `checklist_completed_at < DATE(discharged_at)` | `COMPLETE_CAREGIVER_CHECKLIST` | **SQL**: Completed checklists prior to discharge morning. |
| **21** | **Bedside Medication Delivery Adoption** | `epic-4` | `medication_orders.status = 'DELIVERED_BEDSIDE'` | `DELIVER_BEDSIDE_MEDICATION` | **SQL**: Delivered bedside orders / total discharge orders. |
| **22** | **Bed Turnover Cleaning Latency (<30m SLA)**| `epic-2` & `epic-4` | `last_cleaned_at - cleaning_started_at` | `CLEAN_BED` | **SQL**: `AVG(elapsed_clean_mins)` and `% <= 30.0 mins`.<br>**Log**: Parse `ElapsedCleaningMins` and `Within30mSla=true`. |

---

### 2. Concrete Metric Extraction Recipes

#### Recipe A: SQL Batch Reporting Query (PostgreSQL / H2)
```sql
-- Comprehensive Executive Admission & Turnover KPI Report
SELECT 
  -- ED Assessment Latency
  ROUND(AVG(EXTRACT(EPOCH FROM (a.requested_at - a.created_at)) / 60.0), 1) AS avg_ed_turnaround_mins,
  -- Primary vs Specialist Concordance
  ROUND(100.0 * SUM(CASE WHEN a.primary_acuity_tier = a.secondary_acuity_tier THEN 1 ELSE 0 END) 
        / NULLIF(COUNT(a.secondary_acuity_tier), 0), 1) AS concordance_rate_pct,
  -- BMU Suggestion Acceptance
  ROUND(100.0 * SUM(CASE WHEN a.is_recommendation_accepted = true THEN 1 ELSE 0 END) 
        / NULLIF(COUNT(a.assigned_bed_id), 0), 1) AS bmu_suggestion_acceptance_pct,
  -- Discharge Before Midday
  ROUND(100.0 * SUM(CASE WHEN EXTRACT(HOUR FROM a.discharged_at) < 12 THEN 1 ELSE 0 END) 
        / NULLIF(COUNT(a.discharged_at), 0), 1) AS discharge_before_noon_pct
FROM admission_requests a;
```

#### Recipe B: Command-Line Log Analysis Scripts (Bash / jq / awk)
```bash
# 1. Compute BMU Suggestion Acceptance Rate from console/file logs:
allocs=$(grep -c 'action="ALLOCATE_BED"' application.log)
overrides=$(grep -c 'action="OVERRIDE_ALLOCATION"' application.log)
echo "scale=2; ($allocs / ($allocs + $overrides)) * 100" | bc | awk '{print "BMU Suggestion Acceptance Rate: " $1 "%"}'

# 2. Compute 30-Minute Housekeeping Cleaning Turnaround Compliance:
grep 'action="CLEAN_BED"' application.log | \
  sed -n 's/.*ElapsedCleaningMins=\([0-9.]*\).*/\1/p' | \
  awk '{sum+=$1; count++; if($1<=30.0) ok++} END {
    print "Average Cleaning Duration: " sum/count " mins";
    print "30-min SLA Compliance Rate: " (ok/count)*100 "%";
  }'

# 3. Compute Patient Public Milestone Tracker Access Rate:
dispatched=$(grep -c 'action="SUBMIT_ED_ASSESSMENT"' application.log)
accessed=$(grep -o 'target="PatientToken:[^"]*"' application.log | sort -u | wc -l)
echo "scale=2; ($accessed / $dispatched) * 100" | bc | awk '{print "Public Tracker Adoption Rate: " $1 "%"}'
```

---

### 3. Traceability to Epic Specs

All four specifications in `specs/` now include dedicated `## KPI Instrumentation & Extraction Recommendations` sections matching this resolution:
- [`specs/epic-1-ed-clinical-admission-and-broadcast.md`](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/specs/epic-1-ed-clinical-admission-and-broadcast.md)
- [`specs/epic-2-bmu-bed-capacity-and-dynamic-flex-batching.md`](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/specs/epic-2-bmu-bed-capacity-and-dynamic-flex-batching.md)
- [`specs/epic-3-patient-and-family-milestone-queue-tracker.md`](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/specs/epic-3-patient-and-family-milestone-queue-tracker.md)
- [`specs/epic-4-inpatient-discharge-runway-and-rapid-turnover.md`](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/specs/epic-4-inpatient-discharge-runway-and-rapid-turnover.md)
