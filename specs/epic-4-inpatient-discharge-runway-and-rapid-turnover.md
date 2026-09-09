# Epic 4: Inpatient Discharge Runway & Rapid Bed Turnover Logistics

## Problem Statement

Hospital inpatient bed shortages and Emergency Department (ED) boarding crises are fundamentally driven by delays in discharging recovered patients and turning over vacated beds ("exit block"). Even when physical bed spaces exist, operational inefficiencies throughout the discharge and cleaning pipeline severely bottleneck patient flow.

In standard inpatient operations, several compounding factors cause beds to remain occupied or unavailable long after clinical recovery:
1. **Lack of Advance Discharge Visibility**: Inpatient attending physicians typically evaluate patients during morning ward rounds, but discharge plans are rarely formalized until late morning or early afternoon. Ward nurses, care coordinators, and bed planners have no advance warning 2 to 3 days prior (D-2 / D-3), preventing early arrangements.
2. **Day-of-Discharge Caregiver Bottlenecks**: Families and caregivers are frequently caught unprepared when informed on the morning of discharge. Caregivers scramble to take leave from work, arrange non-emergency medical transport, secure home medical equipment (e.g., oxygen concentrators, hospital beds, suction machines), and complete mandatory specialized training (e.g., insulin administration, wound dressing, catheter care). These last-minute hurdles push physical room departure into late afternoon or evening (3:00 PM – 6:00 PM).
3. **Outpatient Pharmacy Exit Queues**: Discharge medications are traditionally prescribed only after morning rounds, leaving the inpatient pharmacy with an overwhelming mid-day surge. Recovered patients either wait 1 to 2 hours in their hospital beds while paper prescriptions are processed, or endure long waits at crowded outpatient pharmacy counters before heading home.
4. **Disjointed Housekeeping Turnover**: When a patient finally leaves the ward, communication to Environmental Services (EVS) relies on manual phone calls, sticky notes, or whiteboard updates. Vacated beds sit idle, uncleaned, and untracked for extended periods. Without automated turnover task dispatch or strict turnaround SLAs, hours elapse before clean beds are returned to the Bed Management Unit (BMU) for incoming ED patients.

## Solution

The system provides an automated, closed-loop **Inpatient Discharge Runway & Rapid Bed Turnover Logistics** platform:

1. **Multi-Day Runway & Potential Discharge Indicator (D-2 / D-3)**: Enables inpatient attending physicians and ward nurses to record an Estimated Date of Discharge (EDD) paired with clinical confidence ratings (High, Medium, Low) during morning ward rounds 48 to 72 hours in advance. Visual "Potential Discharge" indicators (D-2 / D-3) highlight imminent departures across ward consoles and provide the BMU with predictive capacity forecasts.
2. **Automated Day-of-Discharge Morning Sign-Off & Bedside Medication Delivery (Ward Console Integrated)**: Morning physician or charge nurse sign-off (targeted before 09:30 AM) triggers discharge medication dispensing directly within the ward workflow (`PACKING_IN_PROGRESS`). Upon bedside delivery, the ward team confirms receipt (`DELIVERED_BEDSIDE`), advancing the patient journey to "Ready to Vacate" and eliminating outpatient pharmacy delays.
3. **30-Minute Housekeeping Turnover & BMU Bed Release**: The moment a patient vacates the bed, the ward nurse taps "Patient Vacated" on the console. The bed immediately transitions from `OCCUPIED_TAKEN` (`Grey`) to `EMPTY_PENDING_CLEANING` (`Mustard Yellow`) and initiates a live 30-minute SLA countdown (`ON_TRACK`, `APPROACHING_SLA`, `BREACHED`). Upon sanitization completion, the housekeeper taps "Sign-Off Clean," immediately flipping the bed to `EMPTY_CLEANED` (`White`) and releasing capacity back into the BMU live allocation engine.

---

## User Stories

### Feature 4.1: Multi-Day Runway & Potential Discharge Indicator (D-2 / D-3)

