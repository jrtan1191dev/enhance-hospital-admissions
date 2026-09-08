# Epic 1: ED Clinical Admission & Multi-Doctor Assessment Broadcast

## Problem Statement

Emergency Department (ED) physicians work under intense operational pressure while managing acutely ill patients awaiting inpatient admission. The clinical admission process is currently hindered by fragmented diagnostic data across disparate electronic systems, requiring manual synthesis of vital signs, lab panels, and scan reports. 

Furthermore, seeking inpatient specialist input relies on sequential telephone calls, page requests, and verbal handoffs. When on-call specialists are unavailable, cases sit idle without clear ownership or tracking. In scenarios where the ED attending physician and the consulting inpatient specialist disagree on patient acuity tier or required monitoring, patients risk being under-triaged into general ward beds without continuous telemetry, or delayed indefinitely while clinicians debate placement over the phone. Meanwhile, communication of bed requests to the Bed Management Unit (BMU) relies on phone calls and manual entries, resulting in opaque queues and lost operational minutes.

## Solution

The system provides an automated, unified clinical intake and broadcast platform:

1. **Automated Diagnostic Baseline & 1-Click Submission**: Aggregates laboratory panels, vital signs, and diagnostic findings into an objective admission baseline, pre-populating clinical urgency tiers and constraints for 1-click approval or interactive chip adjustment by the ED attending physician.
2. **Direct Digital BMU Dispatch**: Instantly publishes signed admission requests into the centralized BMU queue, indexed by clinical acuity and submission timestamp without requiring manual phone calls.
3. **Asynchronous Multi-Doctor Assessment Broadcast Pool**: Concurrently broadcasts case summaries to active on-call specialty feeds organized by service clusters (Cardiology, General Medicine, Surgery, Orthopaedics). Inpatient specialists can claim cases, review clinical parameters in parallel, and append specialist consult notes, recommended secondary acuity tiers, and alternative care-pathway endorsements (e.g., Sister Hospital or Hospital-at-Home diversion).
4. **SLA Timeout & Automated Escalation**: Monitors unclaimed broadcast cases against configured SLA thresholds, auto-assigning stagnant cases to designated default on-call specialists with urgent notification triggers.
5. **Safety-First Clinical Discordance Engine**: Detects conflicting acuity assessments between the primary ED attending and consulting specialist. The system automatically elevates the effective allocation priority to the higher acuity tier, enforces continuous telemetry/monitoring constraints, and provides side-by-side comparative views for the BMU coordinator.

---

## User Stories

### Feature 1.1: Automated Diagnostic Synthesis & Smart Pre-Populated Assessment

1. As an ED Attending Physician, I want the system to ingest completed laboratory panels (such as Troponin, WBC, Hemoglobin) and real-time vital signs from the patient's EHR baseline, so that I have an objective clinical profile without manually querying multiple hospital subsystems.
2. As an ED Attending Physician, I want the system to analyze ingested diagnostic markers and suggest an initial acuity tier recommendation (Tier 1 Critical through Tier 5 Observation), so that my cognitive triaging load is minimized.
3. As an ED Attending Physician, I want the system to automatically flag required supportive equipment (such as continuous cardiac telemetry or fall-risk alarms) based on diagnostic criteria, so that clinical safety requirements are captured from the first point of contact.
4. As an ED Attending Physician, I want the intake summary to display prominent visual warning badges when diagnostic scans (such as CT Brain or Ultrasound) are still pending, so that I do not prematurely finalize assessments when critical results are outstanding.
5. As an ED Attending Physician, I want the intake interface to display pre-populated patient ward class preferences (Class A, B1, B2, or C) from patient records, so that financial and administrative preferences are automatically factored into the admission dossier.
6. As an ED Attending Physician, I want a smart admission form pre-populated with synthesized clinical findings that I can confirm and lead with a single click, so that I can dispatch acute admission requests in seconds.
7. As an ED Attending Physician, I want interactive dropdown chips to override or fine-tune recommended acuity tiers, admitting specialty clusters, ward class preferences, and isolation constraints before submission, so that clinical judgment always remains the final authority.
8. As a Clinical Auditor, I want all clinician modifications to pre-populated values to be recorded with clinician identity, previous value, modified value, and timestamp in an audit log, so that deviations from automated recommendations remain transparent and accountable.

