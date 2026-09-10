# Intelligent Patient Flow & Bed Capacity Orchestration System

<!-- ============================================================
     LIVE DEMO URL — Update this single line when the URL changes
     ============================================================ -->
[LIVE_DEMO_URL]: https://enhance-hospital-admissions.onrender.com

> 🌐 **[Live Demo →][LIVE_DEMO_URL]**
>
> *The application may take a moment to wake on first load (cold start). A GraalVM native image build is planned to eliminate this.*

---

## Screenshots

<!-- Replace these placeholders with actual screenshots -->

| ED Clinician — Diagnostic Assessment | BMU Coordinator — Bed Allocation | Patient Milestone Tracker |
|:---:|:---:|:---:|
| *<!-- ![ED Clinician View](docs/screenshots/ed-clinician.png) -->* | *<!-- ![BMU Dashboard](docs/screenshots/bmu-dashboard.png) -->* | *<!-- ![Patient Tracker](docs/screenshots/patient-tracker.png) -->* |
| Pre-populated diagnostic synthesis from clinical data. The ED attending reviews AI-suggested acuity tier, specialty, and care requirements — then confirms with 1-click or adjusts via dropdown chips. | Severity-prioritized bed queue with Top-3 recommendations showing constraint match rationale. BMU coordinators approve allocations with 1-click or override with a mandatory structured reason. | Mobile-optimised 3-stage milestone stepper showing queue position partitioned by ward class, estimated wait duration, and empathetic operational delay explanations. |

---

## The Problem

Having accompanied family members through Singapore's public hospital ED admission process multiple times — experiencing the hours-long waits with zero queue visibility, the phone-tag between ED and BMU staff, and the afternoon discharge gridlocks — I set out to understand the systemic root causes.