1. As an Inpatient Attending Physician or Ward Charge Nurse, I want to record an Estimated Date of Discharge (EDD) and confidence level (High, Medium, Low) during morning ward rounds, so that ward staff and bed planners can identify potential discharges 2 to 3 days in advance.
2. As an Inpatient Attending Physician or Ward Charge Nurse, I want to update or extend the EDD with clinical rationale if the patient's condition changes, so that discharge planning remains aligned with actual clinical trajectory.
3. As an Inpatient Ward Charge Nurse, I want the ward bed roster to highlight patients flagged with D-2 and D-3 Potential Discharge Indicators, so that nursing staff can anticipate imminent discharges.
4. As a BMU Coordinator, I want to view upcoming discharge projections 24 to 72 hours out categorized by ward and specialty cluster, so that capacity planning can anticipate bed vacancies before physical discharge occurs.
5. As a Patient or Family Caregiver, I want to see our planned Estimated Date of Discharge on the public tracker, so that we have clear expectations of our discharge timeline.

### Feature 4.2: Automated Day-of-Discharge Bedside Medication Delivery

1. As an Inpatient Attending Physician or Ward Charge Nurse, I want a 1-click "Final Discharge Sign-Off" action on the ward console during morning rounds (before 09:30 AM), so that discharge authorizations are completed early without administrative delays.
2. As a Ward Nurse, I want the morning discharge sign-off to immediately transition the patient's medication status to `PACKING_IN_PROGRESS`, so that medications are pre-packed hours before the patient vacates.
3. As a Ward Nurse or Medication Runner, I want a 1-click "Confirm Bedside Delivery" action on the ward console, so that bedside medication handoff is recorded without requiring complex standalone pharmacy portals.
4. As a Patient or Family Member, I want pre-packed discharge medications delivered directly to my bedside, so that I do not have to wait in long lines at the outpatient pharmacy counter.
5. As a Patient or Family Member, I want my mobile journey tracker to display "Medications Received at Bedside — Ready to Vacate", so that I know my clinical discharge requirements are 100% complete.

### Feature 4.3: 30-Minute Housekeeping Turnover & BMU Bed Release

1. As an Inpatient Ward Nurse, I want a 1-click "Patient Vacated" action on the ward console the moment a patient physically departs, so that bed vacancy is recorded without delay.
2. As an Inpatient Ward Nurse, I want vacating a bed to automatically transition its status from `OCCUPIED_TAKEN` (`Grey`) to `EMPTY_PENDING_CLEANING` (`Mustard Yellow`), so that uncleaned beds are immediately blocked from premature allocation.
3. As an EVS Housekeeping Supervisor and Specialist, I want vacated beds to display a dynamic 30-minute SLA countdown on the ward turnover queue, indicating whether turnover is `ON_TRACK`, `APPROACHING_SLA`, or `BREACHED` with overdue minutes.
4. As an EVS Housekeeping Specialist, I want to tap "Sign-Off Clean" upon terminal sanitization completion, immediately recording turnover time and compliance.
5. As a BMU Coordinator, I want completed housekeeping sign-offs to immediately flip the bed status from `EMPTY_PENDING_CLEANING` (`Mustard Yellow`) to `EMPTY_CLEANED` (`White`), so that capacity is released back to the hospital.
6. As a BMU Coordinator, I want newly cleaned `White` beds to instantly appear in live recommendation algorithms for waiting ED patients, so that bed turnover latency is eliminated.
7. As a Hospital Facilities Director, I want turnover performance metrics (elapsed cleaning minutes vs. 30-minute SLA) to be tracked and logged in audit trails, so that housekeeping efficiency and turnaround compliance can be continuously measured.

---

## Implementation Decisions

### 1. Modules & Domain Boundaries

