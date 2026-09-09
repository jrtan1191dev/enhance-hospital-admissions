# Epic 1: ED Clinical Admission & Multi-Doctor Assessment Broadcast

## Problem Statement

Emergency Department (ED) physicians work under intense operational pressure while managing acutely ill patients awaiting inpatient admission. The clinical admission process is currently hindered by fragmented diagnostic data across disparate electronic systems, requiring manual synthesis of vital signs, lab panels, and scan reports. 

Furthermore, seeking inpatient specialist input relies on sequential telephone calls, page requests, and verbal handoffs. When on-call specialists are unavailable, cases sit idle without clear ownership or tracking. In complex cases requiring multi-disciplinary input (e.g., polytrauma or comorbid cardiology/orthopaedic conditions), doctors have no mechanism to broadcast consults concurrently or chain secondary specialty reviews.

In scenarios where the ED attending physician and consulting inpatient specialists disagree on patient acuity tier or required monitoring, patients risk being under-triaged into general ward beds without continuous telemetry, or delayed indefinitely while clinicians debate placement over the phone. Meanwhile, communication of bed requests to the Bed Management Unit (BMU) relies on phone calls and manual entries, resulting in opaque queues, lost operational minutes, and premature allocations prior to completing specialist assessments.

## Solution

The system provides an automated, unified clinical intake and broadcast platform:

1. **Automated Diagnostic Baseline & Smart Intake Branching**: Aggregates laboratory panels, vital signs, and diagnostic findings into an objective admission baseline. The ED attending can approve pre-populated recommendations with 1-click or override them with interactive chips (captured as structured audit deltas). The physician selects either **Direct Admission** (dispatched immediately to the BMU queue) or **Consult-Gated Admission** (held pending specialist review).
2. **ED Queue Separation & Assessed Admissions Tracker**: The moment an admission assessment is submitted, the patient is removed from the active intake queue and transitioned to an "Assessed Admissions" tracker tab to monitor ongoing consult progression, review specialist impressions, and track bed allocation.
3. **Asynchronous Multi-Doctor Assessment Broadcast Pool & Consult Chaining**: Concurrently broadcasts consult requests to active on-call specialty feeds organized by service clusters (Cardiology, General Medicine, Surgery, Orthopaedics). Reviewing specialists can claim cases atomically, review clinical parameters in parallel, spawn chained secondary consult broadcasts to other specialty clusters, and append specialist consult notes, recommended secondary acuity tiers, continuous telemetry directives, and structured diversion pathways (Community Hospital or MIC@Home).
4. **Cluster-Scoped SLA Timeout & Automated Escalation**: Monitors unclaimed broadcast cases against acuity-driven SLA thresholds (15 minutes for Tiers 1–2; 30 minutes for Tiers 3–5), auto-assigning overdue cases strictly to the designated default on-call specialist of that specific targeted cluster with urgent notification triggers.
5. **Consensus Completion Gate**: Ensures that admission requests requiring specialist review remain in `ASSESSMENT_PENDING` until **all** open and chained broadcasts reach `COMPLETED`. Only when all consults are closed does the case advance to `BED_REQUESTED` in the BMU queue.
6. **Safety-First Clinical Discordance Engine**: Detects conflicting acuity assessments or telemetry requirements between the primary ED attending and consulting specialists. The system automatically elevates the effective allocation priority to the highest acuity tier (`effectiveAcuityTier`), enforces continuous telemetry (`effectiveTelemetry`), highlights cases with high-visibility discordance badges, and provides side-by-side comparative views.
7. **BMU Admitting Discipline Placement & Tentative Bed Allocation**: BMU coordinators review completed clinical consults and hospital ward availability to assign the final `AdmittingSpecialtyCluster` before executing bed allocation. Assigned beds remain **Tentative Bed Allocations** (reservations) until physical patient arrival, permitting dynamic reallocation for capacity optimization or upon clinical reconciliation.
8. **In-Place Consult Amendments Post-Submission**: Permits consulting specialists to amend notes, acuity, or telemetry in-place if a patient's condition evolves while in the BMU queue, updating safety constraints without resetting queue dwell time or kicking the patient from the queue.

---

## User Stories

### Feature 1.1: Automated Diagnostic Synthesis, Smart Intake & Board Separation