### Feature 1.2: Multi-Doctor Assessment Broadcast & Specialist Consult Pool

9. As an ED Attending Physician, I want the system to automatically broadcast submitted acute admission packets to the relevant service cluster's open on-call feed, so that on-call specialists can immediately review the case without waiting for manual phone consultations.
10. As an On-Call Specialist, I want to filter the broadcast pool by my service cluster (Cardiology, General Medicine, Surgery, Orthopaedics), so that I can focus strictly on inpatient consults relevant to my department.
11. As an On-Call Specialist, I want to view active case counts and urgency badges across all service cluster feeds, so that I have immediate situational awareness of hospital-wide consult demand.
12. As an On-Call Specialist, I want to claim an open case with a single click, so that the broadcast status transitions to claimed and other specialists see that I have taken ownership of the review.
13. As an On-Call Specialist, I want the system to prevent race conditions when two doctors attempt to claim the same case concurrently, so that case ownership is atomically locked to the first responder.
14. As an ED Floor Administrator, I want an active SLA countdown timer attached to each open broadcast, so that clinicians can track how long cases have remained pending specialist review.
15. As an ED Floor Administrator, I want the system to automatically escalate an unclaimed case to the designated default on-call specialist when the cluster SLA timer expires, so that consult reviews are never neglected during shift turnovers or high-volume periods.
16. As a Default On-Call Specialist, I want to receive an urgent high-priority notification when a case is auto-escalated to me due to SLA expiration, so that I can immediately triage the delayed patient.
17. As an On-Call Specialist, I want to asynchronously submit my consult evaluation (including consult notes, recommended acuity tier, and admitting team directives) directly into the active admission dossier, so that my findings inform bed placement without blocking ongoing ED operations.
18. As an On-Call Specialist, I want to endorse alternative diversion pathways (such as Community Hospital transfer or Mobile Inpatient Care at Home) during my consult submission, so that eligible subacute patients can be evaluated for diversion by the BMU.
19. As an On-Call Specialist, I want to edit or append updates to my previously submitted consult impressions while the patient is awaiting bed allocation, so that evolving clinical conditions are communicated immediately to the bed planners.

### Feature 1.3: Safety-First Clinical Discordance & Acuity Escalation

20. As a Patient Safety Officer, I want the system to compare the primary acuity tier submitted by the ED attending with the secondary acuity tier submitted by the consulting specialist, so that divergent clinical assessments are identified algorithmically.
21. As a BMU Coordinator, I want the system to automatically escalate the effective bed allocation priority to the higher acuity tier whenever a clinical discordance is detected, so that patients are never placed into lower-acuity beds due to conflicting opinions.
22. As a BMU Coordinator, I want any discordant request involving a telemetry recommendation to automatically enforce continuous telemetry capability on candidate beds, so that patient cardiac monitoring is never compromised.
23. As a BMU Coordinator, I want discordant admission requests in the BMU queue to be highlighted with high-visibility discordance warning badges, so that bed planners immediately recognize cases requiring special placement scrutiny.
24. As a BMU Coordinator, I want to view a side-by-side comparison of the ED attending's notes and the consulting specialist's notes, so that I understand the clinical rationale behind their divergent recommendations.
25. As a BMU Coordinator, I want a 1-click action to prompt the ED attending and specialist to reconcile their evaluations, so that clinical alignment can be established when necessary without delaying initial bed reservations.
26. As a Clinical Quality Manager, I want all instances of clinical discordance and automated tier escalations to be logged with both clinicians' identities and rationales, so that inter-departmental triage discrepancies can be reviewed for continuous quality improvement.

### Feature 1.4: Direct Digital Bed Request Dispatch & 5-Tier Priority Framework