- **Discharge Runway & EDD Forecasting Service (`WardService` / `AdmissionRequest`)**: Manages Estimated Date of Discharge (EDD) records, confidence indicators, and dynamic ward-level runway calculations (`RUNWAY_D3`, `RUNWAY_D2`, `RUNWAY_D1`, `READY_FOR_MORNING_SIGNOFF`). Computes 24–72 hour aggregate capacity projections for BMU coordinators.
- **Bedside Medication Delivery Coordination**: Manages streamlined day-of-discharge medication statuses (`NOT_DISPATCHED`, `PACKING_IN_PROGRESS`, `DELIVERED_BEDSIDE`) directly in the Ward Console.
- **Ward Nursing Bed Management Service (`PatientTrackerService`)**: Manages physical bed transitions across `EMPTY_ASSIGNED` (`Green`), `OCCUPIED_TAKEN` (`Grey`), and `EMPTY_PENDING_CLEANING` (`Mustard Yellow`) via nurse check-in and vacate actions.
- **EVS Housekeeping Turnover Engine**: Calculates dynamic 30-minute SLA countdowns (`ON_TRACK`, `APPROACHING_SLA`, `BREACHED`), logs cleaning performance metrics, and executes the state flip from `EMPTY_PENDING_CLEANING` (`Mustard Yellow`) to `EMPTY_CLEANED` (`White`).

### 2. Domain Glossary & Enumerations

- **`BedStatus`**:
  - `OCCUPIED_TAKEN` (`Grey`): Patient physically admitted and occupying bed.
  - `EMPTY_PENDING_CLEANING` (`Mustard Yellow`): Patient vacated, empty, pending terminal sanitization (unavailable for allocation).
  - `EMPTY_CLEANED` (`White`): Cleaned, sanitized, inspected, and unallocated ("empty, cleaned").
  - `EMPTY_ASSIGNED` (`Green`): Allocated to patient, patient in transit from ED.
- **`EddConfidence`**: `HIGH`, `MEDIUM`, `LOW`.
- **`DischargeRunwayStage`**: `RUNWAY_D3`, `RUNWAY_D2`, `RUNWAY_D1`, `READY_FOR_MORNING_SIGNOFF`, `MEDICATIONS_PENDING`, `READY_TO_VACATE`, `VACATED`.
- **`MedicationDeliveryStatus`**: `NOT_DISPATCHED`, `PACKING_IN_PROGRESS`, `DELIVERED_BEDSIDE`.
- **`TurnoverSlaStatus`**: `ON_TRACK`, `APPROACHING_SLA`, `BREACHED`.

### 3. API Surface & REST Contracts

- `POST /api/v1/ward/patients/{patientId}/edd`: Sets or updates Estimated Date of Discharge and confidence rating.
- `GET /api/v1/ward/runway`: Retrieves active discharge runways across all wards with dynamic runway stages.
- `POST /api/v1/ward/patients/{patientId}/discharge-signoff`: Clinician executes final morning discharge sign-off; transitions readiness state and auto-queues discharge medications.
- `POST /api/v1/ward/patients/{patientId}/deliver-medication`: Confirms bedside delivery of discharge medications.
- `POST /api/v1/patients/beds/{bedId}/vacate`: Ward nurse marks patient vacated $\to$ bed transitions `OCCUPIED_TAKEN` (`Grey`) $\to$ `EMPTY_PENDING_CLEANING` (`Mustard Yellow`) and initiates 30-minute cleaning SLA countdown.
- `POST /api/v1/patients/beds/{bedId}/clean`: Housekeeping specialist signs off terminal cleaning $\to$ bed transitions `EMPTY_PENDING_CLEANING` (`Mustard Yellow`) $\to$ `EMPTY_CLEANED` (`White`).

```typescript
// Core Data Contracts (Prototype-Verified Shape)
interface DischargeRunwayDto {
  patientId: string;
  patientName: string;
  bedNumber: string;
  wardCode: string;
  levelNumber: number;
  estimatedDateOfDischarge?: string;
  confidence?: 'HIGH' | 'MEDIUM' | 'LOW';
  runwayStage: 'RUNWAY_D3' | 'RUNWAY_D2' | 'RUNWAY_D1' | 'READY_FOR_SIGNOFF' | 'MEDICATIONS_PENDING' | 'READY_TO_VACATE';
  dischargeSignoffAt?: string;
  medicationStatus: 'NOT_DISPATCHED' | 'PACKING_IN_PROGRESS' | 'DELIVERED_BEDSIDE';
}

interface TurnoverTaskDto {
  bedId: string;
  bedNumber: string;
  wardCode: string;
  levelNumber: number;
  vacatedAt: string;
  cleaningStartedAt: string;
  remainingMinutes: number;
  slaStatus: 'ON_TRACK' | 'APPROACHING_SLA' | 'BREACHED';
}
```

