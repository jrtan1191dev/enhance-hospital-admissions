# 005: MVP Tracer Bullet Vertical Slice 1 Architecture (ED to BMU Allocation)

- **Type**: `wayfinder:prototype`
- **Status**: `closed`
- **Assignee**: `antigravity`
- **Blocked by**: none (previously 001, 002, 004 — all closed)
- **Blocks**: [006-tracer-bullet-2-patient-tracker-and-turnover-loop.md](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/006-tracer-bullet-2-patient-tracker-and-turnover-loop.md)

## Question

What is the bare-minimum executable code skeleton and UI stub structure connecting React to Spring Boot for Tracer Bullet 1?

1. Seed dataset: Synthetic ward layout (Level 8 Ward 8A, Level 8 Ward 8B with `Mustard Yellow`/`White`/`Green`/`Grey` beds) and simulated waiting ED patients.
2. ED Attending UI: 1-click submit pre-populated assessment.
3. Inpatient Specialist UI: Service-cluster on-call consult feed view.
4. BMU Dashboard UI: Live queue, Top 3 bed recommendation card, and 1-click approval transitioning bed `White` $\rightarrow$ `Green` $\rightarrow$ `Grey` $\rightarrow$ `Mustard Yellow` $\rightarrow$ `White`.

## Resolution (ADR-005: MVP Tracer Bullet Vertical Slice 1 Architecture)

### 1. Monorepo Skeleton & Technology Selection

- **Backend**: Spring Boot 3 (Java 17/21) with Maven, Spring Web, Spring Data JPA, H2 in-memory DB, and Lombok.
- **Spring Profile Strategy (Production-Ready Default vs Prototype Profile Isolation)**:
  - The application is **production-ready by default**. When no profile is specified (or running in standard production environments), default beans connect to enterprise persistent RDBMS, actual hospital EHR feeds, and real external Sister Hospital API adapters.
  - All prototype decisions and constraints discussed up to now are **strictly tied to the `prototype` profile only** (`@Profile("prototype")`).
  - Active profile for local evaluation is `prototype` (`spring.profiles.active=prototype`), which leverages Spring's Dependency Injection to activate in-memory H2, synthetic `DataInitializer` seeds, and mock external integration gateways without altering core business logic or domain services.
  - Other environments such as `dev` and `qa` are distinct profiles reserved for future configuration and are strictly decoupled from the prototype profile.
- **Frontend**: React 18, Vite, TypeScript, Tailwind CSS, TanStack Query, and **TanStack Router (`@tanstack/react-router`)**.
- **Role Switching & Access Matrix**:
  - Persistent **Topbar Role-Switcher**: `[🩺 ED Attending]` | `[👨‍⚕️ Inpatient Specialist]` | `[🏢 BMU Coordinator]` | `[📱 Patient Admission Tracker]` | `[🧹 Ward & EVS]`.
  - No authentication/login overhead required for prototype evaluation.
  - **TanStack Router route guards**: The active role context in state guards access matrix permissions for routes (`/ed`, `/specialist`, `/bmu`, `/patient`, `/ward`), automatically redirecting to authorized views upon role switch.

---

### 2. Concrete Seed Dataset (`DataInitializer` under `@Profile("prototype")`)

Populates on boot only when the `prototype` profile is active:

- **Level 8 Ward 8A (Cardiology / Class B2 / Locked Male)**:
  - Bed 8A-01: `OCCUPIED_TAKEN` (`GREY`, Male)
  - Bed 8A-02: `OCCUPIED_TAKEN` (`GREY`, Male)
  - Bed 8A-03: `EMPTY_ASSIGNED` (`GREEN`, Male, awaiting porter)
  - Bed 8A-04: `EMPTY_CLEANED` (`WHITE` [empty, cleaned], Telemetry enabled) $\leftarrow$ Candidate target for Tan Ah Meng
  - Bed 8A-05: `EMPTY_PENDING_CLEANING` (`MUSTARD YELLOW` - vacated, empty, pending cleaning)
- **Level 8 Ward 8B (Holding Ward / Class B2 / Flex Unlocked)**:
  - 4 beds (8B-01, 8B-02, 8B-03, 8B-04): all `EMPTY_CLEANED` (`WHITE` [empty, cleaned]) $\leftarrow$ Candidate target for dynamic batch holding ward
- **Level 9 Ward 9A (General Medicine / Class C / Locked Female)**:
  - Beds 9A-01 to 9A-06: 4 `OCCUPIED_TAKEN` (`GREY`), 2 `EMPTY_CLEANED` (`WHITE` [empty, cleaned])
- **Simulated Waiting ED Patients**:
  - `P101` ("Tan Ah Meng", Male, 68): NSTEMI, Troponin 150 ng/L, Telemetry required, Ward Class B2
  - `P102` ("Siti Rahmah", Female, 55): Chest pain, Ward Class B2
  - `P103` ("Kowsalya", Female, 62): Respiratory non-infectious, Ward Class B2
  - `P104` ("Mdm Lee", Female, 71): Respiratory non-infectious, Ward Class B2 $\rightarrow$ Forms a 3-patient cluster with P102 and P103 for holding ward batching!

---

### 3. Vertical Slice 1 End-to-End Test Workflow

1. **ED Attending View (`/ed`)**:
   - Opens patient Tan Ah Meng (`P101`).
   - Reviews pre-populated assessment (Tier 2, Cardiology, B2, Telemetry).
   - Clicks "Confirm & Lead Assessment" in 1 click $\rightarrow$ calls `POST /api/clinicians/ed/assessments/submit`.
   - Result: Admission request dispatched to BMU queue; broadcast published to Cardiology feed.
2. **Inpatient Specialist View (`/specialist`)**:
   - Cardiology feed displays `P101`.
   - Specialist clicks "Claim Case" $\rightarrow$ calls `POST /api/clinicians/specialist/broadcasts/{id}/claim`.
   - Specialist enters consult note ("Cath lab planned, Tier 2 telemetry confirmed") and submits $\rightarrow$ calls `POST /api/clinicians/specialist/broadcasts/{id}/consult`.
3. **BMU Coordinator View (`/bmu`)**:
   - Queue table displays `P101` at top priority (Tier 2).
   - Selecting `P101` calls `GET /api/bmu/recommendations/{id}` $\rightarrow$ returns Top 3 beds (Bed 8A-04 ranked #1: +40 specialty, +30 consolidation, telemetry enabled).
   - Coordinator clicks "Approve Bed 8A-04" $\rightarrow$ calls `POST /api/bmu/allocations/approve`.
   - Result: Bed 8A-04 instantly flips from `EMPTY_CLEANED` (`WHITE` [empty, cleaned]) $\rightarrow$ `EMPTY_ASSIGNED` (`GREEN`).
   - BMU dashboard also displays "Batch Holding Ward Suggestion" for the 3 waiting female patients (P102, P103, P104) targeting Ward 8B.

---

### 4. Tracer Bullet 1 KPI Verification & Measurement Trace

Executing Tracer Bullet 1 emits and verifies the initial pipeline of operational KPIs:
- **Primary ED Assessment Turnaround**: Emits `SUBMIT_ED_ASSESSMENT` with `ElapsedMins`.
- **Specialist Pick-up Latency**: Emits `CLAIM_BROADCAST` and `SUBMIT_SPECIALIST_CONSULT`.
- **Primary vs Specialist Concordance Rate**: Validated when Dr. Lim confirms Dr. Tan's Tier 2 acuity.
- **BMU Suggestion Acceptance Rate**: Emits `ALLOCATE_BED` with `Override=false`.