1. As an ED Attending Physician, I want the system to ingest completed laboratory panels (such as Troponin, WBC, Hemoglobin) and real-time vital signs from the patient's EHR baseline, so that I have an objective clinical profile without manually querying multiple hospital subsystems.
2. As an ED Attending Physician, I want the system to analyze ingested diagnostic markers and suggest an initial acuity tier recommendation (Tier 1 Critical through Tier 5 Observation), so that my cognitive triaging load is minimized.
3. As an ED Attending Physician, I want the system to automatically flag required supportive equipment (such as continuous cardiac telemetry or fall-risk alarms) based on diagnostic criteria, so that clinical safety requirements are captured from the first point of contact.
4. As an ED Attending Physician, I want the intake summary to display prominent visual warning badges when diagnostic scans (such as CT Brain or Ultrasound) are still pending, so that I do not prematurely finalize assessments when critical results are outstanding.
5. As an ED Attending Physician, I want the intake interface to display pre-populated patient ward class preferences (Class A, B1, B2, or C) from patient records, so that financial and administrative preferences are automatically factored into the admission dossier.
6. As an ED Attending Physician, I want a smart admission form pre-populated with synthesized clinical findings that I can confirm with a single click, so that I can dispatch acute admission requests in seconds.
7. As an ED Attending Physician, I want interactive dropdown chips to override or fine-tune recommended acuity tiers, admitting specialty clusters, ward class preferences, and isolation constraints before submission, so that clinical judgment always remains the final authority.
8. As a Clinical Auditor, I want all clinician modifications to pre-populated values to be recorded as structured field-level deltas with clinician identity, previous value, modified value, and timestamp in an audit log, so that deviations from automated recommendations remain transparent and accountable.
9. As an ED Attending Physician, I want to choose between Direct Admission (no specialist consult needed) and Consult-Gated Admission (specialist consult required), so that straightforward admissions are not delayed by unnecessary consult pools.
10. As an ED Attending Physician, the moment I decide on admission and submit an assessment, I want the patient removed from the active "Awaiting Assessment" list and moved to a dedicated "Assessed Admissions" tracker tab, so that my active intake board remains clean and uncluttered.
11. As an ED Attending Physician, I want to track all my submitted admissions in the "Assessed Admissions" tab—viewing real-time consult statuses, specialist impressions, discordance alerts, and bed allocation progression—so that I have full end-to-end visibility of my patients awaiting ward transfer.

### Feature 1.2: Multi-Doctor Assessment Broadcast, Specialist Consult Pool & Chaining

12. As an ED Attending Physician, I want to select one or more target specialty clusters (Cardiology, General Medicine, Surgery, Orthopaedics) when requesting specialist consults, so that concurrent broadcasts are dispatched to all relevant departments simultaneously.
13. As an On-Call Specialist, I want to filter the broadcast pool by my service cluster, so that I can focus strictly on inpatient consults relevant to my department.
14. As an On-Call Specialist, I want to view active case counts and urgency badges across all service cluster feeds, so that I have immediate situational awareness of hospital-wide consult demand.
15. As an On-Call Specialist, I want to claim an open case with a single click, so that the broadcast status transitions to claimed and other specialists see that I have taken ownership of the review.
16. As an On-Call Specialist, I want the system to prevent race conditions when two doctors attempt to claim the same case concurrently using targeted optimistic locking, so that case ownership is atomically locked to the first responder while failing fast with HTTP 409 Conflict for the second doctor.
17. As an ED Floor Administrator, I want an active SLA countdown timer attached to each open broadcast based on clinical acuity (15 minutes for Tier 1–2; 30 minutes for Tier 3–5), so that clinicians can track how long cases have remained pending specialist review.
18. As an ED Floor Administrator, I want the system to automatically escalate an unclaimed case to the designated default on-call specialist of that specific targeted cluster when the cluster SLA timer expires, so that consult reviews are never neglected during shift turnovers.
19. As a Default On-Call Specialist, I want to receive an urgent high-priority notification when a case is auto-escalated to me due to SLA expiration, so that I can immediately triage the delayed patient.
20. As an On-Call Specialist, I want to spawn chained secondary broadcast requests to additional specialty clusters during my review, so that multi-disciplinary clinical evaluation can be coordinated seamlessly before admission finalization.
21. As an On-Call Specialist, I want to asynchronously submit my consult evaluation (including consult notes, recommended secondary acuity tier, recommended telemetry, and structured diversion pathway) directly into the active admission dossier.
22. As an On-Call Specialist, I want to endorse structured alternative diversion pathways (Community Hospital transfer or Mobile Inpatient Care at Home) during my consult submission, recommending Tier 4 Subacute Diversion for eligible patients.
23. As an On-Call Specialist, I want to edit or append updates to my previously submitted consult impressions in-place while the patient is awaiting bed allocation, so that evolving clinical conditions immediately update effective acuity and telemetry without resetting queue position.
24. As a Hospital Operations Lead, I want the system to enforce a Consensus Completion Gate, holding the admission request in `ASSESSMENT_PENDING` until all primary and chained broadcasts reach `COMPLETED` before dispatching the bed request to the BMU queue.