### 4. Architectural Decisions (ADR Alignment)

- **ADR-001 (Spatial Hierarchy & Audit Integrity)**: Beds follow Level $\to$ Ward $\to$ Bed hierarchy with no cubicles. All vacate and clean events record timestamped audit entries with user principal context.
- **ADR-002 (Bed State Machine & Heuristic Recalculation)**: Vacated beds enter `EMPTY_PENDING_CLEANING` and are strictly excluded from BMU algorithms until housekeeping flips status to `EMPTY_CLEANED`. Once flipped, BMU allocation heuristics immediately include the bed in live candidate rankings.
- **ADR-003 (Polling & Immediate Cache Invalidation)**: Ward consoles and EVS queues utilize TanStack Query with active 3-second background polling (`refetchInterval: 3000`), supplemented by query invalidation upon mutate actions (`useVacatePatient`, `useCleanBed`).
- **ADR-004 (Strict Validation & RFC 7807)**: API endpoints validate requests using Jakarta Bean Validation. Calling `clean` on a bed that is not in `EMPTY_PENDING_CLEANING` returns HTTP 400 Bad Request with Problem Details.
- **ADR-005 & ADR-006 (Persona Switching & Audit Trails)**: Evaluators can simulate ward clinicians (`nurse_sarah`, `doctor_chen`) and housekeeping specialists (`evs_cleaner_ali`) with zero login friction via topbar role switching, emitting structured SLF4J audit events (`VACATE_PATIENT`, `CLEAN_BED`, `DISCHARGE_SIGNOFF`, `DISPENSE_MEDICATION`, `DELIVER_BEDSIDE_MEDICATION`).

---

## KPI Instrumentation & Extraction Recommendations

All Epic 4 discharge runway, pharmacy bedside delivery, and bed turnover KPIs are instrumented via relational entity timestamps and structured SLF4J audit logs emitted through `AuditLogger`.

### 1. Structured Audit Log Events

- `RECORD_EDD`:
  - `target`: `AdmissionRequest:{id}`
  - `details`: `PatientId={patientId}, EDD={date}, Confidence={HIGH|MEDIUM|LOW}, RunwayStage={stage}`
- `DISCHARGE_SIGNOFF`:
  - `target`: `AdmissionRequest:{id}`
  - `details`: `DoctorId={doctorId}, SignOffTime={time}, PreDischargeHour={hour}`
- `DISPENSE_MEDICATION`:
  - `target`: `AdmissionRequest:{id}`
  - `details`: `PatientId={id}, WardBed={bedNumber}, TargetSla="11:00 AM"`
- `DELIVER_BEDSIDE_MEDICATION`:
  - `target`: `AdmissionRequest:{id}`
  - `details`: `PatientId={id}, BedId={bedId}, ConfirmedBy={userId}, DeliveryTime={time}`
- `VACATE_PATIENT`:
  - `target`: `Bed:{bedId}`
  - `details`: `BedNumber={number}, VacateTimestamp={time}, VacateHour={hour}, DischargedBeforeNoon={true|false}`
- `CLEAN_BED`:
  - `target`: `Bed:{bedId}`
  - `details`: `BedNumber={number}, HousekeeperId={cleanerId}, ElapsedCleaningMins={mins}, Within30mSla={true|false}`

### 2. Database Schema Audit Fields

- `admission_requests`: `admitted_at`, `discharged_at`, `edd`, `edd_confidence`, `discharge_signoff_at`, `medication_delivery_status`
- `beds`: `cleaning_started_at`, `last_cleaned_at`, `status`

