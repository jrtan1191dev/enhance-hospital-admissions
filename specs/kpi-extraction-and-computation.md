# Feature Specification: Operational KPI Logging, Extraction & Analytics Platform

## Problem Statement

Hospital executives, clinical chairs, Bed Management Unit (BMU) directors, and operational analysts face significant challenges in objectively measuring and optimizing patient flow across emergency admissions, inpatient capacity, and bed turnover. While hospitals invest heavily in digital workflows—such as multi-doctor assessment broadcasts, algorithmic bed matching, public queue trackers, and rapid housekeeping turnovers—they lack a unified, automated mechanism to track and extract operational Key Performance Indicators (KPIs).

Currently, hospital stakeholders encounter critical measurement bottlenecks:

1. **Fragmented Data Silos & Manual Reporting**: Operational data is scattered across emergency triage notes, specialist consultation logs, bed management spreadsheets, and housekeeping clipboards. Compiling key turnaround metrics requires laborious manual collation, retrospective chart reviews, and error-prone reconciliation.
2. **Inability to Quantify Operational Interventions**: Leadership cannot readily verify whether system enhancements (such as direct digital bed requests, holding ward batching, or bedside discharge medication delivery) achieve their intended targets, such as eliminating phone calls, reducing ghost capacity, or clearing acute beds before 12:00 PM.
3. **Opaque Root-Cause Attribution for Delays**: When patient boarding exceeds clinical SLA thresholds, managers cannot distinguish whether delays stem from diagnostic compilation latency, specialist consult backlogs, housekeeping turnaround gaps, or caregiver unreadiness.
4. **Performance Penalties of Ad-Hoc Analytics**: Running complex, unindexed analytical queries against live transactional healthcare databases during peak hours risks degrading core clinical workflows. Conversely, attempting to parse arbitrary unstructured log text leads to brittle scripts that fail whenever log wording changes.

## Solution

The system provides a standardized, high-performance **Operational KPI Logging, Extraction & Analytics Platform** that unifies metric capture across Epics 1 through 4 via a **Dual-Pathway Extraction Architecture**:

1. **Path A: Relational Database Audit Fields & Pre-Aggregated SQL Views**:
   - Persists state-transition timestamps (`created_at`, `requested_at`, `allocated_at`, `admitted_at`, `discharged_at`, `cleaning_started_at`, `last_cleaned_at`) and decision flags (`is_discordant`, `is_recommendation_accepted`, `override_reason_code`, `delay_reason_tag`, `diversion_recommended`) directly on domain entities extending `AuditableEntity`.
   - Provides optimized, indexed relational SQL queries and views for batch reporting, executive summaries, and Business Intelligence (BI) dashboards (e.g., Metabase, Tableau, PowerBI).
2. **Path B: Standardized Structured SLF4J / MDC Audit Logging**:
   - Emits structured, machine-parseable `[AUDIT]` log statements via `AuditLogger` enriched with Mapped Diagnostic Context (`auditUser`, `auditAction`, `auditTarget`) and structured key-value pairs in the log body.
   - Enables zero-overhead real-time log ingestion into enterprise SIEM and observability tools (AWS CloudWatch, OpenSearch, Splunk, Datadog) as well as immediate command-line verification via standard Unix utilities (`grep`, `jq`, `awk`).
3. **Automated KPI Analytics Service & REST API**:
   - Implements a dedicated `KpiMetricsService` and `KpiAnalyticsController` delivering pre-aggregated summary metrics, compliance percentages, and latency percentiles ($P_{50}$, $P_{95}$) across all 21 hospital performance indicators.

---

## User Stories

### 1. Emergency Department & Clinical Collaboration Metrics (Epic 1)

1. As an Emergency Department Director, I want the system to calculate the average and 95th-percentile Primary ED Assessment Turnaround Time, so that I can evaluate whether diagnostic baseline pre-population reduces physician intake latency.
2. As a Clinical Quality Lead, I want the system to compute the Specialist Broadcast Pick-Up and Consult Latency by specialty cluster, so that consult bottlenecks in Cardiology, General Medicine, or Surgery can be identified.
3. As a Patient Safety Officer, I want the system to compute the Primary vs. Specialist Concordance Rate across all multi-doctor assessments, so that inter-departmental triage divergence can be monitored and audited.
4. As an ED Operations Analyst, I want the system to calculate the percentage of clinical assessments submitted using system-recommended baselines without manual chip adjustments, so that AI diagnostic synthesis adoption can be quantified.
5. As a Hospital Operations Director, I want a breakdown of all digital bed requests categorized by Acuity Tiers (Tiers 1–5), so that hospital-wide admission severity trends can be tracked over time.
6. As an ED Charge Nurse, I want the system to track the total volume of digital bed requests dispatched to BMU to verify 100% elimination of synchronous telephone calls between ED and bed planners.

