# Epic 2: BMU Bed Capacity & Dynamic Flex-Cubicle Batching

## Problem Statement

Bed Management Unit (BMU) coordinators operate under extreme cognitive load while navigating complex, multi-variable clinical and operational constraints in real time. Matching acute patients arriving from the Emergency Department (ED) to inpatient hospital beds requires balancing rigid clinical safety constraints (ward class subsidy entitlement, strict gender cohorting, droplet/airborne infection control isolation, and continuous telemetry monitoring) against operational ward utilization goals.

Currently, bed planners rely on disparate spreadsheet trackers, whiteboards, and repetitive telephone calls with ward nurses and housekeeping staff. This fragmented approach leads to significant operational failure modes:
1. **Ghost Capacity and Single-Gender Cubicle Lockout**: Placing an individual patient into an unallocated multi-bed room inadvertently locks the entire room to that single gender and infection profile, stranding surrounding clean beds as unusable "ghost capacity" when surges of opposite cohorts arrive.
2. **Opaque Bed Turnover Latency**: Vacated beds often sit empty for hours without timely housekeeping dispatch, or are manually held in limbo because coordinators cannot ascertain whether a room has undergone terminal sanitization.
3. **Underutilized Diversion Pathways**: Clinically stable subacute patients eligible for diversion to Sister Community Hospitals (e.g., Outram Community Hospital, Alexandra Hospital, St. Andrew's Community Hospital) or virtual hospital care (MIC@Home) remain boarded in acute beds due to burdensome manual referral workflows and lack of operational authority.
4. **Information Voids during Prolonged Boarding**: Prolonged bed queue delays lack objective dwell-time SLA monitoring and structured delay attribution, leaving floor nurses unable to explain placement bottlenecks to anxious patients and family members.

## Solution

The system introduces an automated, constraint-driven bed capacity orchestration platform that provides:

1. **Two-Phase Heuristic Bed Allocation Engine**: Evaluates available beds against hard clinical filters (ward class, locked gender, infection status, telemetry capability, negative pressure) and optimizes soft operational scoring (+40 specialty cluster alignment, +30 consolidation packing bonus, +15 high fall-risk nursing station proximity). The system presents the Top 3 ranked beds with match rationale chips for 1-click coordinator approval, while enforcing mandatory structured override reason codes for governance and machine learning feedback.
2. **Strict Four-State Bed Lifecycle Machine**: Tracks all physical inpatient beds across four deterministic states: `EMPTY_PENDING_CLEANING` (`Mustard Yellow` - vacated, empty, pending clean), `EMPTY_CLEANED` (`White` - sanitized and available for allocation), `EMPTY_ASSIGNED` (`Green` - reserved/in-transit for an ED patient), and `OCCUPIED_TAKEN` (`Grey` - physically occupied by an admitted patient). Vacated beds enter `Mustard Yellow` upon discharge and can only transition to `White` after an Environmental Services (EVS) terminal cleaning sign-off satisfying the 30-minute turnover SLA.
3. **Dynamic Holding Ward Batching & Dynamic Cohort-Swap Re-Optimization**:
   - *Phase 1 Consolidation Packing*: Prioritizes placing incoming patients into partially filled, matching wards, deliberately preserving uncommitted all-`White` flex wards.
   - *Phase 2 Dynamic Batching*: Automatically detects waiting queue clusters of $\ge 3$ patients sharing ward class, gender, and infection profiles, proactively surfacing a 1-click "Batch Holding Ward Suggestion" card to convert an all-`White` flex ward into a dedicated holding cohort, pairing allocations with coordinated batch porter transfer.
   - *Dynamic Cohort-Swap Re-Optimization*: Detects when an uncommitted flex ward is blocked by 1–2 isolated `Green` assigned beds whose patients have not departed the ED, suggesting a 1-click swap into matching partially occupied wards to release the flex ward for an incoming high-volume surge cluster.
4. **Operational Diversion & Alternative Care Routing**: Empowers BMU coordinators with 1-click digital referral dispatch to Sister Community Hospitals (OCH, AH, SACH) and Mobile Inpatient Care at Home (MIC@Home), generating standardized clinical packets and initiating 30-minute bilateral SLA countdowns.
5. **Acuity Dwell-Time Tracking & Delay Reason Tagging**: Monitors patient boarding time against acuity-tier SLA thresholds, surfacing flashing visual escalation badges for prolonged waits and enabling coordinators to record structured operational delay tags that propagate directly to ED floor nurse consoles and patient trackers.

---

## User Stories

### Feature 2.1: Constraint-Satisfaction Bed Recommendation & 1-Click Approval

1. As a BMU Coordinator, I want the system to filter out candidate beds that do not match the patient's requested Ward Class (Class A, B1, B2, or C), so that patients are never assigned to wards exceeding their financial subsidy selection without explicit override.
2. As a BMU Coordinator, I want the system to enforce strict biological gender cohorting across multi-bed wards, so that male and female patients are never placed into the same room cohort.
3. As a BMU Coordinator, I want the system to enforce negative pressure isolation room constraints for patients with airborne or droplet infection flags, so that infectious patients are never placed into general open wards.
4. As a BMU Coordinator, I want the system to enforce continuous cardiac telemetry equipment availability on candidate beds when requested by admitting clinicians, so that patients requiring continuous monitoring are not assigned to unequipped beds.
5. As a BMU Coordinator, I want candidate beds that satisfy all hard constraints to receive a positive scoring bonus (+40 points) when the ward's service cluster matches the patient's admitting specialty, so that patients are co-located with their primary clinical care teams.
6. As a BMU Coordinator, I want candidate beds in partially occupied wards to receive a consolidation packing bonus (+30 points), so that existing room cohorts are filled first and empty wards are preserved for surge batching.
7. As a BMU Coordinator, I want high fall-risk patients (Morse fall score $\ge 45$) to receive a proximity bonus (+15 points) for beds located adjacent to the ward nursing station, so that vulnerable patients are placed in direct visual sight of nurses.
8. As a BMU Coordinator, I want the system to present the Top 3 ranked candidate beds with detailed score breakdown chips on my queue detail panel, so that I can immediately understand the clinical and operational rationale behind each recommendation.
9. As a BMU Coordinator, I want to approve the #1 ranked bed recommendation with a single click, so that routine bed assignments can be completed in under two seconds.
10. As a BMU Coordinator, I want to approve any alternative bed among the recommendations or manually select an eligible inventory bed, so that human operational judgment can accommodate unmodelled ward situations.
11. As a BMU Coordinator, I want the system to require a mandatory structured override reason whenever I select a bed other than the recommended candidates, so that institutional deviations are captured for audit and algorithm refinement.
12. As a Hospital Operations Director, I want all allocation approvals, overrides, and rationale codes to be recorded with user identity and timestamp in the audit log, so that bed allocation decisions remain transparent and traceable.

### Feature 2.2: Four-State Bed Lifecycle Machine (`Mustard Yellow` / `White` / `Green` / `Grey`)

1. As a BMU Coordinator, I want all physical hospital beds to strictly adhere to a four-state operational lifecycle (`Mustard Yellow`, `White`, `Green`, `Grey`), so that bed availability and turnover status are unambiguous across all departments.
2. As a BMU Coordinator, I want an available bed in `White` (`EMPTY_CLEANED`) to transition immediately to `Green` (`EMPTY_ASSIGNED`) upon 1-click allocation approval, so that the bed is locked and cannot be double-booked by another planner.
3. As a Ward Nurse, I want to confirm patient physical arrival on the ward console with a single click, so that the bed transitions from `Green` (`EMPTY_ASSIGNED`) to `Grey` (`OCCUPIED_TAKEN`).
4. As a Ward Nurse, I want to mark a patient as vacated upon inpatient discharge, so that the bed immediately transitions from `Grey` (`OCCUPIED_TAKEN`) to `Mustard Yellow` (`EMPTY_PENDING_CLEANING`).
5. As an Environmental Services (EVS) Housekeeper, I want vacated `Mustard Yellow` beds to automatically trigger an urgent turnover task on our housekeeping queue with an active 30-minute SLA countdown timer, so that cleaning crews can sanitize beds promptly.
6. As a BMU Coordinator, I want beds in `Mustard Yellow` (`EMPTY_PENDING_CLEANING`) to be excluded from standard recommendation candidate pools, so that uncleaned or contaminated beds are never allocated to waiting patients.
7. As an EVS Housekeeper, I want to sign off completed terminal sanitization and inspection on my mobile terminal, so that the bed immediately transitions from `Mustard Yellow` to `White` (`EMPTY_CLEANED`).
8. As a BMU Coordinator, I want newly cleaned `White` beds to instantly become available for algorithmic recommendation and queue matching without manual data re-entry, so that bed turnover latency is eliminated.
9. As a Hospital Facilities Manager, I want the system to measure and log the elapsed turnover time between the nurse vacate event and the housekeeper clean sign-off against the 30-minute SLA, so that housekeeping efficiency can be monitored.

### Feature 2.3: Dynamic Flex-Cubicle Batching & Holding Room Creation

1. As a BMU Coordinator, I want the allocation engine to prioritize packing incoming individual patients into partially filled wards matching their clinical profile (Phase 1 Consolidation Packing), so that completely empty flex wards are kept uncommitted.
2. As a BMU Coordinator, I want the system to continuously scan the waiting queue to identify surge clusters of $\ge 3$ patients sharing the same Ward Class, gender, and infection profile, so that high-volume patient cohorts are identified proactively.
3. As a BMU Coordinator, I want the system to surface a high-visibility "Batch Holding Ward Suggestion" card whenever a surge cluster matches an available all-`White` flex ward, so that I am alerted to bulk placement opportunities.
4. As a BMU Coordinator, I want to approve a Batch Holding Ward recommendation in a single click, so that all candidate beds in the flex ward transition simultaneously from `White` to `Green` and are assigned to the cluster patients.
5. As an ED Charge Nurse, I want approval of a Batch Holding Ward to generate a coordinated batch porter transfer dispatch, so that 3 to 6 patients can be transported together to the inpatient ward rather than sequentially.
6. As a BMU Coordinator, I want the engine to detect when an all-`White` flex ward is obstructed by 1 or 2 isolated `Green` assigned beds whose patients have not yet left the ED, so that locked capacity can be identified during surges.
7. As a BMU Coordinator, I want the engine to evaluate whether the isolated `Green` patients can be safely moved to matching beds in partially occupied wards without violating any hard constraints, so that a viable cohort swap can be identified algorithmically.
8. As a BMU Coordinator, I want the system to present a "Dynamic Cohort-Swap Proposal" comparing current vs. proposed assignments, so that I can review the operational benefit before approving.
9. As a BMU Coordinator, I want to execute the proposed cohort swap with a single click, so that isolated assignments are re-pointed to partially occupied wards and the flex ward is completely freed to batch-admit the waiting surge cluster.

### Feature 2.4: Alternative Care-Pathway & Diversion Operational Authority

1. As a BMU Coordinator, I want the queue interface to highlight patients who have been clinically endorsed for diversion (Tier 4 Subacute or Virtual Hospital) by consulting specialists, so that alternative care pathways can be prioritized.
2. As a BMU Coordinator, I want to view a pre-populated digital referral packet for eligible subacute patients targeting Sister Community Hospitals (Outram Community Hospital, Alexandra Hospital, St. Andrew's Community Hospital), so that external transfers do not require manual paperwork.
3. As a BMU Coordinator, I want to dispatch the digital referral to the selected Sister Hospital with a single click, so that the transfer packet is transmitted electronically to the receiving hospital's bed office.
4. As a BMU Coordinator, I want the system to initiate a 30-minute bilateral SLA countdown upon dispatch of a Sister Hospital referral, so that the referral outcome is tracked against institutional transfer response standards.
5. As a BMU Coordinator, I want to route clinically stable acute patients meeting home-care safety criteria directly to the Hospital-at-Home (MIC@Home) virtual ward, so that acute tertiary beds are preserved for higher-acuity emergencies.
6. As a BMU Coordinator, I want confirming a MIC@Home admission to allocate a virtual bed identifier (e.g., `MIC-V042`) and update the admission status to `DIVERTED_HAH`, so that home nursing logistics and tele-monitoring setup orders are triggered automatically.
7. As a Clinical Operations Director, I want diversion referral metrics (total referrals, acceptance rate, and average bilateral response time) to be recorded, so that community health system integration can be evaluated.

### Feature 2.5: Long-Wait Monitoring & Operational Delay Communication

1. As a BMU Coordinator, I want the prioritized admission queue to display active dwell timers measuring the exact minutes elapsed since each patient's admission request was submitted, so that waiting duration is continuously transparent.
2. As a BMU Coordinator, I want patients who exceed their clinical acuity-tier dwell SLA threshold (e.g., 60 minutes for Tier 2 Acute Urgent, 120 minutes for Tier 3 Acute Stable) to display prominent visual escalation badges, so that delayed patients are elevated to immediate attention.
3. As a BMU Coordinator, I want to attach structured operational delay reason tags (such as `SPECIALIZED_ISOLATION_TURNOVER`, `EMERGENCY_TRAUMA_SURGE`, `WARD_STAFFING_DELAY`, `BED_INSPECTION_PENDING`) to prolonged-wait cases, so that operational bottlenecks are formally documented.
4. As an ED Floor Nurse, I want structured delay reason tags recorded by BMU to appear immediately on my triage console alongside suggested family talking points, so that I can proactively inform waiting patients and resolve family anxiety without repeatedly calling BMU.
5. As a Patient Safety Officer, I want historical dwell times and delay reason tags to be logged in operational reporting databases, so that systemic root causes of hospital boarding delays can be analyzed and mitigated.

---

## Implementation Decisions

### 1. Modules & Domain Boundaries

- **BMU Orchestration & Capacity Service (`BmuService`)**: Manages prioritized admission queues, inventory aggregation (Level $\to$ Ward $\to$ Bed hierarchy), 1-click allocation approvals, manual overrides with structured rationale codes, and operational delay tagging.
- **Constraint Satisfaction & Heuristic Scoring Engine (`BedAllocationSolver` / `HeuristicBedAllocationSolver`)**: Implements two-phase mathematical optimization:
  - *Hard Constraint Disqualification*: Prunes candidate beds violating Ward Class, locked gender cohorts, infection control isolation, telemetry capability, or negative pressure requirements.
  - *Soft Scoring Optimization*: Applies configurable weights for specialty cluster alignment (`weightSpecialtyCluster`, default: 40), consolidation packing (`weightConsolidation`, default: 30), and nursing station proximity for high fall-risk patients (`weightFallRiskStation`, default: 15).
- **Dynamic Batching & Dynamic Cohort-Swap Engine**: Scans pending `BED_REQUESTED` queues for clusters of $\ge 3$ patients matching ward class, gender, and infection status; evaluates all-`White` flex wards for bulk batching; identifies isolated `Green` beds blocking flex wards and generates 1-click cohort swap plans.
- **Bed Lifecycle & Turnover Enforcement Service**: Manages state transitions across `EMPTY_PENDING_CLEANING` (`Mustard Yellow`), `EMPTY_CLEANED` (`White`), `EMPTY_ASSIGNED` (`Green`), and `OCCUPIED_TAKEN` (`Grey`), enforcing the 30-minute terminal cleaning SLA.
- **Sister Hospital & Virtual Ward Diversion Gateway (`SisterHospitalGateway`)**: Dispatches electronic referral packets to external community hospitals (OCH, AH, SACH) and MIC@Home virtual wards under 30-minute bilateral SLAs, profile-gated to mock implementations in prototype mode and HTTP/FHIR in production.

### 2. Domain Glossary & Enumerations

- **`BedStatus`**:
  - `EMPTY_PENDING_CLEANING`: Patient vacated, bed empty but uncleaned (`Mustard Yellow`).
  - `EMPTY_CLEANED`: Cleaned, sanitized, inspected, and unallocated (`White`).
  - `EMPTY_ASSIGNED`: Allocated to patient, patient in transit from ED (`Green`).
  - `OCCUPIED_TAKEN`: Patient physically admitted and occupying bed (`Grey`).
- **`WardClass`**: `A`, `B1`, `B2`, `C`.
- **`AcuityTier`**: `TIER_1_CRITICAL`, `TIER_2_ACUTE_URGENT`, `TIER_3_ACUTE_STABLE`, `TIER_4_SUBACUTE_DIVERSION`, `TIER_5_SHORT_STAY`.
- **`AdmissionStatus`**: `ASSESSMENT_PENDING`, `BED_REQUESTED`, `BED_ALLOCATED`, `ADMITTED_INPATIENT`, `DISCHARGED`, `DIVERTED_SISTER_HOSPITAL`, `DIVERTED_HAH`.
- **`InfectionStatus`**: `NON_INFECTIOUS`, `RESPIRATORY`, `MRSA`.
- **`Gender`**: `MALE`, `FEMALE`.
- **`OverrideReasonCode`**: `ATTENDING_CLINICAL_REQUEST`, `WARD_STAFFING_LIMITATION`, `IMPENDING_CUBICLE_MAINTENANCE`, `FAMILY_PROXIMITY_REQUEST`.
- **`DelayReasonCode`**: `HOUSEKEEPING_DELAY`, `BED_SHORTAGE`, `SPECIALIZED_ISOLATION_CLEANING`, `SURGE_TRAUMA_EVENT`.

### 3. API Surface & REST Contracts

- `GET /api/v1/bmu/queue`: Retrieves prioritized admission queue sorted by Acuity Tier (ordinal 0=Tier 1 highest) and ascending `requestedAt` (FIFO dwell time).
- `GET /api/v1/bmu/inventory`: Returns Level $\to$ Ward $\to$ Bed hierarchy with real-time bed states and cohort locks.
- `GET /api/v1/bmu/recommendations/{requestId}`: Computes constraint satisfaction and returns Top 3 ranked candidate beds with score breakdowns.
- `POST /api/v1/bmu/allocate`: Accepts `BedAllocationRequest` (`admissionRequestId`, `bedId`), validates bed is `EMPTY_CLEANED`, updates bed to `EMPTY_ASSIGNED`, and links patient.
- `POST /api/v1/bmu/allocations/override`: Accepts `AllocationOverrideRequest` (`admissionRequestId`, `bedId`, `reasonCode`), records override rationale, and allocates bed.
- `GET /api/v1/bmu/batch-suggestions`: Scans queue for clusters $\ge 3$ and returns available all-`White` flex wards for batch holding.
- `POST /api/v1/bmu/batch-holding-wards/approve`: Accepts `BatchApprovalRequest` (`suggestionId`), bulk allocates cluster patients, and dispatches batch porter request.
- `GET /api/v1/bmu/cohort-swap-suggestions`: Identifies isolated `Green` beds blocking flex wards and returns viable non-conflicting reassignment plans.
- `POST /api/v1/bmu/cohort-swap/approve`: Executes cohort swap atomically, freeing the flex ward.
- `POST /api/v1/bmu/diversion/refer`: Dispatches digital referral to Sister Hospital or MIC@Home.
- `POST /api/v1/bmu/beds/{bedId}/clean`: Housekeeping sign-off; transitions bed from `EMPTY_PENDING_CLEANING` (`Mustard Yellow`) to `EMPTY_CLEANED` (`White`).
- `POST /api/v1/bmu/requests/{requestId}/delay-tag`: Attaches structured delay reason tag and optional explanatory note to an admission request.
- `GET /api/v1/bmu/config`: Retrieves active algorithm scoring weights.
- `PUT /api/v1/bmu/config`: Updates algorithm weights (`weightSpecialtyCluster`, `weightConsolidation`, `weightFallRiskStation`, `batchHoldingWardThreshold`).

```typescript
// Core Data Contracts (Prototype-Verified Shape)
interface BedRecommendation {
  bedId: string;
  bedNumber: string;
  level: number;
  wardName: string;
  score: number;
  scoreBreakdown: string[];
  isRecommended: boolean;
}

interface BatchSuggestion {
  suggestionId: string;
  targetWardId: string;
  targetWardName: string;
  patientIds: string[];
  patientNames: string[];
  commonWardClass: WardClass;
  commonGender: Gender;
  commonInfectionStatus: InfectionStatus;
}

interface CohortSwapSuggestion {
  swapId: string;
  flexWardName: string;
  blockedPatientId: string;
  blockedPatientName: string;
  currentBedNumber: string;
  targetBedNumber: string;
  targetWardName: string;
  unlockedCapacityCount: number;
}

interface BmuConfigUpdateRequest {
  weightSpecialtyCluster: number;
  weightConsolidation: number;
  weightFallRiskStation: number;
  batchHoldingWardThreshold: number;
}
```

### 4. Architectural Decisions (ADR Alignment)

- **ADR-001 (Spatial Hierarchy & Cohorting)**: Enforces Level $\to$ Ward $\to$ Bed hierarchy with no intermediate cubicles. Cohort locks (`lockedGender`, `lockedInfectionStatus`) and flex capabilities (`isHoldingWard`) reside on `Ward`.
- **ADR-002 (Bed State Machine & Heuristics)**: Strict 4-state transitions with two-phase pack-then-batch optimization. Consolidates patients into partially filled wards before allocating flex buffers.
- **ADR-003 (Polling & Cache Invalidation)**: React Query polling at 3000 ms intervals on `/bmu/queue` and `/bmu/inventory`, with targeted cache invalidation upon mutations.
- **ADR-004 (Strict Validation & RFC 7807)**: All POST/PUT payloads validated via Jakarta Validation, emitting RFC 7807 `ProblemDetail` on errors (e.g., allocating a bed that is not `EMPTY_CLEANED` yields HTTP 409 Conflict / 400 Bad Request).
- **ADR-005 & ADR-006 (Persona Switching & Audit Trails)**: `PrototypeSecurityFilter` maps `bmu_coord_wong` identity with SLF4J/MDC audit logging for every allocation, override, and config mutation.

---

## KPI Instrumentation & Extraction Recommendations

All Epic 2 operational capacity and allocation KPIs are instrumented via relational entity audit columns and structured SLF4J audit events emitted through `AuditLogger`.

### 1. Structured Audit Log Events

- `ALLOCATE_BED`:
  - `target`: `AdmissionRequest:{id}`
  - `details`: `AssignedBed={bedNumber}, Ward={wardName}, Rank={rank}, Score={score}, Override=false`
- `OVERRIDE_ALLOCATION`:
  - `target`: `AdmissionRequest:{id}`
  - `details`: `AssignedBed={bedNumber}, Ward={wardName}, SelectedRank={rank}, OverrideReason={reasonCode}, Override=true`
- `APPROVE_BATCH_HOLDING_WARD`:
  - `target`: `Ward:{wardId}`
  - `details`: `BatchSize={count}, PatientIds=[{id1},{id2},...], WardClass={class}, Gender={gender}`
- `APPROVE_COHORT_SWAP`:
  - `target`: `CohortSwap:{swapId}`
  - `details`: `ReassignedPatient={patientId}, FromBed={bed1}, ToBed={bed2}, UnlockedWard={wardId}`
- `DIVERSION_REFERRAL`:
  - `target`: `AdmissionRequest:{id}`
  - `details`: `Facility={facility}, ReferralId={referralId}, SlaWindowMins=30`
- `TAG_DELAY_REASON`:
  - `target`: `AdmissionRequest:{id}`
  - `details`: `DelayCode={code}, DwellMins={mins}, AcuityTier={tier}`

### 2. Database Schema Audit Fields

- `admission_requests`: `allocated_at`, `requested_at`, `assigned_bed_id`, `is_recommendation_accepted`, `override_reason_code`, `diversion_recommended`, `sister_hospital_referral_id`, `delay_reason_tag`
- `beds`: `status`, `last_cleaned_at`, `cleaning_started_at`, `ward_id`
- `wards`: `is_holding_ward`, `locked_gender`, `locked_infection_status`

### 3. Metric Computation Recipes

#### KPI 7: BMU Suggestion Acceptance Rate
- **SQL Extraction**:
  ```sql
  SELECT 
    COUNT(*) AS total_allocations,
    SUM(CASE WHEN is_recommendation_accepted = true THEN 1 ELSE 0 END) AS accepted_suggestions,
    SUM(CASE WHEN override_reason_code IS NOT NULL THEN 1 ELSE 0 END) AS manual_overrides,
    ROUND(100.0 * SUM(CASE WHEN is_recommendation_accepted = true THEN 1 ELSE 0 END) / COUNT(*), 2) AS suggestion_acceptance_rate_pct
  FROM admission_requests
  WHERE assigned_bed_id IS NOT NULL;
  ```
- **Log Script (Bash / grep)**:
  ```bash
  allocs=$(grep -c 'action="ALLOCATE_BED"' application.log)
  overrides=$(grep -c 'action="OVERRIDE_ALLOCATION"' application.log)
  total=$((allocs + overrides))
  echo "scale=2; ($allocs / $total) * 100" | bc | awk '{print "Suggestion Acceptance Rate: " $1 "%"}'
  ```

#### KPI 8: Number & % of Accepted Diversions (Sister Hospitals & MIC@Home)
- **SQL Extraction**:
  ```sql
  SELECT 
    COUNT(*) AS total_admissions,
    SUM(CASE WHEN diversion_recommended = true THEN 1 ELSE 0 END) AS total_diversions,
    ROUND(100.0 * SUM(CASE WHEN diversion_recommended = true THEN 1 ELSE 0 END) / COUNT(*), 2) AS diversion_rate_pct
  FROM admission_requests;
  ```

#### KPI 9: Sister Hospital Bilateral Acceptance Latency
- **SQL Extraction**:
  ```sql
  SELECT 
    COUNT(*) AS total_referrals,
    SUM(CASE WHEN bilateral_response_mins <= 30 THEN 1 ELSE 0 END) AS within_sla_count,
    ROUND(100.0 * SUM(CASE WHEN bilateral_response_mins <= 30 THEN 1 ELSE 0 END) / COUNT(*), 2) AS sla_compliance_pct
  FROM sister_hospital_referrals;
  ```

#### KPI 10, 11, 12: Batch Holding Ward Adoption & Ghost Bed Recoveries
- **SQL Extraction**:
  ```sql
  -- Batch Holding Room Adoption Rate
  SELECT 
    COUNT(*) AS total_placed_patients,
    SUM(CASE WHEN placed_via_batch = true THEN 1 ELSE 0 END) AS batch_placed_patients,
    ROUND(100.0 * SUM(CASE WHEN placed_via_batch = true THEN 1 ELSE 0 END) / COUNT(*), 2) AS batch_adoption_rate_pct
  FROM admission_requests
  WHERE status IN ('BED_ALLOCATED', 'ADMITTED_INPATIENT');
  ```
- **Log Script (Bash / jq)**:
  ```bash
  grep 'action="APPROVE_BATCH_HOLDING_WARD"' application.log | \
    sed -n 's/.*BatchSize=\([0-9]*\).*/\1/p' | \
    awk '{sum+=$1; count++} END {print "Total Batch Patients Placed:", sum, "(across", count, "batches)"}'
  ```

#### KPI 13: Cohort-Swap Optimization Yield
- **Log Script (Bash / grep)**:
  ```bash
  swaps=$(grep -c 'action="APPROVE_COHORT_SWAP"' application.log)
  echo "Total Dynamic Cohort-Swaps Executed: $swaps"
  ```

#### KPI 16: Prolonged-Wait Communication Rate
- **SQL Extraction**:
  ```sql
  -- Ratio of SLA-breached patients with active delay reason tags
  SELECT 
    COUNT(*) AS total_long_waiting_cases,
    SUM(CASE WHEN delay_reason_tag IS NOT NULL THEN 1 ELSE 0 END) AS documented_delay_cases,
    ROUND(100.0 * SUM(CASE WHEN delay_reason_tag IS NOT NULL THEN 1 ELSE 0 END) / COUNT(*), 2) AS delay_communication_rate_pct
  FROM admission_requests
  WHERE EXTRACT(EPOCH FROM (COALESCE(allocated_at, CURRENT_TIMESTAMP) - requested_at)) / 60.0 > 60.0;
  ```

---

## Testing Decisions

### What Makes a Good Test

Tests must verify external observable behavior and domain invariants through HTTP and service interfaces, rather than testing internal repository queries or private method state:

- **Constraint Enforcement**: Beds violating Ward Class, gender cohorting, or telemetry must never appear in recommendation candidates.
- **Scoring Correctness**: Beds matching specialty clusters and consolidation criteria must reflect configured mathematical bonus weights.
- **State Machine Integrity**: Beds in `EMPTY_PENDING_CLEANING` cannot be allocated; only `EMPTY_CLEANED` beds can transition to `EMPTY_ASSIGNED`; housekeeping clean sign-off transitions `EMPTY_PENDING_CLEANING` to `EMPTY_CLEANED`.
- **Dynamic Batching & Swap Invariants**: Surge clusters of $\ge 3$ matching patients must trigger batch holding ward suggestions; cohort swap must move isolated `Green` beds without violating cohort locks in target wards.
- **Audit & Governance**: Manual overrides must reject missing reason codes and log structured audit entries with coordinator identity.

### Tested Modules & Seams

1. **Primary End-to-End API Seam (`BmuController` via `MockMvc`)**:
   - Tests complete REST endpoints for queue retrieval, recommendation scoring, 1-click allocation, override capturing, batch suggestions, cohort swaps, and housekeeping clean sign-offs.
2. **Heuristic Solver & Constraint Seam (`HeuristicBedAllocationSolver`)**:
   - Verifies hard constraint pruning and dynamic multi-criteria soft score calculations against varied patient and ward profiles.
3. **Capacity & Service Logic Seam (`BmuService`)**:
   - Verifies atomic transaction handling, bed status updates, priority sorting (acuity first, FIFO dwell time second), and delay tag persistence.
4. **Sister Hospital Diversion Seam (`SisterHospitalGateway`)**:
   - Tests referral generation, synthetic reference tracking, and bilateral SLA timer activation.

### Prior Art

- `backend/src/test/java/com/hospital/admissions/service/BmuServiceTest.java`: Demonstrates service-level validation, queue sorting, and allocation execution.
- `backend/src/test/java/com/hospital/admissions/web/BmuControllerTest.java`: Demonstrates web-tier MockMvc testing for BMU endpoints.
- `backend/src/test/java/com/hospital/admissions/gateway/GatewaysAndSolversTest.java`: Demonstrates solver constraint evaluation and sister hospital gateway mock responses.
- `backend/src/test/java/com/hospital/admissions/TracerBulletsIntegrationTest.java`: Demonstrates full vertical tracer bullet flow across ED submission, specialist consult, and BMU bed allocation.

---

## Out of Scope

1. **ED Clinical Intake & Diagnostic Pre-Population**: Initial patient triage, diagnostic scan/lab ingestion, and primary assessment submission are handled by Epic 1.
2. **Specialist Consult Broadcast & Clinical Discordance**: Broadcasting cases to on-call specialty feeds, doctor claiming, and secondary acuity discordance detection are handled by Epic 1.
3. **Public Patient Mobile Milestone Tracker**: End-user smartphone tracker interface, SMS link dispatch, and family-facing delay communication are handled by Epic 3.
4. **Inpatient Discharge Runway & 30-Minute Housekeeping Turnover**: Multi-day EDD entry, morning discharge medication bedside delivery, and nurse vacate triggers belong to Epic 4 (Epic 2 handles BMU capacity reception of the clean bed flip).
5. **Physical Bed Sensors / IoT Hardware**: Real-time bed occupancy sensors or automated RFID tracking; bed status updates rely on digital nurse check-in and housekeeping sign-offs.

---

## Further Notes

- **Profile Gating**: Synthetic hospital inventory (Wards 8A, 8B, 9A) and mock sister hospital referral adapters are strictly active under `@Profile("prototype")`. When transitioning to production, standard dependency injection wires production Timefold solvers and enterprise FHIR gateways without altering core BMU allocation workflows.
- **Batch Holding Ward Sizing**: Default configuration triggers batch suggestions when cluster size $\ge 3$, optimizing typical 4-to-6 bed hospital wards without creating ghost capacity.