### 3. Metric Computation Recipes

#### KPI 19: Discharge Before 12:00 PM
- **SQL Extraction**:
  ```sql
  SELECT 
    COUNT(*) AS total_discharges,
    SUM(CASE WHEN EXTRACT(HOUR FROM discharged_at) < 12 THEN 1 ELSE 0 END) AS discharged_before_noon_count,
    ROUND(100.0 * SUM(CASE WHEN EXTRACT(HOUR FROM discharged_at) < 12 THEN 1 ELSE 0 END) / COUNT(*), 2) AS discharge_before_noon_pct
  FROM admission_requests
  WHERE discharged_at IS NOT NULL;
  ```
- **Log Script (Bash / jq)**:
  ```bash
  total=$(grep -c 'action="VACATE_PATIENT"' application.log)
  before_noon=$(grep 'action="VACATE_PATIENT"' application.log | grep -c 'DischargedBeforeNoon=true')
  echo "scale=2; ($before_noon / $total) * 100" | bc | awk '{print "Discharge Before 12:00 PM Rate: " $1 "%"}'
  ```

#### KPI 20: Advance Runway Establishment Rate (D-2 / D-3 EDD Recorded)
- **SQL Extraction**:
  ```sql
  SELECT 
    COUNT(*) AS total_admitted_patients,
    SUM(CASE WHEN edd IS NOT NULL THEN 1 ELSE 0 END) AS edd_recorded_count,
    ROUND(100.0 * SUM(CASE WHEN edd IS NOT NULL THEN 1 ELSE 0 END) / COUNT(*), 2) AS edd_advance_rate_pct
  FROM admission_requests
  WHERE status IN ('ADMITTED_INPATIENT', 'DISCHARGED');
  ```

#### KPI 21: Bedside Discharge Medication Delivery Adoption
- **SQL Extraction**:
  ```sql
  SELECT 
    COUNT(*) AS total_discharge_prescriptions,
    SUM(CASE WHEN status = 'DELIVERED_BEDSIDE' THEN 1 ELSE 0 END) AS bedside_delivered_count,
    ROUND(100.0 * SUM(CASE WHEN status = 'DELIVERED_BEDSIDE' THEN 1 ELSE 0 END) / COUNT(*), 2) AS bedside_adoption_rate_pct
  FROM medication_orders;
  ```
- **Log Script (Bash / grep)**:
  ```bash
  queued=$(grep -c 'action="DISPENSE_MEDICATION"' application.log)
  delivered=$(grep -c 'action="DELIVER_BEDSIDE_MEDICATION"' application.log)
  echo "scale=2; ($delivered / $queued) * 100" | bc | awk '{print "Bedside Med Delivery Adoption: " $1 "%"}'
  ```

#### KPI 22: Bed Turnover Cleaning Latency (<30 mins SLA)
- **SQL Extraction**:
  ```sql
  SELECT 
    COUNT(*) AS total_turnovers,
    AVG(EXTRACT(EPOCH FROM (last_cleaned_at - cleaning_started_at)) / 60.0) AS avg_turnover_mins,
    SUM(CASE WHEN EXTRACT(EPOCH FROM (last_cleaned_at - cleaning_started_at)) / 60.0 <= 30.0 THEN 1 ELSE 0 END) AS within_30m_sla_count,
    ROUND(100.0 * SUM(CASE WHEN EXTRACT(EPOCH FROM (last_cleaned_at - cleaning_started_at)) / 60.0 <= 30.0 THEN 1 ELSE 0 END) / COUNT(*), 2) AS sla_compliance_pct
  FROM beds
  WHERE last_cleaned_at IS NOT NULL AND cleaning_started_at IS NOT NULL;
  ```
- **Log Script (Bash / jq)**:
  ```bash
  grep 'action="CLEAN_BED"' application.log | \
    sed -n 's/.*ElapsedCleaningMins=\([0-9.]*\).*/\1/p' | \
    awk '{sum+=$1; count++; if($1<=30.0) under_sla++} END {
      print "Average Cleaning Latency:", sum/count, "mins";
      print "30-min SLA Compliance:", (under_sla/count)*100, "%";
    }'
  ```

