# Development Guide

## Prerequisites

- Java 25 (JDK)
- Node.js v24.21+ (auto-installed by frontend-maven-plugin if building via Maven)
- Maven (wrapper included: `./mvnw`)

## Quick Start — Running Separately (Development)

### Backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=prototype
```

The backend starts on `http://localhost:8080` with:

- In-memory H2 database (auto-seeded with wards 8A, 8B, 9A and patients P101-P104)
- Mock EHR gateway (synthetic clinical baselines)
- Mock Sister Hospital gateway (simulated transfer acceptance)
- Prototype security filter (header-driven persona switching)
- H2 Console at `/h2-console`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on `http://localhost:5173` with Vite hot-reload.

## Quick Start — Single JAR (Production Build)

The Maven build uses `frontend-maven-plugin` to compile the React frontend into static assets bundled inside the Spring Boot JAR:

```bash
cd backend
./mvnw clean package -DskipTests
java -Dspring.profiles.active=prototype -jar target/admissions-0.0.1-SNAPSHOT.jar
```

Access the full application at `http://localhost:8080`.

## Docker

```bash
docker build -t hospital-admissions .
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prototype hospital-admissions
```

The Dockerfile uses a multi-stage build: eclipse-temurin:25-jdk for building, eclipse-temurin:25-jre for runtime.

## Running Tests

### Backend Tests

```bash
cd backend
./mvnw test
```

JaCoCo coverage report generated at `backend/target/site/jacoco/index.html`.

### Frontend Tests

```bash
cd frontend
npm test
npm run test:coverage
```

V8 coverage report generated at `frontend/coverage/index.html`.

## Demo Walkthrough — End-to-End Flow

I've designed this 3-step tracer bullet flow to prove the full lifecycle of our application:

### Step 1: ED Clinician Initiates Admission (ED Attending Persona)

- I switch to the ED Attending persona via the topbar.
- I can view the awaiting assessment list with my pre-populated patients (P101-P104).
- I select a patient and review the AI-synthesized diagnostic baseline.
- I then choose Direct Admission (immediate BMU dispatch) or Consult-Gated Admission (broadcast to specialist pool).
- Finally, I confirm the assessment with a 1-click action.

### Step 2: Specialist Claims & BMU Allocates (Specialist + BMU Personas)

- Next, I switch to the Specialist persona and claim the broadcast from the consult feed.
- I submit the specialist evaluation (acuity tier, telemetry, diversion suitability).
- The system auto-resolves any discordance (safety-first: highest acuity wins).
- Then, I switch to the BMU Coordinator persona.
- I set the admitting specialty cluster.
- I can view the Top-3 bed recommendations along with their match rationale.
- I approve the optimal bed with 1-click (and watch the bed transition from White → Green).

### Step 3: Patient Tracks & Bed Turns Over (Patient + Nurse Personas)

- I click the Patient Admission Tracker link in the topbar.
- I view the 3-stage milestone stepper, queue position, and estimated wait time.
- Next, I switch to the Ward Nurse persona and check in the patient (bed transitions Green → Grey).
- Later, I mark the patient as vacated (bed transitions Grey → Mustard Yellow).
- Finally, I switch to Housekeeping and complete terminal cleaning within the 30-min SLA (bed transitions Mustard Yellow → White).
- The bed is now immediately available for the next patient in my BMU queue.

## Persona Accounts

Here is a list of the available personas in the prototype:

| Persona | User ID | Role |
| --------- | --------- | ------ |
| ED Attending | `dr_tan_ed` | ED_ATTENDING |
| Cardiology Specialist | `dr_lim_cardio` | SPECIALIST |
| General Medicine | `dr_tan_genmed` | SPECIALIST |
| General Surgery | `dr_kumar_surg` | SPECIALIST |
| Orthopaedics | `dr_lee_ortho` | SPECIALIST |
| BMU Coordinator | `bmu_coord_wong` | BMU_COORDINATOR |
| Ward Nurse | `nurse_sarah` | WARD_NURSE |
| Patient P101 | `patient_p101` | PATIENT |

## KPI Dashboard & Audit Logs

- I can access the Analytics dashboard via the BMU coordinator view.
- I've included CLI KPI extraction scripts: `scripts/kpi-extract-all.sh` parses structured `[AUDIT]` log lines.
- The system guarantees dual-pathway verification: my SQL queries and log parsing produce mathematically identical results.
