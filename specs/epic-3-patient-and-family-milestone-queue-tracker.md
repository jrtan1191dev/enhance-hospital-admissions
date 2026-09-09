# Epic 3: Patient & Family Milestone & Queue Tracker

## Problem Statement

Patients admitted through the Emergency Department (ED) and their accompanying family members experience acute anxiety, emotional distress, and vulnerability during the post-admission boarding period. Once an attending physician determines that an inpatient admission is necessary, patients are typically placed in holding corridors or observation bays where they enter an operational "black box."

In traditional workflows, patients and caregivers face several critical pain points:
1. **Opaque Waiting and Information Voids**: Patients have no visibility into the stages required before an inpatient bed is ready (e.g., matching clinical constraints, terminal cleaning, room sanitization, and porter dispatch). They cannot discern whether their request has been received, is being processed, or has been lost.
2. **Repeated Staff Interruptions**: Lacking self-service tracking, anxious families repeatedly approach ED triage desks and bedside nurses to ask "How much longer?", "Is my room ready?", and "Why was that other patient taken first?". This creates cognitive friction for clinicians who must interrupt acute care delivery to search for bed updates.
3. **Misunderstanding of Non-FIFO Clinical Triage**: General public expectations assume hospital admission operates on a first-come, first-served (FIFO) basis. When a higher-acuity patient (e.g., acute myocardial infarction or trauma) is allocated a bed ahead of someone who arrived earlier, patients perceive unfairness or neglect in the absence of clear clinical context.
4. **Financial Ambiguity and Fear of Hidden Costs**: When clinicians discuss ward classes (Class A, B1, B2, C) or step-down care (Community Hospital transfer or virtual wards like MIC@Home), families fear unexpected medical bills. Lacking immediate estimates of subsidies, MediSave coverage, and daily co-pays, caregivers often delay making care decisions.
5. **Frustration Over Unexplained Delays**: When unforeseen operational bottlenecks occur (such as mandatory 30-minute UV sanitization for negative pressure isolation rooms), patients receive no explanation, leading to complaints and escalated tensions.

## Solution

The system introduces a secure, token-activated mobile **Patient & Family Public Milestone Tracker** accessible on any mobile smartphone without requiring native app downloads, complex account registrations, or login credentials:

1. **Dispatch-Gated Activation & 3-Stage Milestone Stepper**: The public tracking token remains in a quiescent triage state ("ED Clinical Assessment in Progress") while emergency department doctor and specialist evaluations are ongoing. Upon attending physician sign-off and dispatch of the bed request to BMU (`BED_REQUESTED`), the tracker activates at Milestone 1 and guides the patient through a transparent 3-stage journey:
   - *Milestone 1*: Admission Decision Confirmed & Bed Queued (Bed request actively matched by BMU).
   - *Milestone 2*: Bed Assigned & Preparing Room (Matching ward identified; housekeeping sanitization underway).
   - *Milestone 3*: Admitted to Inpatient Ward Bed (Patient received and checked in by ward nursing staff).
2. **Transparent Queue Metrics & Clinical Priority Context**: Displays real-time estimated wait duration (dynamically adjusted with operational delay buffers), the number of patients ahead in the matching ward class (`requestedWardClass`), and empathetic contextual explanations stating that hospital bed allocations are prioritized by acute clinical urgency (`effectiveAcuityTier`) and infection control safety rather than arrival order.
3. **Automated 5-Minute Status Refreshes & Empathetic Delay Disclosures**: Delivers automated status updates pushed via simulated SMS/notifications periodically (5 minutes in prototype mode, 2 hours in production) or immediately upon milestone advancement. When BMU coordinators attach operational delay tags (hybrid standard enum + free-text custom reason), the tracker translates technical operational tags into compassionate, patient-friendly explanations with ward liaison contact details. Active delay tags are automatically cleared/archived when the request advances to `BED_ALLOCATED`.
4. **Interactive Financial & Care Explainer (FYI Insights)**: Integrates non-intimidating informational advisory cards in the frontend that outline estimated daily out-of-pocket co-pays, government means-tested subsidy percentages (Class A, B1, B2, C), MediShield Life coverage, and alternative care benchmarks (e.g., 14 to 21 days for Community Hospital transfers or virtual ward details for MIC@Home). Direct 1-click action buttons enable caregivers to dial Medical Social Work (MSW) or Financial Counseling hotlines directly from their device.