### Feature 1.3: Safety-First Clinical Discordance & Acuity Escalation

25. As a Patient Safety Officer, I want the system to compare the primary acuity tier submitted by the ED attending with the secondary acuity tiers submitted by all consulting specialists, so that divergent clinical assessments are identified algorithmically.
26. As a BMU Coordinator, I want the system to automatically calculate and persist `effectiveAcuityTier` as the highest acuity (lowest numerical tier) across the primary ED attending and all consulted specialists whenever discordance occurs, so that patients are never placed into lower-acuity beds due to conflicting opinions.
27. As a BMU Coordinator, I want any discordant request where either the ED attending OR any consulting specialist flags a telemetry requirement to automatically enforce continuous telemetry (`effectiveTelemetry = true`) on candidate beds, so that patient cardiac monitoring is never compromised.
28. As a BMU Coordinator, I want discordant admission requests in the BMU queue to be highlighted with high-visibility discordance warning badges, so that bed planners immediately recognize cases requiring placement scrutiny.
29. As a BMU Coordinator, I want to view a side-by-side comparison of the ED attending's notes and all consulting specialists' notes, so that I understand the clinical rationale behind divergent recommendations.
30. As a BMU Coordinator, I want a 1-click action to prompt the ED attending and specialists to reconcile their evaluations asynchronously, so that clinical alignment can be established without delaying initial bed reservations.
31. As a Clinical Quality Manager, I want all instances of clinical discordance and automated tier escalations to be logged with clinician identities, timestamps, and rationales, so that inter-departmental triage discrepancies can be reviewed for quality improvement.

### Feature 1.4: Direct Digital Bed Request Dispatch, 5-Tier Priority & BMU Placement

32. As an ED Attending Physician, I want Direct Admissions to publish immediately into the active BMU queue as digital bed requests (`status = BED_REQUESTED`), eliminating manual telephone calls between ED clinicians and bed managers.
33. As a BMU Coordinator, I want incoming bed requests to be indexed by clinical urgency tiers using a standardized 5-tier framework (`TIER_1_CRITICAL` through `TIER_5_OBSERVATION`), prioritized strictly by `effective_acuity_tier` and `requested_at`.
34. As a BMU Coordinator, I want incoming digital bed requests to contain complete structured patient attributes (gender, ward class preference, infection status, fall risk score, effective telemetry requirements), so that bed allocation rules have 100% complete data without follow-up inquiries.
35. As a BMU Coordinator, I want the authority to review all completed specialist consult notes and assign the final `AdmittingSpecialtyCluster` before triggering the bed allocation solver, ensuring operational bed capacity matches clinical needs.
36. As a BMU Coordinator, I want assigned beds to remain designated as Tentative Bed Allocations until the patient physically arrives and occupies the bed, so that beds can be dynamically reallocated for better hospital-wide capacity optimization or in response to reconciled clinical updates.
37. As an ED Attending Physician, I want immediate visual confirmation and queue status feedback once my assessment is dispatched, so that I know the bed request has been safely received by the BMU.

---

## Implementation Decisions

