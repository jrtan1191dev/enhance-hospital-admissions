# 004: API Surface & Contract Design (REST Endpoints)

- **Type**: `wayfinder:grilling`
- **Status**: `closed`
- **Assignee**: `antigravity`
- **Blocked by**: none (previously 001, 003 — both closed)
- **Blocks**: [005-tracer-bullet-1-ed-to-bmu-architecture.md](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/005-tracer-bullet-1-ed-to-bmu-architecture.md)

## Question

What are the explicit RESTful endpoints, DTO payloads, and status codes for:
1. ED Assessment & Broadcast: `POST /api/assessments/primary`, `GET /api/broadcasts/feed`, `POST /api/broadcasts/{id}/claim`, `POST /api/assessments/consult`
2. BMU Allocation & Batching: `GET /api/bmu/queue`, `GET /api/bmu/beds/recommendations/{requestId}`, `POST /api/bmu/allocations/approve`, `POST /api/bmu/batch-holding-rooms/approve`, `POST /api/bmu/cohort-swap`
3. Patient Milestone Tracker: `GET /api/public/queue-status/{token}`
4. Inpatient & Turnover: `POST /api/inpatient/vacate`, `POST /api/housekeeping/signoff`

## Resolution (ADR-004: Three-Controller REST Surface with Native Spring Web Defaults)

### 1. Architectural Design Principles
- **Minimal Controller Footprint**: Exactly **3 Spring Web `@RestController` classes** across the entire prototype.
- **Native Spring Web Standards**: Direct domain DTOs (Java records/POJOs) returned directly in `ResponseEntity<T>`, leveraging Spring's built-in Jackson serialization and default `ProblemDetail` (RFC 7807) error handling. Zero third-party envelope wrappers.

---

### 2. The Three Controller Contracts

#### Controller 1: `ClinicianController` (`/api/clinicians`)
Handles all clinical workflows across ED triage, specialist broadcast, and ward execution:
- `GET /api/clinicians/ed/patients`: List of waiting ED patients available for assessment.
- `GET /api/clinicians/ed/assessments/prepopulate/{patientId}`: Returns synthesized diagnostic baseline, suspected diagnosis, and recommended acuity tier.
- `POST /api/clinicians/ed/assessments/submit`: ED attending confirms primary assessment (with optional chip tweaks) $\rightarrow$ creates `AdmissionRequest` and triggers broadcast. Returns HTTP 201.
- `GET /api/clinicians/specialist/broadcasts`: On-call feed filtered by `serviceCluster`.
- `POST /api/clinicians/specialist/broadcasts/{id}/claim`: On-call specialist claims a case. Returns HTTP 409 if already claimed.
- `POST /api/clinicians/specialist/broadcasts/{id}/consult`: Specialist submits consult impression, acuity, and diversion endorsement. Auto-updates discordance flag.
- `POST /api/clinicians/ward/receive`: Ward nurse checks in arriving patient $\rightarrow$ bed flips `EMPTY_ASSIGNED` (`GREEN`) $\rightarrow$ `OCCUPIED_TAKEN` (`GREY`).
- `POST /api/clinicians/ward/vacate`: Ward nurse marks patient discharged $\rightarrow$ bed flips `OCCUPIED_TAKEN` (`GREY`) $\rightarrow$ `EMPTY_PENDING_CLEANING`.

#### Controller 2: `BmuController` (`/api/bmu`)
Handles bed management, allocation heuristics, dynamic batching, and housekeeping sign-off:
- `GET /api/bmu/queue`: Prioritized admission request queue (sorted by Acuity Severity > arrival time).
- `GET /api/bmu/wards`: Complete hospital ward inventory (Level $\rightarrow$ Ward $\rightarrow$ Beds with statuses `EMPTY_CLEANED`, `EMPTY_PENDING_CLEANING`, `EMPTY_ASSIGNED`, `OCCUPIED_TAKEN`).
- `GET /api/bmu/recommendations/{requestId}`: Synchronously computes hard filters and soft scoring to return **Top 3 Recommended Beds**.
- `POST /api/bmu/allocations/approve`: BMU coordinator 1-click approves bed assignment $\rightarrow$ bed flips to `EMPTY_ASSIGNED` (`GREEN`).
- `POST /api/bmu/allocations/override`: Coordinator overrides recommendation with mandatory structured reason code.
- `GET /api/bmu/batch-suggestions`: Scans queue for clusters ($\ge 3$ patients) matching $\langle \text{WardClass}, \text{Gender}, \text{Infection} \rangle$ and candidate flex wards.
- `POST /api/bmu/batch-holding-wards/approve`: 1-click approves batch holding ward allocation $\rightarrow$ batch porters dispatched.
- `GET /api/bmu/cohort-swap-suggestions`: Detects isolated `EMPTY_ASSIGNED` beds blocking flex wards during surge conditions.
- `POST /api/bmu/cohort-swap/approve`: Executes cohort swap re-allocation.
- `POST /api/bmu/beds/{bedId}/clean`: Housekeeping marks sanitization complete $\rightarrow$ bed flips `EMPTY_PENDING_CLEANING` $\rightarrow$ `EMPTY_CLEANED` (`WHITE`).
- `GET /api/bmu/config` & `PUT /api/bmu/config`: View/update algorithm scoring weights via BMU Configuration Portal.

#### Controller 3: `PatientTrackerController` (`/api/patients`)
Dedicated consumer portal for patients and authorized family members:
- `GET /api/patients/tracker/{token}`: Token-based secure public tracking endpoint returning:
  - Patient masked name (e.g. "Tan * * H * *")
  - Milestone stage (1: Confirmed/Queued, 2: Matching Ward, 3: Room Sanitization, 4: Transfer in Progress)
  - Estimated wait duration in minutes
  - Number of patients ahead in matching ward category
  - Transparent nurse/BMU delay reason disclosure (if tagged)
  - FYI Financial & Care Explainer data with direct Medical Social Work (MSW) hotlines.