---

## User Stories

### Feature 3.1: Dispatch-Activated Milestone Queue Tracker

1. As a Patient or Family Member, I want my mobile tracker link to remain in pre-admission triage status while ED doctor evaluations are ongoing, so that I am not confused or alarmed by interim clinical deliberations before an admission decision is confirmed.
2. As a Patient or Family Member, I want to receive an automated SMS notification containing a secure, personalized token URL the moment the ED attending physician signs off and dispatches my bed request to BMU, so that I can immediately track my admission progress.
3. As a Patient or Family Member, I want to open the tracking link directly in any standard mobile web browser without downloading a native mobile app or entering passwords, so that tracking is effortless during an emergency.
4. As a Patient or Family Member, I want to see a clear 3-stage visual milestone stepper (`Admission Confirmed` $\to$ `Bed Assigned & Sanitizing` $\to$ `Admitted to Ward`), so that I understand where I am in the hospital admission process.
5. As a Patient or Family Member, I want the tracker to automatically advance to Milestone 2 when BMU approves a bed allocation, so that I know a physical room has been secured for my care.
6. As a Patient or Family Member, I want the tracker to display my assigned bed number, ward name, and floor level once a bed has been assigned, so that my family knows our inpatient destination.
7. As a Patient or Family Member, I want the tracker to advance to Milestone 3 when the ward nurse completes my bedside check-in, so that my family members receive confirmation of my safe arrival in the ward.
8. As a Patient or Family Member, I want to see a dynamically calculated estimated wait duration (e.g., "~45 mins"), with additive operational buffers applied when delays occur, so that I have realistic expectations of boarding time.
9. As a Patient or Family Member, I want to see the number of patients ahead of me in the queue for my matching ward class category (`requestedWardClass`), prioritized by effective acuity tier, so that queue progress is transparent.
10. As a Patient or Family Member, I want the tracker to display clear context explaining that hospital admissions are prioritized by acute clinical urgency and safety rather than first-come-first-served order, so that I understand why arrival sequence does not dictate bed allocation.
11. As an Evaluator or Prototype Demonstrator, I want a patient quick-picker dropdown in the simulator interface, so that I can switch between simulated patient tokens (`TOKEN-P101`, `TOKEN-P102`) in one click without manually typing URLs.

### Feature 3.2: Automated Status Updates & Delay Explanations

1. As a Patient or Family Member, I want to receive an automated status update on my phone periodically (every 5 minutes in prototype evaluation) while waiting in the bed queue, so that I remain reassured that my admission is actively being handled without needing to ask the nurse.
2. As a Patient or Family Member, I want to receive an instant push notification or SMS whenever my milestone changes, so that I am immediately notified of important transitions.
3. As a Patient or Family Member, I want the tracker to display clear, empathetic explanations when my wait is prolonged due to operational factors, so that I understand what is causing the delay rather than being left in an information void.
4. As a Patient or Family Member, I want specialized cleaning delays (e.g., UV terminal sanitization of an airborne isolation room) to be explained in terms of patient safety and infection prevention, so that I feel protected rather than frustrated.
5. As a Patient or Family Member, I want direct contact information for the ward liaison or nurse hotline displayed alongside delay explanations, so that I can ask questions if I have special comfort or medical needs while waiting.
6. As an ED Floor Nurse, I want patient trackers to answer common boarding questions proactively, so that the volume of repetitive status inquiries at the nursing desk is significantly reduced.
7. As a Clinical Operations Manager, I want periodic notification delivery rates and timestamps to be logged, so that communication consistency across all admitted patients can be audited.

### Feature 3.3: Patient Financial & Care Explainer (FYI Insights)