### 1. Modules & Domain Boundaries
- **Clinician Intake & Assessment Module**: Ingests diagnostic baselines, handles structured override capture, splits workflow between Direct Admission (`BED_REQUESTED`) and Consult-Gated Admission (`ASSESSMENT_PENDING`), and maintains the "Assessed Admissions" tracker tab.
- **Specialist Broadcast Pool Service**: Manages asynchronous publication of multi-cluster `AssessmentBroadcast` entities, enforces targeted optimistic locking (`@Version`) during atomic case claiming, supports specialist consult chaining, executes acuity-driven cluster-scoped auto-escalations, and processes in-place consult amendments.
- **Discordance & Priority Engine**: Evaluates primary vs. secondary acuity tiers across all completed broadcasts, calculates and persists `effectiveAcuityTier` and `effectiveTelemetry`, flags `isDiscordant`, and triggers non-blocking reconciliation alerts.
- **BMU Queue & Placement Controller**: Enforces the Consensus Completion Gate, indexes requests by `effectiveAcuityTier`, allows BMU assignment of `admittingSpecialtyCluster`, manages Tentative Bed Allocations, and handles diversion referrals.
- **Audit & Security Logging Layer**: Emits structured MDC-enriched audit events (`SUBMIT_ED_ASSESSMENT`, `OVERRIDE_CLINICAL_BASELINE`, `CLAIM_BROADCAST`, `AUTO_ESCALATE_BROADCAST`, `SUBMIT_SPECIALIST_CONSULT`, `AMEND_SPECIALIST_CONSULT`, `REQUEST_CLINICAL_RECONCILIATION`, `ASSIGN_ADMITTING_CLUSTER`).

### 2. Domain Glossary & Enumerations
- **`AcuityTier`**: `TIER_1_CRITICAL`, `TIER_2_ACUTE_URGENT`, `TIER_3_ACUTE_STABLE`, `TIER_4_SUBACUTE_DIVERSION`, `TIER_5_OBSERVATION`.
- **`SpecialtyCluster`**: `CARDIOLOGY`, `GENERAL_MEDICINE`, `SURGERY`, `ORTHOPAEDICS`.
- **`WardClass`**: `A`, `B1`, `B2`, `C`.
- **`BroadcastStatus`**: `OPEN`, `CLAIMED`, `AUTO_ESCALATED`, `COMPLETED`.
- **`AdmissionStatus`**: `ASSESSMENT_PENDING`, `BED_REQUESTED`, `BED_ALLOCATED`, `ADMITTED_INPATIENT`, `DISCHARGED`.
- **`DiversionPathway`**: `NONE`, `COMMUNITY_HOSPITAL`, `HOSPITAL_AT_HOME_MIC`.

### 3. API Surface & REST Contracts

- `GET /api/v1/clinicians/ed/patients`: Returns available ED patients awaiting initial admission assessment (excludes patients with active admissions).
- `GET /api/v1/clinicians/ed/admissions`: Returns submitted admissions for the ED attending's "Assessed Admissions" tracker tab.
- `POST /api/v1/clinicians/ed/assessments/submit`: Accepts `EdAssessmentSubmitRequest`. If `requiresSpecialistConsult = false`, sets status to `BED_REQUESTED`. If `true`, sets status to `ASSESSMENT_PENDING` and publishes concurrent `AssessmentBroadcast` records to all `targetClusters`.
- `GET /api/v1/clinicians/specialist/broadcasts`: Retrieves active consult broadcasts, optionally filtered by `cluster`.
- `POST /api/v1/clinicians/specialist/broadcasts/{id}/claim`: Atomically transitions broadcast status to `CLAIMED` via targeted optimistic locking (`@Version`), returning HTTP 409 Conflict if already claimed.
- `POST /api/v1/clinicians/specialist/broadcasts/{id}/chain`: Accepts `ChainConsultRequest` allowing a specialist to spawn an additional broadcast to another specialty cluster.
- `POST /api/v1/clinicians/specialist/broadcasts/{id}/consult`: Accepts `SpecialistConsultRequest`, transitions broadcast to `COMPLETED`, evaluates Consensus Completion Gate, and if all broadcasts are completed, transitions admission to `BED_REQUESTED`, sets `effectiveAcuityTier` and `effectiveTelemetry`, and evaluates discordance.
- `PUT /api/v1/clinicians/specialist/broadcasts/{id}/consult`: In-place amendment of consult notes, secondary acuity, and telemetry. Recalculates effective constraints without resetting queue dwell time.
- `POST /api/v1/bmu/requests/{id}/reconcile`: BMU 1-click action setting `reconciliationRequested = true` and alerting clinicians without blocking bed reservation.
- `POST /api/v1/bmu/requests/{id}/admitting-cluster`: BMU coordinator selects the final `AdmittingSpecialtyCluster` before bed allocation.