---

## Testing Decisions

### What Makes a Good Test

Tests must verify external observable behavior, domain state invariants, and HTTP contracts rather than internal database queries:

- **State Machine Enforcement**:
  - Calling `vacate` on an `OCCUPIED_TAKEN` bed must transition it to `EMPTY_PENDING_CLEANING` and link admission request status to `DISCHARGED`.
  - Calling `clean` on an `EMPTY_PENDING_CLEANING` bed must transition it to `EMPTY_CLEANED` and set `currentPatient` to `null`.
  - Attempting to clean a bed that is already `EMPTY_CLEANED` or `OCCUPIED_TAKEN` must be rejected with an appropriate error.
- **Discharge Runway Calculations**: Setting an EDD 2 days out must calculate `RUNWAY_D2` status and verify checklist completion.
- **Pharmacy Queue Dispatch**: Morning physician discharge sign-off must create an active dispensing order with delivery SLA target before 11:00 AM.
- **BMU Re-inclusion**: Once a bed transitions to `EMPTY_CLEANED`, verify via BMU recommendation queries that the bed is immediately scored and available for candidate allocation.

### Tested Modules & Seams

1. **Primary End-to-End API Seam (`PatientTrackerController` / `WardController` via `MockMvc`)**:
   - Tests HTTP POST `/api/v1/patients/beds/{bedId}/vacate` and `/clean`, response bodies, and bean validations.
2. **Ward & Turnover Service Seam (`PatientTrackerService` / `WardService`)**:
   - Tests business logic for bed state transitions, patient dissociation, discharge timestamping, and SLA countdown calculation.
3. **Audit Trail Seam (`AuditLogger`)**:
   - Verifies that security principal names and action tags (`VACATE_PATIENT`, `CLEAN_BED`, `DISCHARGE_SIGNOFF`) are logged with structured attributes.

### Prior Art

- `backend/src/test/java/com/hospital/admissions/service/PatientTrackerServiceTest.java`: Demonstrates service-level unit tests for bed check-in, vacate, and clean transitions.
- `backend/src/test/java/com/hospital/admissions/web/PatientTrackerControllerTest.java`: Demonstrates web-tier MockMvc testing for vacate and clean endpoints.
- `backend/src/test/java/com/hospital/admissions/TracerBulletsIntegrationTest.java`: Demonstrates full closed-loop vertical slice from ED assessment to BMU bed allocation, ward check-in, nurse vacate, and housekeeping clean sign-off.

---

## Out of Scope

1. **Long-Term Outpatient Care Management**: Follow-up clinic appointment scheduling, polyclinic transfer notes, and outpatient chronic illness surveillance.
2. **Physical Hardware & Robotics Automation**: Automated pharmacy dispensing robots, automated guided vehicles (AGVs) for medication transport, or pneumatic tube logistics.
3. **Commercial Fleet Transportation APIs**: Direct API integrations with third-party wheelchair taxi fleets or private ambulance dispatchers.
4. **ED Clinical Triage & Specialist Broadcast**: Primary intake forms, diagnostic pre-population, and specialist claiming belong to Epic 1.
5. **BMU Constraint Satisfaction Bed Allocation**: Multi-constraint scoring, holding ward batching, and cohort swaps belong to Epic 2.
6. **Public Patient Mobile Journey Tracker**: Patient-facing milestone screens, wait-time estimations, and financial advisory cards belong to Epic 3.

---

## Further Notes

- **The Closed-Loop Bed Lifecycle**: Epic 4 completes the hospital's circular capacity lifecycle. By accelerating pre-discharge preparation and enforcing 30-minute cleaning turnaround, Epic 4 creates the available `White` beds required by Epic 2's allocation engine to resolve Epic 1's emergency admissions, while advancing Epic 3's public milestone tracker to completion.