### 2. BMU Bed Allocation, Capacity & Diversion Metrics (Epic 2)

1. As a BMU Operations Manager, I want the system to track the BMU Suggestion Acceptance Rate (percentage of 1-click approvals vs. manual overrides), so that heuristic algorithm accuracy can be continuously tuned.
2. As a BMU Operations Manager, I want to view a categorized breakdown of structured override reasons (e.g., staffing constraint, cubicle maintenance), so that unmodelled operational constraints can be incorporated into algorithm weights.
3. As a Hospital Capacity Director, I want to track the Number and Percentage of Accepted Requests Routed to Sister Hospitals (OCH, AH, SACH) and Virtual Beds (MIC@Home), so that community hospital diversion performance can be measured.
4. As a Sister Hospital Liaison, I want to track Sister Hospital Bilateral Acceptance Latency against the 30-minute SLA window, so that external transfer response compliance can be audited.
5. As a BMU Coordinator, I want the system to measure Effective Bed Capacity Utilization Gain and Ghost Bed Hours Recovered achieved through Phase 1 consolidation packing, so that multi-bed cubicle efficiency gains can be proven.
6. As a BMU Coordinator, I want to track the Batch Holding Ward Adoption Rate (percentage of patients admitted via batch recommendations), so that bulk admission efficiency can be monitored.
7. As a BMU Coordinator, I want to record the frequency and bed-hours recovered through Dynamic Cohort-Swaps, so that capacity unlocked prior to ED departure can be quantified.
8. As an Operations Analyst, I want to calculate the Prolonged-Wait Delay Communication Rate (percentage of long-waiting patients with recorded delay reason tags), so that compliance with proactive nursing communication protocols can be enforced.

### 3. Patient Journey & Family Communication Metrics (Epic 3)

 1. As a Chief Patient Experience Officer, I want to track the Public Patient Milestone Tracker Access Rate (percentage of admitted patients who access their tracking link), so that digital patient engagement can be measured.
 2. As a Patient Experience Lead, I want the system to audit the Periodic Update Delivery Compliance Rate for patients boarding in the queue (every 5 minutes in prototype evaluation), so that communication consistency is guaranteed.
 3. As an ED Nurse Manager, I want the system to correlate public milestone tracker adoption with reductions in patient and family inquiries at the triage desk, so that nursing workload alleviation can be evaluated.
 4. As a Medical Social Work Lead, I want to track the 1-click contact rates for Medical Social Work (MSW) and Financial Counseling initiated from the patient tracker explainer card, so that caregiver counseling demand can be forecasted.

### 4. Inpatient Discharge Runway & Turnover Metrics (Epic 4)

 1. As an Inpatient Clinical Director, I want the system to track the percentage of patients Vacating Inpatient Beds Before 12:00 PM, so that acute bed availability for afternoon ED admissions is maximized.
 2. As an Inpatient Care Coordinator, I want to measure the Advance Runway Establishment Rate (percentage of admitted patients with an Estimated Date of Discharge recorded at least 48 hours prior to departure), so that capacity planning can anticipate bed vacancies.
 3. As an Inpatient Pharmacy Director, I want to track the Adoption Rate of Bedside Discharge Medication Delivery via ward console sign-offs, so that elimination of outpatient pharmacy collection queues can be measured.
 4. As an Environmental Services (EVS) Director, I want to track the Bed Turnover Cleaning Latency and 30-Minute Housekeeping SLA Compliance Rate, so that cleaning performance and room turnover speed can be benchmarked.