```typescript
// Core Data Contracts
interface ClinicalBaselineOverride {
  field: string;
  originalValue: string;
  submittedValue: string;
  overrideReason?: string;
}

interface EdAssessmentSubmitRequest {
  patientId: string;
  primaryAcuityTier: AcuityTier;
  requestedWardClass: WardClass;
  primaryTelemetry: boolean;
  requiresSpecialistConsult: boolean;
  targetClusters?: SpecialtyCluster[];
  overrides?: ClinicalBaselineOverride[];
  clinicalNotes?: string;
}

interface ChainConsultRequest {
  targetCluster: SpecialtyCluster;
  rationale: string;
}

interface SpecialistConsultRequest {
  secondaryAcuityTier: AcuityTier;
  secondaryTelemetry: boolean;
  consultNotes?: string;
  diversionPathway: DiversionPathway;
}

interface AssessmentBroadcast {
  id: string;
  admissionRequestId: string;
  targetCluster: SpecialtyCluster;
  status: BroadcastStatus;
  version: number;
  claimedBySpecialistId?: string;
  claimedAt?: string;
  consultNotes?: string;
  secondaryAcuityTier?: AcuityTier;
  secondaryTelemetry?: boolean;
  diversionPathway?: DiversionPathway;
  parentBroadcastId?: string;
}

interface AdmissionRequest {
  id: string;
  patientId: string;
  status: AdmissionStatus;
  primaryAcuityTier: AcuityTier;
  secondaryAcuityTier?: AcuityTier;
  effectiveAcuityTier: AcuityTier;
  primaryTelemetry: boolean;
  secondaryTelemetry?: boolean;
  effectiveTelemetry: boolean;
  admittingSpecialtyCluster?: SpecialtyCluster;
  requestedWardClass: WardClass;
  isDiscordant: boolean;
  reconciliationRequested: boolean;
  isRecommendationAccepted: boolean;
  requestedAt: string;
  allocatedAt?: string;
}
```

### 4. Architectural Decisions (ADR Alignment)
- **ADR-001 (Relational Persistence & Physical Hierarchy)**: Physical hospital layout strictly follows Level -> Ward -> Bed (no intermediate cubicles).
- **ADR-003 (Real-Time Communication via Polling & Cache Invalidation)**: Uses TanStack Query with targeted invalidation and 3-second background polling.
- **ADR-004 (Strict REST & Validation Standards)**: Jakarta Bean Validation returning RFC 7807 Problem Details.
- **ADR-006 (Zero-Auth Persona Switching for Prototype Evaluation)**: Role switcher mapping to seeded clinician identities with full SLF4J audit trail.
- **[ADR-007 (Consult-Gated Admission and BMU Queue Entry)](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/docs/adr/0007-consult-gated-bed-allocation.md)**: Gating bed allocation strictly upon assessment completion (`Direct Admission` vs `Consult-Gated Admission`).
- **[ADR-008 (Multi-Broadcast Specialist Consult Chaining and Consensus Gate)](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/docs/adr/0008-multi-broadcast-consult-chaining.md)**: Supporting multi-cluster broadcasts, specialist consult chaining, cluster-scoped auto-escalation, and consensus completion gating.
- **[ADR-009 (Separation of Clinical Consults and BMU Admitting Service Placement)](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/docs/adr/0009-bmu-admitting-service-placement.md)**: Clinicians focus strictly on clinical evaluations; BMU coordinators assign final admitting specialty clusters.

---

## KPI Instrumentation & Extraction Recommendations

All Epic 1 KPIs are instrumented via relational database fields and structured SLF4J audit events emitted through `AuditLogger`.

### 1. Structured Audit Log Events

Every clinical action emits an `[AUDIT]` log record with MDC context (`auditUser`, `auditAction`, `auditTarget`):
- `SUBMIT_ED_ASSESSMENT`:
  - `target`: `AdmissionRequest:{id}`
  - `details`: `PrimaryAcuity={tier}, WardClass={class}, RequiresConsult={true|false}, TargetClusters=[{clusters}], ElapsedMins={mins}`