27. As an ED Attending Physician, I want my submitted admission assessment to publish directly into the active BMU queue as a digital bed request, so that manual telephone calls between ED clinicians and bed managers are eliminated.
28. As a BMU Coordinator, I want incoming bed requests to be indexed by clinical urgency tiers using a standardized 5-tier framework (Tier 1 Critical, Tier 2 Acute Urgent, Tier 3 Acute Stable, Tier 4 Subacute Diversion, Tier 5 Observation), so that beds are prioritized strictly by clinical acuity and dwell time rather than verbal advocacy.
29. As a BMU Coordinator, I want incoming digital bed requests to contain complete structured patient attributes (gender, ward class preference, infection status, fall risk score, telemetry requirements), so that bed allocation rules have 100% complete data without follow-up inquiries.
30. As an ED Attending Physician, I want immediate visual confirmation and queue status feedback once my assessment is dispatched, so that I know the bed request has been safely received by the BMU.

---

## Implementation Decisions

### 1. Modules & Domain Boundaries
- **Clinician Intake & Assessment Module**: Handles ingestion of diagnostic baselines, validation of ED clinical submissions, and initialization of `AdmissionRequest` entities with status `BED_REQUESTED`.
- **Specialist Broadcast Pool Service**: Manages asynchronous publication of `AssessmentBroadcast` entities to target specialty clusters, atomic case claiming with user association, and submission of specialist consult directives.
- **Discordance & Priority Engine**: Evaluates primary vs. secondary acuity tiers, sets the `discordant` flag on `AdmissionRequest`, recalculates effective placement priorities to the higher tier, and enforces telemetry constraints.
- **Audit & Security Logging Layer**: Captures all admissions, claims, consult submissions, and discordance triggers via Spring Security identity context and structured SLF4J audit events.

### 2. Domain Glossary & Enumerations
- **`AcuityTier`**: `TIER_1_CRITICAL`, `TIER_2_ACUTE_URGENT`, `TIER_3_ACUTE_STABLE`, `TIER_4_SUBACUTE_DIVERSION`, `TIER_5_OBSERVATION`.
- **`SpecialtyCluster`**: `CARDIOLOGY`, `GENERAL_MEDICINE`, `SURGERY`, `ORTHOPAEDICS`.
- **`WardClass`**: `A`, `B1`, `B2`, `C`.
- **`BroadcastStatus`**: `OPEN`, `CLAIMED`, `AUTO_ESCALATED`, `COMPLETED`.
- **`AdmissionStatus`**: `ASSESSMENT_PENDING`, `BED_REQUESTED`, `BED_ALLOCATED`, `ADMITTED_INPATIENT`, `DISCHARGED`.

### 3. API Surface & REST Contracts
- `GET /api/v1/clinicians/ed/patients`: Returns available ED patients awaiting clinical admission assessment.
- `POST /api/v1/clinicians/ed/assessments/submit`: Accepts `EdAssessmentSubmitRequest`, creates an `AdmissionRequest`, and emits an `AssessmentBroadcast` in `OPEN` status.
- `GET /api/v1/clinicians/specialist/broadcasts`: Retrieves active consult broadcasts, optionally filtered by `cluster`.
- `POST /api/v1/clinicians/specialist/broadcasts/{id}/claim`: Atomically transitions broadcast status to `CLAIMED` and sets the authenticated specialist identity.
- `POST /api/v1/clinicians/specialist/broadcasts/{id}/consult`: Accepts `SpecialistConsultRequest`, transitions status to `COMPLETED`, updates secondary acuity tier and diversion status on the underlying `AdmissionRequest`, evaluates discordance, and updates BMU queue visibility.

```typescript
// Core Data Contracts (Prototype-Verified Shape)
interface EdAssessmentSubmitRequest {
  patientId: string;
  suspectedDiagnosisService: SpecialtyCluster;
  primaryAcuityTier: AcuityTier;
  requestedWardClass: WardClass;
  needsTelemetry: boolean;
}

interface SpecialistConsultRequest {
  secondaryAcuityTier: AcuityTier;
  consultNotes?: string;
  diversionRecommended: boolean;
}

interface AssessmentBroadcast {
  id: string;
  admissionRequest: AdmissionRequest;
  targetCluster: SpecialtyCluster;
  status: BroadcastStatus;
  claimedBySpecialistId?: string;
  claimedAt?: string;
  consultNotes?: string;
}
```