### 5. Data Governance & Operational Reporting

 1. As a BI / Data Engineer, I want optimized relational SQL queries and database views that compute all 21 KPIs without performing unindexed full table scans, so that analytical reporting runs cleanly without impacting transactional services.
 2. As a DevOps / System Administrator, I want standardized CLI scripts (`grep`, `jq`, `awk`) capable of parsing audit log files in seconds, so that operational metric snapshots can be extracted directly from application servers.
 3. As a Compliance Auditor, I want every metric computation to link back to auditable entity records and timestamped user events, so that reported KPIs are fully reproducible and non-repudiable under hospital governance policies.

---

## Implementation Decisions

### 1. Modules & Domain Boundaries

- **`KpiMetricsService`**: Central analytics service in the backend that calculates hospital-wide operational metrics using optimized JPA queries and SQL projections. Provides cached, real-time KPI aggregates.
- **`KpiAnalyticsController`**: REST endpoint layer exposing the consolidated hospital executive summary for internal administration dashboards and BI tools.
- **`AuditLogger` & MDC Context Filter**: Central logging component emitting standardized, structured `[AUDIT]` log statements with user principal, action name, target identifier, and machine-parseable key-value details.
- **Relational Schema Audit Infrastructure**: Base entity `AuditableEntity` providing automatic JPA auditing (`@CreatedDate`, `@CreatedBy`, `@LastModifiedDate`, `@LastModifiedBy`) across all domain models, supplemented by explicit domain lifecycle timestamps.

### 2. Standardized Audit Log Schema (Path B)

All transactional operations emit structured log entries formatted as:

```
[AUDIT] user="{user}" action="{action}" target="{target}" details="{details}"
```

#### Standard Audit Action Vocabulary & Details Specification

| Domain Action | Audit Action Tag | Mandatory `details` Key-Value Attributes |
| :--- | :--- | :--- |
| ED Assessment Submitted | `SUBMIT_ED_ASSESSMENT` | `PrimaryAcuity`, `WardClass`, `Cluster`, `RecommendedAccepted={true\|false}`, `ElapsedMins` |
| Specialist Case Claimed | `CLAIM_BROADCAST` | `TargetCluster`, `Specialist`, `ElapsedClaimMins` |
| Broadcast Auto-Escalated | `AUTO_ESCALATE_BROADCAST` | `TargetCluster`, `EscalatedTo`, `SlaExceeded=true` |
| Specialist Consult Completed | `SUBMIT_SPECIALIST_CONSULT` | `PrimaryAcuity`, `SecondaryAcuity`, `Concordant={true\|false}`, `DiversionEndorsed={true\|false}` |
| Bed Allocation Approved | `ALLOCATE_BED` | `AssignedBed`, `Ward`, `Rank`, `Score`, `Override=false` |
| Bed Allocation Overridden | `OVERRIDE_ALLOCATION` | `AssignedBed`, `Ward`, `SelectedRank`, `OverrideReason`, `Override=true` |
| Batch Holding Ward Approved | `APPROVE_BATCH_HOLDING_WARD` | `BatchSize`, `PatientIds`, `WardClass`, `Gender` |
| Cohort Swap Approved | `APPROVE_COHORT_SWAP` | `ReassignedPatient`, `FromBed`, `ToBed`, `UnlockedWard` |
| Diversion Dispatched | `DIVERSION_REFERRAL` | `Facility`, `ReferralId`, `SlaWindowMins=30` |
| Operational Delay Tagged | `TAG_DELAY_REASON` | `DelayCode`, `DwellMins`, `AcuityTier` |
| Patient Tracker Accessed | `TRACK_PATIENT_ACCESS` | `PatientId`, `MilestoneStep`, `EstWaitMins` |
| Periodic Update Sent | `DISPATCH_PERIODIC_UPDATE` | `Channel=SMS_PUSH`, `Milestone`, `DeliveryStatus=SUCCESS` |
| MSW / Counseling Contacted | `CONNECT_MSW_HOTLINE` | `Service=MSW`, `AdmissionId`, `Action=CLICK_TO_CALL` |
| Morning Discharge Signed Off | `DISCHARGE_SIGNOFF` | `DoctorId`, `SignOffTime`, `PreDischargeHour` |
| Discharge Meds Dispensed | `DISPENSE_MEDICATION` | `PatientId`, `WardBed`, `TargetSla="11:00 AM"` |
| Bedside Meds Delivered | `DELIVER_BEDSIDE_MEDICATION` | `PatientId`, `BedId`, `ConfirmedBy`, `DeliveryTime` |
| Patient Vacated (Discharge) | `VACATE_PATIENT` | `BedNumber`, `VacateTimestamp`, `VacateHour`, `DischargedBeforeNoon={true\|false}` |
| Terminal Clean Signed Off | `CLEAN_BED` | `BedNumber`, `HousekeeperId`, `ElapsedCleaningMins`, `Within30mSla={true\|false}` |