- `OVERRIDE_CLINICAL_BASELINE`:
  - `target`: `AdmissionRequest:{id}`
  - `details`: `Field={field}, Original={orig}, Submitted={sub}, Reason={reason}`
- `CLAIM_BROADCAST`:
  - `target`: `AssessmentBroadcast:{id}`
  - `details`: `TargetCluster={cluster}, Specialist={doctorName}, ElapsedClaimMins={mins}`
- `AUTO_ESCALATE_BROADCAST`:
  - `target`: `AssessmentBroadcast:{id}`
  - `details`: `TargetCluster={cluster}, EscalatedTo={defaultDoctorName}, SlaExceeded=true`
- `SUBMIT_SPECIALIST_CONSULT`:
  - `target`: `AssessmentBroadcast:{id}`
  - `details`: `PrimaryAcuity={primaryTier}, SecondaryAcuity={secondaryTier}, Concordant={true|false}, Diversion={pathway}`
- `AMEND_SPECIALIST_CONSULT`:
  - `target`: `AssessmentBroadcast:{id}`
  - `details`: `RevisedAcuity={tier}, RevisedTelemetry={telemetry}, NotesAppended=true`
- `REQUEST_CLINICAL_RECONCILIATION`:
  - `target`: `AdmissionRequest:{id}`
  - `details`: `InitiatedBy={bmuCoordinator}, DiscordanceType=ACUITY_OR_TELEMETRY`
- `ASSIGN_ADMITTING_CLUSTER`:
  - `target`: `AdmissionRequest:{id}`
  - `details`: `AssignedCluster={cluster}, AssignedBy={bmuCoordinator}`

### 2. Database Schema Audit Fields

- `admission_requests`: `id`, `created_at`, `requested_at`, `primary_acuity_tier`, `secondary_acuity_tier`, `effective_acuity_tier`, `primary_telemetry`, `secondary_telemetry`, `effective_telemetry`, `is_discordant`, `reconciliation_requested`, `is_recommendation_accepted`, `admitting_specialty_cluster`
- `assessment_broadcasts`: `id`, `admission_request_id`, `target_cluster`, `status`, `version`, `claimed_by_specialist_id`, `claimed_at`, `created_at`, `last_modified_at`, `parent_broadcast_id`, `diversion_pathway`

### 3. Metric Computation Recipes

#### KPI 1: Primary ED Assessment Turnaround Time
- **SQL Extraction**:
  ```sql
  SELECT 
    AVG(EXTRACT(EPOCH FROM (requested_at - created_at)) / 60.0) AS avg_turnaround_mins,
    PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (requested_at - created_at)) / 60.0) AS p50_mins,
    PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (requested_at - created_at)) / 60.0) AS p95_mins
  FROM admission_requests;
  ```

#### KPI 2: Specialist Broadcast Pick-Up & Response Latency
- **SQL Extraction**:
  ```sql
  -- Claim Pick-Up Latency
  SELECT 
    target_cluster,
    AVG(EXTRACT(EPOCH FROM (claimed_at - created_at)) / 60.0) AS avg_pickup_latency_mins
  FROM assessment_broadcasts 
  WHERE claimed_at IS NOT NULL
  GROUP BY target_cluster;

  -- Consult Submission Latency
  SELECT 
    target_cluster,
    AVG(EXTRACT(EPOCH FROM (last_modified_at - claimed_at)) / 60.0) AS avg_consult_latency_mins
  FROM assessment_broadcasts 
  WHERE status = 'COMPLETED'
  GROUP BY target_cluster;
  ```

#### KPI 3: Primary vs Specialist Concordance Rate
- **SQL Extraction**:
  ```sql
  SELECT 
    COUNT(*) AS total_consulted_cases,
    SUM(CASE WHEN is_discordant = false THEN 1 ELSE 0 END) AS concordant_cases,
    ROUND(100.0 * SUM(CASE WHEN is_discordant = false THEN 1 ELSE 0 END) / COUNT(*), 2) AS concordance_rate_pct
  FROM admission_requests
  WHERE secondary_acuity_tier IS NOT NULL;
  ```