1. As a Patient or Authorized Caregiver, I want an interactive financial advisory card showing estimated daily out-of-pocket co-pays based on my requested ward subsidy class (Class A, B1, B2, or C), so that I have clarity on hospitalization costs from the start.
2. As a Patient or Authorized Caregiver, I want the financial explainer to display applicable government subsidy tiers (e.g., up to 70% means-tested subsidy for Class B2/C) and MediShield Life coverage, so that I understand how government healthcare financing applies to my stay.
3. As a Patient or Authorized Caregiver, I want the explainer to clearly state that cost breakdowns are informational estimates for peace of mind ("FYI Insights") and do not require digital signatures or upfront deposits, so that reading financial information does not feel stressful.
4. As a Patient or Authorized Caregiver, I want patients recommended for step-down care (such as Outram Community Hospital or St. Andrew's Community Hospital) to see estimated daily costs and expected rehabilitation lengths of stay (e.g., 14 to 21 days), so that families can plan caregiver arrangements.
5. As a Patient or Authorized Caregiver, I want patients evaluated for Hospital-at-Home (MIC@Home) to view an explanation of virtual ward monitoring, visiting nurse schedules, and home equipment delivery, so that they feel confident receiving hospital-level care at home.
6. As a Patient or Authorized Caregiver, I want a 1-click action button to call Medical Social Work (MSW) directly from the mobile tracker, so that families with financial distress or caregiving challenges can access immediate counseling.
7. As a Patient or Authorized Caregiver, I want a 1-click button to contact Hospital Financial Counseling, so that questions regarding MediSave balances and insurance claims can be addressed during admission.

---

## Implementation Decisions

### 1. Modules & Domain Boundaries

- **Patient Journey & Milestone Tracking Service (`PatientTrackerService`)**: Resolves public tokens to active `AdmissionRequest` entities, evaluates current journey milestones (1 through 3), calculates estimated wait durations with delay buffers, and maps clinical delay tags to empathetic user-facing copy.
- **Dynamic Queue Position Calculator**: Evaluates pending `BED_REQUESTED` queues partitioned by `requestedWardClass` and sorted by `effectiveAcuityTier` and `requestedAt` timestamp to compute the patient's ordinal position and queue depth in their matching category.
- **Financial & Care Advisory (Frontend FYI Insights)**: Displays structured static subsidy breakdowns, co-pay estimates, and rehabilitation benchmarks based on the patient's `requestedWardClass` and diversion pathway (`COMMUNITY_HOSPITAL_TRANSFER` or `MIC_AT_HOME`).
- **Public Patient Tracker Controller (`PatientTrackerController`)**: Exposes unauthenticated, token-scoped REST endpoints returning sanitized, patient-safe DTOs with no leakage of internal clinical notes, diagnostic data, or other patients' PII.
- **Automated Milestone Notification Scheduler**: Periodically identifies active admission requests waiting $\ge 5$ minutes and dispatches periodic queue refresh messages (`DISPATCH_PERIODIC_UPDATE`) and milestone progression alerts.

### 2. Domain Glossary & Milestone State Mapping

- **Public Token (`queueToken`)**: Secure alphanumeric token (e.g., `TOKEN-P101`, `TOKEN-P102`) generated upon admission creation.
- **Milestone Stepper States**:
  - **`Quiescent Pre-Milestone: ED Assessment in Progress`**: Displayed when `AdmissionRequest.status` is `ASSESSMENT_PENDING`.
  - **`Milestone 1: Admission Decision Confirmed`**: Triggered when `AdmissionRequest.status` is `BED_REQUESTED`.
  - **`Milestone 2: Bed Assigned & Preparing Room`**: Triggered when `AdmissionRequest.status` is `BED_ALLOCATED`.
  - **`Milestone 3: Admitted to Inpatient Bed`**: Triggered when `AdmissionRequest.status` is `ADMITTED_INPATIENT`.
- **Operational Delay Reason Mapping**:
  - Standard Enum (`OperationalDelayTag`):
    - `HOUSEKEEPING_DELAY` $\to$ *"Your ward bed is currently undergoing final housekeeping sanitization and linen preparation."* (+20 mins wait buffer)
    - `BED_SHORTAGE` $\to$ *"Our clinical coordinators are actively prioritizing ward beds across the hospital to ensure optimal clinical placement."* (+30 mins wait buffer)
    - `SPECIALIZED_ISOLATION_CLEANING` $\to$ *"Your specialized isolation room is completing a mandatory 30-minute UV disinfection cycle for your safety."* (+30 mins wait buffer)
    - `SURGE_TRAUMA_EVENT` $\to$ *"The emergency department is currently managing critical trauma arrivals. Thank you for your patience as urgent cases are stabilized."* (+45 mins wait buffer)
  - Custom / Free-text (`OTHER`):
    - Free-form remarks entered by BMU coordinators fall back to empathetic clinical coordination copy with liaison hotline contact info.
  - Active delays are automatically archived/cleared upon transition to `BED_ALLOCATED`.

### 3. API Surface & REST Contracts

- `GET /api/v1/patients/track/{token}`: Returns real-time milestone progress, queue position, estimated wait minutes, assigned bed details, operational delay disclosures, and ward class / diversion flags.
- `GET /api/v1/patients/tokens`: Returns list of available patient records with their public tokens (strictly active under `@Profile("prototype")` to power the UI simulator patient quick-picker).
- `POST /api/v1/patients/beds/{bedId}/checkin`: Ward nurse checks in patient arrival $\to$ updates bed to `OCCUPIED_TAKEN` and admission request to `ADMITTED_INPATIENT` (advancing tracker to Milestone 3).
- `POST /api/v1/patients/beds/{bedId}/vacate`: Ward nurse marks patient discharged $\to$ updates bed to `EMPTY_PENDING_CLEANING` and admission request to `DISCHARGED`.
- `POST /api/v1/patients/beds/{bedId}/clean`: Housekeeping clean sign-off $\to$ updates bed to `EMPTY_CLEANED`.
- `POST /api/v1/patients/simulate-periodic-update`: Triggers scheduled notification cycle under `@Profile("prototype")`.

```typescript
// Core Data Contracts (Prototype-Verified Shape)
interface PatientMilestoneResponse {
  patientId?: string;
  patientName: string;
  queueToken: string;
  admissionStatus: AdmissionStatus;
  requestedWardClass: WardClass;
  queuePosition: number;
  patientsAhead: number;
  estimatedWaitMinutes: number;
  assignedBedNumber?: string;
  assignedWardName?: string;
  assignedLevel?: number;
  delayReason?: string;
  delayContactHotline?: string;
  coPayEstimate?: string;
  careGuidance?: string;
  diversionRecommended?: boolean;
  diversionPathway?: DiversionPathway;
}
```

### 4. Architectural Decisions (ADR Alignment)

- **ADR-001 (Relational Design & Spatial Hierarchy)**: Bed assignments display Level $\to$ Ward $\to$ Bed hierarchy with no cubicles (e.g., "Bed 8A-04, Ward 8A, Level 8").
- **ADR-003 (Polling & Real-Time Synchronization)**: The mobile tracker frontend leverages TanStack Query with active 3-second background polling (`refetchInterval: 3000`), ensuring that milestone transitions (e.g., bed allocation, nurse check-in) reflect on screen instantaneously without manual page refreshes.
- **ADR-004 (Strict REST & Sanitized Responses)**: Endpoints return strict, minimal DTOs. Sensitive diagnostic reports, doctor notes, and clinical discordance details are strictly excluded from patient-facing responses. Invalid tokens return RFC 7807 `ProblemDetail` with HTTP 404 Not Found.
- **ADR-005 & ADR-006 (Zero-Auth Token Security & Persona Switching)**: Public tracker access is secured via unguessable opaque tokens rather than clinical login sessions. In prototype mode, a topbar persona switcher allows evaluators to simulate the mobile phone view within a centered mobile frame.

---

## KPI Instrumentation & Extraction Recommendations

All Epic 3 public patient tracking and communication KPIs are instrumented via token access logs, scheduled notification events, and interaction audit logs.

### 1. Structured Audit Log Events

- `TRACK_PATIENT_ACCESS`:
  - `target`: `PatientToken:{token}`
  - `details`: `PatientId={id}, AdmissionStatus={status}, MilestoneStep={step}, QueuePos={pos}, EstWaitMins={mins}`
- `DISPATCH_PERIODIC_UPDATE`:
  - `target`: `AdmissionRequest:{id}`
  - `details`: `Channel=SMS_PUSH, Token={token}, Milestone={step}, DwellMins={mins}, DeliveryStatus=SUCCESS`
- `VIEW_FINANCIAL_EXPLAINER`:
  - `target`: `AdmissionRequest:{id}`
  - `details`: `WardClass={class}, CoPayRange={range}, Token={token}`
- `CONNECT_MSW_HOTLINE`:
  - `target`: `Patient:{id}`
  - `details`: `Service=MEDICAL_SOCIAL_WORK, AdmissionId={reqId}, Action=CLICK_TO_CALL`
- `CONNECT_FINANCIAL_COUNSELING`:
  - `target`: `Patient:{id}`
  - `details`: `Service=FINANCIAL_COUNSELING, AdmissionId={reqId}, Action=CLICK_TO_CALL`

### 2. Database Schema Audit Fields

- `admission_requests`: `public_tracking_token`, `first_tracker_accessed_at`, `last_tracker_accessed_at`, `tracker_access_count`, `last_periodic_update_sent_at`, `delay_reason_tag`
- `patient_audit_interactions`: `token`, `action_type` (`ACCESS`, `MSW_CALL`, `FINANCE_CALL`), `created_at`

### 3. Metric Computation Recipes

#### KPI 14: Patient & Family Portal Login & Access Rate
- **SQL Extraction**:
  ```sql
  SELECT 
    COUNT(*) AS total_dispatched_requests,
    SUM(CASE WHEN first_tracker_accessed_at IS NOT NULL THEN 1 ELSE 0 END) AS accessed_tracker_count,
    ROUND(100.0 * SUM(CASE WHEN first_tracker_accessed_at IS NOT NULL THEN 1 ELSE 0 END) / COUNT(*), 2) AS tracker_access_rate_pct
  FROM admission_requests
  WHERE status IN ('BED_REQUESTED', 'BED_ALLOCATED', 'ADMITTED_INPATIENT');
  ```
- **Log Script (Bash / jq)**:
  ```bash
  dispatched=$(grep -c 'action="SUBMIT_ED_ASSESSMENT"' application.log)
  accessed=$(grep -o 'target="PatientToken:[^"]*"' application.log | sort -u | wc -l)
  echo "scale=2; ($accessed / $dispatched) * 100" | bc | awk '{print "Patient Portal Access Rate: " $1 "%"}'
  ```

#### KPI 15: 2-Hour Periodic Update Delivery Rate
- **SQL Extraction**:
  ```sql
  -- Checks for waiting patients who remained in BED_REQUESTED for >= 120 mins
  SELECT 
    COUNT(*) AS long_wait_cohort_count,
    SUM(CASE WHEN last_periodic_update_sent_at IS NOT NULL THEN 1 ELSE 0 END) AS received_2hr_update_count,
    ROUND(100.0 * SUM(CASE WHEN last_periodic_update_sent_at IS NOT NULL THEN 1 ELSE 0 END) / COUNT(*), 2) AS update_delivery_rate_pct
  FROM admission_requests
  WHERE EXTRACT(EPOCH FROM (COALESCE(allocated_at, CURRENT_TIMESTAMP) - requested_at)) / 60.0 >= 120.0;
  ```
- **Log Script (Bash / grep)**:
  ```bash
  updates_sent=$(grep -c 'action="DISPATCH_PERIODIC_UPDATE"' application.log)
  echo "Total Automated 2-Hour Queue Refreshes Delivered: $updates_sent"
  ```

#### KPI 17: Nursing Wait-Time Inquiries & Complaints Reduction
- **Measurement Script**:
  - Measured by contrasting recorded nursing desk triage interruptions before vs. after public tracker activation via audit event `LOG_NURSING_DESK_INQUIRY`.

#### KPI 18: Early MSW & Financial Counseling Connect Rate
- **SQL Extraction**:
  ```sql
  SELECT 
    COUNT(DISTINCT a.id) AS diversion_candidate_count,
    COUNT(DISTINCT i.admission_id) AS assisted_counseling_calls,
    ROUND(100.0 * COUNT(DISTINCT i.admission_id) / COUNT(DISTINCT a.id), 2) AS counseling_connect_rate_pct
  FROM admission_requests a
  LEFT JOIN patient_audit_interactions i 
    ON a.id = i.admission_id AND i.action_type IN ('MSW_CALL', 'FINANCE_CALL')
  WHERE a.diversion_recommended = true;
  ```
- **Log Script (Bash / grep)**:
  ```bash
  msw_clicks=$(grep -c 'action="CONNECT_MSW_HOTLINE"' application.log)
  fin_clicks=$(grep -c 'action="CONNECT_FINANCIAL_COUNSELING"' application.log)
  echo "Financial & Social Counseling Direct Engagements: MSW=$msw_clicks, FinancialCounseling=$fin_clicks"
  ```

---

## Testing Decisions

### What Makes a Good Test

Tests must verify external observable behavior and domain invariants through HTTP and service interfaces, rather than testing internal repository queries or private method state:

- **Token Resolution**: Valid tokens return HTTP 200 OK with correct patient milestone data; invalid tokens return HTTP 404 Not Found with Problem Details.
- **Milestone Progression**: 
  - `BED_REQUESTED` status must yield Milestone 1 with active queue position and estimated wait.
  - `BED_ALLOCATED` status must yield Milestone 2 with assigned bed, ward, and level details.
  - `ADMITTED_INPATIENT` status must yield Milestone 3 with 0 mins remaining wait and queue position 0.
- **Privacy & PII Protection**: Verify that clinical consult impressions, primary doctor notes, and internal discordance flags are completely absent from the JSON response.
- **Empathetic Delay Copy**: Structured delay reason tags on the admission request must translate into empathetic user-facing disclosures in the response payload.
- **Financial Advisory Consistency**: Verify that requested ward classes (Class A vs. Class B2/C) correctly reflect subsidized co-pay ranges and appropriate guidance text.

### Tested Modules & Seams

1. **Primary End-to-End API Seam (`PatientTrackerController` via `MockMvc`)**:
   - Tests HTTP GET `/api/v1/patients/track/{token}`, response code mappings, token verification, and JSON serialization.
2. **Service & Domain Invariant Seam (`PatientTrackerService`)**:
   - Tests business logic for queue position calculation, wait-time estimation formulas, milestone derivation, and status transitions (`checkinPatient`, `vacatePatient`, `cleanBed`).
3. **Audit Trail Seam (`AuditLogger`)**:
   - Verifies that ward check-in, vacate, and clean actions trigger structured audit logging events.

### Prior Art

- `backend/src/test/java/com/hospital/admissions/service/PatientTrackerServiceTest.java`: Demonstrates service-level validation of milestone mapping, wait duration calculations, and bed check-in/vacate transitions.
- `backend/src/test/java/com/hospital/admissions/web/PatientTrackerControllerTest.java`: Demonstrates web-tier MockMvc testing for public tracking endpoints.
- `backend/src/test/java/com/hospital/admissions/TracerBulletsIntegrationTest.java`: Demonstrates end-to-end integration where allocating a bed and checking in a patient immediately updates the public tracker response.

---

## Out of Scope

1. **Patient Clinical Chart Access**: Viewing laboratory results, radiology scan images, or physician progress notes is handled by the official hospital EHR/HealthHub patient portal, not this admission tracker.
2. **Payment Processing & Upfront Deposit Collection**: Digital payment gateways, credit card processing, and live MediSave deduction authorizations belong to hospital finance billing systems.
3. **Internal BMU Bed Optimization & Constraint Solving**: Algorithmic bed scoring, holding ward batching, and cohort swaps belong to Epic 2.
4. **Physician Admission Assessment & Specialist Broadcast**: Primary intake forms, diagnostic pre-population, and specialist claiming belong to Epic 1.
5. **Inpatient Ward Rounds & Medication Logistics**: Morning ward rounds, discharge runway forecasting, and bedside pharmacy delivery belong to Epic 4.

---

## Further Notes

- **Channel Agnostic Design**: While the prototype implements a responsive mobile web tracker inside a smartphone simulator frame, the backend DTO design directly supports omnichannel dispatch (e.g., WhatsApp Business API, Gov.sg SMS gateway, or SingHealth HealthHub native app push notifications) when transitioning to production.
- **Accessibility & Multilingual Support**: In production, patient-facing copy must support Singapore's four official languages (English, Chinese, Malay, Tamil); the prototype focuses on clear, empathetic English text.