### 4. Architectural Decisions (ADR Alignment)
- **ADR-001 (Relational Persistence & Physical Hierarchy)**: All entities extend an auditable base class tracking timestamps and modifying users. Physical hospital layout strictly follows Level -> Ward -> Bed (no intermediate cubicle entities).
- **ADR-003 (Real-Time Communication via Polling & Cache Invalidation)**: Uses TanStack Query with targeted query invalidation on mutation success, supplemented by 3-second background polling on active clinician boards.
- **ADR-004 (Strict REST & Validation Standards)**: Uses Jakarta Bean Validation (`@NotNull`, `@Valid`) on all request bodies, returning RFC 7807 Problem Details on invalid input.
- **ADR-006 (Zero-Auth Persona Switching for Prototype Evaluation)**: In prototype mode, security context is established via request header mapping to seeded clinician identities (`dr_tan_ed`, `dr_lim_cardio`) with full SLF4J audit trail integration.

---

## Testing Decisions

### What Makes a Good Test
Tests must verify externally observable behavior and domain invariants through HTTP and service interfaces, rather than testing internal repository queries or private method state:
- Validation errors return HTTP 400 Bad Request with Problem Details.
- Successful ED intake submissions transition patient status to `BED_REQUESTED` and create an active `OPEN` broadcast.
- Claiming an open broadcast locks the record to the caller and changes status to `CLAIMED`.
- Specialist consult submissions set broadcast status to `COMPLETED` and update the `AdmissionRequest` with secondary acuity and diversion flags.
- Submitting conflicting acuity tiers between ED and Specialist automatically flags `discordant = true` and surfaces the higher acuity tier in BMU queue order.

### Tested Modules & Seams
1. **Primary End-to-End API Seam (`ClinicianController` via `MockMvc`)**:
   - Tests complete HTTP request and response cycle, JSON serialization/deserialization, bean validation, and security context extraction.
2. **Service & Domain Invariant Seam (`ClinicianService`)**:
   - Tests business logic for atomic broadcast transitions, SLA auto-escalation conditions, and discordance calculation.
3. **Audit Trail Seam (`AuditLogger`)**:
   - Verifies that security principal names and action tags (`SUBMIT_ED_ASSESSMENT`, `CLAIM_BROADCAST`, `SUBMIT_SPECIALIST_CONSULT`) are logged with structured details.

### Prior Art
- `backend/src/test/java/com/hospital/admissions/TracerBulletsIntegrationTest.java`: Demonstrates the end-to-end tracer bullet flow from ED assessment submission to specialist claim and BMU queuing.
- `backend/src/test/java/com/hospital/admissions/web/ClinicianControllerTest.java`: Demonstrates web-tier mock verification for all clinician endpoints.
- `backend/src/test/java/com/hospital/admissions/service/ClinicianServiceTest.java`: Demonstrates service-tier business logic and exception handling.

---

## Out of Scope

1. **Automated Bed Constraint Solving & 4-State Bed Machine**: Evaluating ward bed availability, constraint satisfaction, and 1-click bed allocations belongs to Epic 2 (BMU Bed Capacity).
2. **Dynamic Holding Ward & Flex-Cubicle Batching**: Automatic conversion of wards into temporary holding spaces is handled by Epic 2.
3. **External Sister Hospital Referral Dispatch & SLA Countdown**: Executing transfer agreements with Outram Community Hospital or MIC@Home is handled by Epic 2.
4. **Public Patient Mobile Journey Tracker**: Token-based queue progression and family notifications are handled by Epic 3.
5. **Inpatient Discharge Runway & 30-Minute Housekeeping Turnover**: Discharge planning, bedside pharmacy dispensing, and room sanitization are handled by Epic 4.
6. **Live Telemedicine / Audio Calls**: Video/audio calling between ED attendings and specialists; communication is strictly asynchronous through structured consult packets and notes.

---

## Further Notes

- **Profile Gating**: Synthetic ED patients and mock EHR diagnostic baselines are strictly active under `@Profile("prototype")`. When transitioning to production, standard dependency injection wires production FHIR clients without altering core admission or broadcast logic.