#### KPI 4: Accepted Admissions Based on Recommendations & Overrides
- **SQL Extraction**:
  ```sql
  SELECT 
    COUNT(*) AS total_admissions,
    SUM(CASE WHEN is_recommendation_accepted = true THEN 1 ELSE 0 END) AS accepted_recommendations,
    ROUND(100.0 * SUM(CASE WHEN is_recommendation_accepted = true THEN 1 ELSE 0 END) / COUNT(*), 2) AS acceptance_rate_pct
  FROM admission_requests;
  ```

#### KPI 5: Breakdown of Accepted Requests by Effective Severity Tier
- **SQL Extraction**:
  ```sql
  SELECT 
    effective_acuity_tier,
    COUNT(*) AS request_count,
    ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER(), 2) AS percentage_of_total
  FROM admission_requests
  WHERE status IN ('BED_REQUESTED', 'BED_ALLOCATED', 'ADMITTED_INPATIENT')
  GROUP BY effective_acuity_tier
  ORDER BY effective_acuity_tier;
  ```

---

## Testing Decisions

### What Makes a Good Test
Tests must verify externally observable behavior and domain invariants through HTTP and service interfaces:
- Validation errors return HTTP 400 Bad Request with Problem Details.
- Direct Admission submissions immediately transition patient status to `BED_REQUESTED` without creating broadcasts.
- Consult-Gated Admission submissions transition patient status to `ASSESSMENT_PENDING` and emit concurrent `OPEN` broadcasts.
- Simultaneous claim attempts on the same broadcast enforce optimistic locking: the first succeeds and the second returns HTTP 409 Conflict.
- SLA expiration automatically escalates overdue broadcasts to the targeted cluster's default specialist.
- Chained consult requests spawn secondary broadcasts linked to the parent admission.
- Consensus Completion Gate holds admission status in `ASSESSMENT_PENDING` until all primary and chained broadcasts are `COMPLETED`.
- Conflicting acuity or telemetry automatically sets `isDiscordant = true`, elevates `effectiveAcuityTier` to the highest acuity, and enforces `effectiveTelemetry = true`.
- BMU reconciliation requests flag `reconciliationRequested = true` without blocking tentative bed allocation.
- In-place consult amendments recalculate `effectiveAcuityTier` and telemetry without removing the patient from the BMU queue.

### Tested Modules & Seams
1. **Primary End-to-End API Seam (`ClinicianController` via `MockMvc`)**:
   - Tests complete HTTP cycle, validation, optimistic locking 409 responses, and security context mapping.
2. **Service & Domain Invariant Seam (`ClinicianService`)**:
   - Tests Consensus Completion Gate, multi-broadcast lifecycle, consult chaining, discordance calculation, and SLA auto-escalation.
3. **Audit Trail Seam (`AuditLogger`)**:
   - Verifies that structured audit logs are emitted for `OVERRIDE_CLINICAL_BASELINE`, `AUTO_ESCALATE_BROADCAST`, `AMEND_SPECIALIST_CONSULT`, and `REQUEST_CLINICAL_RECONCILIATION`.

---

## Out of Scope

1. **Automated Bed Constraint Solving & 4-State Bed Machine**: Ward bed availability, constraint satisfaction, and 1-click bed allocations belong to Epic 2 (BMU Bed Capacity).
2. **Dynamic Holding Ward & Flex-Cubicle Batching**: Automatic conversion of wards into temporary holding spaces is handled by Epic 2.
3. **External Sister Hospital Referral Dispatch & SLA Countdown**: Executing transfer agreements with Outram Community Hospital or MIC@Home is handled by Epic 2.
4. **Public Patient Mobile Journey Tracker**: Token-based queue progression and family notifications are handled by Epic 3.
5. **Inpatient Discharge Runway & 30-Minute Housekeeping Turnover**: Discharge planning, bedside pharmacy dispensing, and room sanitization are handled by Epic 4.
6. **Live Telemedicine / Audio Calls**: Video/audio calling between clinicians; communication is strictly asynchronous through structured consult packets and notes.

---

## Further Notes

- **Profile Gating**: Synthetic ED patients and mock EHR diagnostic baselines are strictly active under `@Profile("prototype")`. In production mode, standard dependency injection wires production FHIR clients without altering core admission, broadcast, or discordance logic.