### 3. API Surface & REST Contracts

- `GET /api/v1/analytics/kpis/summary`: Returns the consolidated executive summary of all 21 operational KPIs.
  - **Query Parameters**: Optional `startDate` and `endDate` (ISO-8601). If omitted, defaults to `ALL_TIME` to include all seeded prototype records and live session events.
  - **Division-by-Zero Safety**: All rate and percentage calculations are guarded; unobserved cohorts return `0.0` alongside their respective count fields to guarantee valid numerical JSON payloads.

```typescript
// Core Analytics Data Contracts (Prototype-Verified Shape)
interface HospitalKpiSummaryDto {
  periodStart: string;
  periodEnd: string;
  
  // Clinical Intake & Collaboration (Epic 1)
  avgEdTurnaroundMinutes: number;
  edTurnaroundP95Minutes: number;
  specialistClaimLatencyAvgMinutes: number;
  primarySpecialistConcordanceRatePct: number;
  digitalBedRequestCount: number;
  
  // BMU Capacity & Diversions (Epic 2)
  bmuSuggestionAcceptanceRatePct: number;
  bmuManualOverrideCount: number;
  totalDiversionCount: number;
  diversionRatePct: number;
  sisterHospitalSlaCompliancePct: number;
  batchHoldingWardAdoptionRatePct: number;
  
  // Patient Experience (Epic 3)
  patientTrackerAccessRatePct: number;
  twoHourPeriodicUpdateDeliveryPct: number;
  prolongedWaitCommunicationRatePct: number;
  
  // Inpatient Discharge & Turnover (Epic 4)
  dischargeBeforeNoonRatePct: number;
  advanceRunwayEstablishmentRatePct: number;
  bedsideMedicationDeliveryAdoptionPct: number;
  housekeepingTurnoverAvgMinutes: number;
  housekeeping30mSlaCompliancePct: number;
}
```

### 4. Frontend Analytics Dashboard Requirements (`/analytics`)

The web frontend provides an executive **Hospital Operational KPI Dashboard** accessible at route `/analytics`:

1. **Top-Level Navigation & Layout**:
   - Integrated as a top-level route in `router.tsx` and linked via an "Analytics" tab in `Header.tsx`.
   - Responsive layout with maximum width container and clean visual hierarchy matching the rest of the application.
2. **Organized 4-Domain Presentation**:
   - Displays all 21 metrics organized into 4 distinct domain sections corresponding to Epics 1 through 4:
     - *1. ED Clinical Intake & Specialist Collaboration*
     - *2. BMU Capacity Orchestration & Diversions*
     - *3. Patient & Family Milestone Tracking*
     - *4. Inpatient Discharge Runway & Rapid Turnover*
   - Each metric rendered in an executive stat card featuring:
     - Metric title and short descriptive subtitle
     - Formatted numerical value and unit (`mins`, `%`, `pax`)
     - Target benchmark indicator (e.g. `Target: ≥ 80%`, `SLA: ≤ 30 mins`)
     - Visual health badge: Green (Target Met), Amber (Approaching Threshold), or Red (Action Required)
3. **Real-Time Data Polling & Manual Synchronization**:
   - Powered by TanStack Query with an active 5-second background refetch interval (`refetchInterval: 5000`) so actions taken in BMU, Ward, or ED views reflect dynamically on the dashboard.
   - Includes a manual "Refresh Metrics" button with loading spinner state and a live "Last updated at HH:mm:ss" indicator.
4. **Hybrid Temporal Filtering Controls**:
   - **Preset Filter Buttons**: 1-click pill buttons (`All Time (Demo Mode)`, `Today (Last 24h)`, `Past 7 Days`), defaulting to `All Time (Demo Mode)`.
   - **Custom Date Inputs**: Optional `From` and `To` date pickers allowing users to define custom reporting windows.

### 5. Architectural Decisions (ADR Alignment)