I mapped the actual patient journey and stakeholder pain points across each persona (ED attending, specialist, BMU coordinator, ward nurse, patient/family), informed by personal experience and [public discourse documenting these persistent issues](https://youtu.be/tQ_zwPpyn6c?si=gB8O5wNbTb_8Rnfl). This analysis identified **6 core pain points** — from multi-doctor admission decision latency to discharge medication bottlenecks — which became the foundation for this system.

---

## How I Approached It

This project follows a **spec-driven development methodology** — every line of code traces back to a documented decision. No code was written until the problem was understood, the users stories were specified, and the architecture was decided.

```
Personal Experience & Domain Research
        │
        ▼
┌─────────────────────────────┐
│  Pain Point Analysis        │──── product-idea/pain-points.md
│  (6 systemic root causes)   │     Root cause analysis with system solutions
└─────────────┬───────────────┘
              ▼
┌─────────────────────────────┐
│  User Stories & Acceptance  │──── product-idea/user-stories.md
│  Criteria (Gherkin)         │     4 Epics, 15 Features, 30+ Stories
└─────────────┬───────────────┘
              ▼
┌─────────────────────────────┐
│  Technical Specifications   │──── specs/
│  (per Epic)                 │     5 detailed spec documents
└─────────────┬───────────────┘
              ▼
┌─────────────────────────────┐
│  Architecture Decisions     │──── docs/adr/ + .wayfinder/tickets/
│  (ADRs + Design Tickets)    │     4 ADRs + 11 design tickets
└─────────────┬───────────────┘
              ▼
┌─────────────────────────────┐
│  Implementation             │──── backend/ + frontend/
│  (Spring Boot + React)      │     >90% test coverage
└─────────────────────────────┘
```

This workflow demonstrates how I'd set up a new product team: establish domain understanding first, codify engineering standards into reusable guidelines, create specification-driven delivery pipelines, and prioritize vertical slices for early architectural validation.

---

## Repository Navigation

### Primary Content

| Folder | What It Contains | Start Here |
|--------|-----------------|------------|
| [`product-idea/`](product-idea/) | Pain point analysis mapping 6 root causes to system solutions, and user stories with Gherkin acceptance criteria across 4 Epics | [pain-points.md](product-idea/pain-points.md) |
| [`specs/`](specs/) | Detailed technical specifications per epic — data models, API contracts, state machines, and KPI instrumentation | [Epic 1 Spec](specs/epic-1-ed-clinical-admission-and-broadcast.md) |
| [`docs/adr/`](docs/adr/) | Architecture Decision Records for domain-critical design choices | [ADR-0007](docs/adr/0007-consult-gated-bed-allocation.md) |
| [`backend/`](backend/) | Spring Boot 4.1 (Java 25) API — domain services, heuristic constraint solver, structured audit logging, REST controllers | [pom.xml](backend/pom.xml) |
| [`frontend/`](frontend/) | React 19 (TypeScript) SPA — TanStack Router, role-based views, shadcn/ui components, Tailwind CSS 4 | [package.json](frontend/package.json) |
| [`docs/`](docs/) | Architecture deep-dive, development guide, testing strategy, production roadmap | [architecture.md](docs/architecture.md) |

<details>
<summary><strong>Process & Tooling Directories</strong></summary>

| Folder | What It Contains |
|--------|-----------------|
| [`.wayfinder/`](.wayfinder/) | Architectural roadmap ([map.md](.wayfinder/map.md)) + 11 design decision tickets documenting prototype vs production boundaries |
| [`.scratch/`](.scratch/) | 28 atomic implementation issues across all 5 epics with detailed acceptance criteria |
| [`.agents/`](.agents/) | AI agent skills — reusable engineering standards ([write-minimum](.agents/skills/write-minimum/SKILL.md)), automated code review pipelines, IM8 compliance checks |
| [`scripts/`](scripts/) | KPI extraction CLI scripts — dual-pathway verification (SQL vs structured log parsing) |

</details>

---

## Architecture Summary

The system uses a **Spring Boot 4.1 + React 19** monorepo deployed as a single JAR. The architecture is **production-ready by default** — when no Spring profile is active, default beans connect to real PostgreSQL databases, FHIR EHR endpoints, and external hospital APIs. All prototype simplifications are strictly isolated under `@Profile("prototype")`.

**Core architectural patterns:**
- **Gateway / Adapter Pattern** — EHR integration, sister hospital transfers, and constraint solvers are behind swappable interfaces, enabling mock implementations for prototyping and real adapters for production
- **Two-Tier Constraint Hierarchy** — Absolute safety invariants (gender cohorting, airborne isolation) are non-overridable; operational constraints (ward class, portable telemetry) permit coordinator override with mandatory institutional reason codes
- **Safety-First Discordance Engine** — When ED attending and specialist disagree on acuity, the system automatically escalates to the higher tier and unions telemetry requirements
- **Dual-Pathway Observability** — Every operational KPI is extractable via both SQL queries (for BI dashboards) and structured log parsing (for real-time SIEM alerting)

**3 API Controllers, 0 phone calls:**
- `ClinicianController` — ED assessment, specialist broadcast, consult evaluation
- `BmuController` — Bed allocation, constraint validation, diversion routing
- `PatientTrackerController` — Public milestone tracking, financial explainer, support hotlines

> 📐 **[Full Architecture Deep Dive →](docs/architecture.md)** — 10 architectural dimensions with prototype vs production comparisons
>
> 🗺️ **[Production Roadmap →](docs/production-roadmap.md)** — Reliability, observability, security, and integration targets (already designed in `.wayfinder/tickets/`)

---

## Testing & Quality

Testing follows a **deliberate pyramid strategy**: >90% line coverage across both frontend (Vitest + V8) and backend (JUnit 5 + JaCoCo), concentrated at the unit and integration test layers where domain invariants provide the highest confidence-per-test.

**What's tested at the domain layer:**
- Bed state machine transitions (White → Green → Grey → Mustard Yellow → White)
- Constraint satisfaction engine (hard invariant rejection, soft constraint scoring)
- Consensus completion gate (all specialist broadcasts must complete before BMU dispatch)
- Safety-first discordance resolution (highest acuity, telemetry union)
- Dual-pathway KPI metric extraction (SQL and log parity verification)
- Optimistic locking (concurrent specialist claim → HTTP 409 Conflict)

E2E browser tests are deferred as a production investment — in the prototype, authentication is header-driven and data is synthetic, making the domain logic layer the highest-value test target.

> 🧪 **[Full Testing Strategy →](docs/testing-strategy.md)** — Test pyramid rationale, coverage reports, and what's covered at each layer

---

## Running Locally

### Backend (Spring Boot)

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=prototype
```

Starts on `http://localhost:8080` with H2 in-memory database, seeded patients (P101–P104), and mock gateways.

### Frontend (Vite Dev Server)

```bash
cd frontend
npm install && npm run dev
```

Starts on `http://localhost:5173` with hot-reload.

### Single JAR (Full Build)

```bash
cd backend
./mvnw clean package -DskipTests
java -Dspring.profiles.active=prototype -jar target/admissions-0.0.1-SNAPSHOT.jar
```

> 📖 **[Full Development Guide →](docs/development-guide.md)** — Persona accounts, demo walkthrough, KPI dashboard, and end-to-end flow

---

## Design Philosophy — Write Minimum

Every line of custom code is a liability. I follow a strict **6-tier decision ladder** that prioritizes eliminating unnecessary code before writing any:

1. **Don't write it** (YAGNI) → 2. **Reuse existing code** → 3. **Use framework built-ins** → 4. **Use installed libraries** → 5. **Evaluate external libraries** → 6. **Write custom code (last resort)**

This philosophy is codified in a [reusable engineering standard](.agents/skills/write-minimum/SKILL.md) that governs all code in this project. The result:

- **3 controllers** — not 30. Consolidated API surface using native Spring ProblemDetail error handling
- **0 custom UI primitives** — shadcn/ui + Base UI provide all components
- **0 hand-rolled HTTP/CSRF/cache handling** — framework defaults and TanStack Query handle everything
- **No single-implementation interfaces** — concrete service classes directly, not `FooService` / `FooServiceImpl`

---

## AI-Augmented Development

I used AI coding agents as a development accelerator throughout this project. The `.agents/` directory contains the evidence — and I want to be transparent about how.

**What I directed:**
- The pain point analysis came from my personal experience and domain research
- The user stories, acceptance criteria, and architectural decisions reflect my product and technical judgment
- The spec-driven workflow, quality gates, and engineering standards are my process design

**What AI accelerated:**
- Code generation from specifications I wrote
- Automated compliance reviews against the standards I defined
- Test generation aligned with acceptance criteria I specified

**Why this matters for a TLM role:** A tech lead who can leverage AI agents to multiply a small team's output — while maintaining architectural control and quality standards — is how modern engineering teams ship fast without sacrificing reliability. The reusable skills I built ([write-minimum](.agents/skills/write-minimum/SKILL.md), [code-review](.agents/skills/code-review/SKILL.md), [im8-review](.agents/skills/im8-review/SKILL.md)) are the kind of engineering process infrastructure a TLM builds for their team.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Spring Boot 4.1, Java 25, Spring Security, Spring Data JPA, Bean Validation, Lombok |
| **Frontend** | React 19, TypeScript 6, TanStack Router + Query + Table, shadcn/ui, Tailwind CSS 4, Vite 8 |
| **Testing** | JUnit 5, JaCoCo, Vitest 5, Testing Library, happy-dom |
| **Database** | H2 (prototype), PostgreSQL (production) |
| **Deployment** | Docker (multi-stage), Render |
| **Linting** | oxlint (frontend) |