- **ADR-001 (Relational Data Model & Auditing)**: Entities inherit `AuditableEntity`. Explicit lifecycle timestamps and flags are persisted on relational tables with database indexes for rapid filtering.
- **ADR-003 (Polling & Real-Time Synchronization)**: Consumes summary metrics via TanStack Query with 5-second background refetch.
- **ADR-009 (Enterprise Security, RBAC & IM8 Audit Logging)**: Structured SLF4J audit events provide the immutable, streamable foundation for real-time log ingestion and compliance.
- **ADR-011 (Dual-Pathway KPI Logging & Extraction Architecture)**: Establishes Path A (Relational SQL Analytics) and Path B (Structured Log Extraction) as the complementary operational metric pipelines.

---

## Testing Decisions

### What Makes a Good Test

Tests must verify the external behavioral accuracy of metric computations, schema integrity, and audit logging contracts:

- **Mathematical Calculation Accuracy**: Verify that percentage formulas, averages, and ratios compute correctly against known dataset counts (e.g., 8 accepted suggestions out of 10 allocations yields exactly `80.0%`).
- **Zero-Division & Edge-Case Safety**: Ensure that when a metric has zero occurrences (e.g., zero specialist consults or zero discharges), the API returns `0.0` or `null` gracefully without throwing arithmetic `DivisionByZero` exceptions.
- **Relational Timestamp Calculations**: Verify that `requested_at - created_at` and `last_cleaned_at - cleaning_started_at` accurately compute elapsed durations in minutes.
- **Audit Event Schema Verification**: Verify that invoking clinical and operational endpoints results in properly formatted `[AUDIT]` log statements containing all required key-value tokens in `details`.
- **API Contract Verification**: Ensure that `GET /api/v1/analytics/kpis/summary` returns HTTP 200 OK with fully populated JSON payloads meeting the TypeScript contract.

### Tested Modules & Seams

1. **Primary End-to-End API Seam (`KpiAnalyticsController` via `MockMvc`)**:
   - Tests HTTP GET endpoints for KPI summaries, checking JSON output structure, numerical formatting, and response codes.
2. **Metrics Calculation Service Seam (`KpiMetricsService`)**:
   - Directly tests computation formulas against varied database states using H2 in-memory test data.
3. **Structured Audit Log Seam (`AuditLogger`)**:
   - Captures log appender output during controller execution to verify that action tags, principal identities, and details tokens conform to the specification.

### Prior Art

- `backend/src/test/java/com/hospital/admissions/domain/JpaAuditingIntegrationTest.java`: Demonstrates JPA entity auditing and timestamp persistence.
- `backend/src/test/java/com/hospital/admissions/service/BmuServiceTest.java`: Demonstrates business logic validation and queue sorting.
- `backend/src/test/java/com/hospital/admissions/TracerBulletsIntegrationTest.java`: Demonstrates end-to-end event triggering from admission submission through bed allocation, patient check-in, nurse vacate, and housekeeping clean sign-off.

---

## Out of Scope

1. **Third-Party BI Tool Frontends**: Building or embedding dedicated BI dashboards (e.g., Metabase, Grafana, Tableau); the platform exposes clean REST contracts and SQL views for BI tool consumption.
2. **Heavy Distributed Stream Processing Engines**: Deploying Apache Flink, Apache Spark, or Kafka Streams; calculations are handled synchronously via JPA projections and lightweight log extractors in the prototype.
3. **Clinical Diagnostic Accuracy Assessment**: Evaluating the medical validity of physician diagnoses or lab test sensitivities; KPIs focus strictly on operational turnaround, collaboration, capacity, and logistics.
4. **Financial Payment Gateway Auditing**: Reconciling bank settlements or credit card transactions (financial metrics focus strictly on estimated subsidy tiers and counseling contact rates).

---

## Further Notes

- **Zero Clinical Overhead**: All KPI logging occurs asynchronously or as lightweight side-effects of existing clinical workflows (1-click approvals, status changes, barcode scans), requiring zero additional data-entry burden from doctors, nurses, or coordinators.
- **Production Pipeline Extensibility**: In production environments, Path B audit log statements stream seamlessly via standard collectors (Fluentbit, Logstash) into AWS CloudWatch / OpenSearch / Splunk to power real-time visual executive dashboards.
