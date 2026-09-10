# Intelligent Patient Flow & Bed Capacity Orchestration System

<!-- ============================================================
     LIVE DEMO URL — Update this single line when the URL changes
     ============================================================ -->
[LIVE_PROTOTYPE_DEMO_URL]: https://enhance-hospital-admissions.onrender.com

> 🌐 **[Live Prototype Demo →][LIVE_PROTOTYPE_DEMO_URL]**
>
> *The application may take a moment to wake on first load (cold start, ~50seconds) as this prototype is a personal project and is deployed using free tier of https://www.render.com.*

---

## Screenshots

<!-- Replace these placeholders with actual screenshots -->

| ED Clinician — Diagnostic Assessment | BMU Coordinator — Bed Allocation | Patient Milestone Tracker |
| :---: | :---: | :---: |
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
| -------- | ----------------- | ------------ |
| [`product-idea/`](product-idea/) | Pain point analysis mapping 6 root causes to system solutions, and user stories with Gherkin acceptance criteria across 4 Epics | [pain-points.md](product-idea/pain-points.md) |
| [`specs/`](specs/) | Detailed technical specifications per epic — data models, API contracts, state machines, and KPI instrumentation | [Epic 1 Spec](specs/epic-1-ed-clinical-admission-and-broadcast.md) |
| [`docs/adr/`](docs/adr/) | Architecture Decision Records for domain-critical design choices | [ADR-0007](docs/adr/0007-consult-gated-bed-allocation.md) |
| [`backend/`](backend/) | Spring Boot 4.1 (Java 25) API — domain services, heuristic constraint solver, structured audit logging, REST controllers | [pom.xml](backend/pom.xml) |
| [`frontend/`](frontend/) | React 19 (TypeScript) SPA — TanStack Router, role-based views, shadcn/ui components, Tailwind CSS 4 | [package.json](frontend/package.json) |
| [`docs/`](docs/) | Architecture deep-dive, development guide, testing strategy, production roadmap | [architecture.md](docs/architecture.md) |

<details>
<summary><strong>Process & Tooling Directories</strong></summary>

| Folder | What It Contains |
| -------- | ----------------- |
| [`.wayfinder/`](.wayfinder/) | Architectural roadmap ([map.md](.wayfinder/map.md)) + 11 design decision tickets documenting prototype vs production boundaries |
| [`.scratch/`](.scratch/) | 28 atomic implementation issues across all 5 epics with detailed acceptance criteria |
| [`.agents/`](.agents/) | AI agent skills — reusable engineering standards ([write-minimum](.agents/skills/write-minimum/SKILL.md)), automated code review pipelines, IM8 compliance checks |
| [`scripts/`](scripts/) | KPI extraction CLI scripts — dual-pathway verification (SQL vs structured log parsing) |

</details>

---

## Architecture Summary

The system uses a **Spring Boot 4.1 + React 19** monorepo deployed as a single JAR. The architecture is **production-architected by design** — interfaces, domain logic, and profile boundaries are structured so that swapping in real adapters is additive integration work, not an architectural rewrite. All prototype simplifications are strictly isolated under `@Profile("prototype")`.

> [!IMPORTANT]
> **Prototype Boundaries:** This is a working prototype. Domain logic (constraint engine, state machines, safety invariants) is production-grade and tested. External integrations (EHR, authentication, database, real-time push) are stubbed behind gateway interfaces — the architectural seams are cut in the right places, but the production adapters behind them are intentional placeholders pending real infrastructure. See [What's Production-Grade Today](#whats-production-grade-today) and [What's Designed but Stubbed](#whats-designed-but-stubbed-for-production) below.

**Core architectural patterns:**

- **Gateway / Adapter Pattern** — EHR integration, sister hospital transfers, and constraint solvers are behind swappable interfaces. Prototype mocks are fully functional; production adapters are defined as interface contracts (stubbed) pending real integration endpoints. See [Implementation Status](#implementation-status) for details.
- **Two-Tier Constraint Hierarchy** `[IMPLEMENTED]` — Absolute safety invariants (gender cohorting, airborne isolation) are non-overridable; operational constraints (ward class, portable telemetry) permit coordinator override with mandatory institutional reason codes
- **Safety-First Discordance Engine** `[IMPLEMENTED]` — When ED attending and specialist disagree on acuity, the system automatically escalates to the higher tier and unions telemetry requirements
- **Dual-Pathway Observability** `[IMPLEMENTED]` — Every operational KPI is extractable via both SQL queries (for BI dashboards) and structured log parsing (for real-time SIEM alerting)

**3 clinical workflow controllers + 1 analytics dashboard — zero phone calls between ED, BMU, and ward staff:**

- `ClinicianController` — ED assessment, specialist broadcast, consult evaluation
- `BmuController` — Bed allocation, constraint validation, diversion routing
- `PatientTrackerController` — Public milestone tracking, financial explainer, support hotlines
- `KpiAnalyticsController` — 18 operational KPI metrics with dual-pathway extraction

### Implementation Status

The domain patterns (constraint hierarchy, discordance engine, consensus gate, observability) are **fully implemented and tested with >90% coverage**. The integration patterns (EHR, sister hospital, solver, auth) are **architecturally complete** — interfaces and mock adapters exist and run — with production adapters stubbed pending real infrastructure.

> 📐 **[Full Architecture Deep Dive →](docs/architecture.md)** — 10 architectural dimensions with prototype vs production comparisons
>
> 🗺️ **[Production Roadmap →](docs/production-roadmap.md)** — Reliability, observability, security, and integration targets

---

## What's Production-Grade Today

These components are **fully implemented, tested, and profile-independent** — identical logic runs regardless of whether the prototype or production profile is active:

- ✅ **Bed state machine** — 4-state lifecycle (White → Green → Grey → Mustard Yellow → White) with strict transition gates
- ✅ **Two-tier constraint hierarchy** — Absolute safety invariants (gender cohorting, airborne isolation) are non-overridable; operational constraints permit coordinator override with mandatory reason codes
- ✅ **Safety-first discordance engine** — Highest acuity wins, telemetry requirements are unioned
- ✅ **Consensus completion gate** — All specialist broadcasts must resolve before BMU dispatch
- ✅ **Optimistic locking** — `@Version` annotations prevent concurrent specialist claim races (HTTP 409 Conflict)
- ✅ **Structured audit log format** — Identical MDC context and log structure; only the log destination changes between prototype (stdout) and production (SIEM)
- ✅ **18 operational KPI calculations** — Computed via JPA queries in `KpiMetricsService`, verified for mathematical parity with structured log extraction
- ✅ **API contracts and error handling** — RFC 7807 `ProblemDetail` responses; identical contract regardless of profile
- ✅ **Heuristic constraint solver** — 4 hard constraints + 3 soft scoring rules, pure Java, <50ms synchronous evaluation

## What's Designed but Stubbed for Production

These components are **not implemented** — they are architectural placeholders demonstrating where production integration points would connect. The prototype deliberately uses simplified alternatives to keep the focus on domain logic validation. The interfaces and profile boundary are designed so that production work is additive — new adapter implementations behind existing interfaces, not architectural changes. Each adapter can be implemented independently without touching domain logic or other adapters.

| Component | Prototype Simplified Alternative | Production Target (Designed, Not Implemented) | Relevant Ticket |
| --- | --- | --- | --- |
| **Hospital EHR** | `MockHospitalEhrGateway` — synthetic data for P101–P104 | `FhirHospitalEhrGateway` — HAPI FHIR R4 client, mTLS, SMART on FHIR `[STUBBED]` | [Ticket 008](.wayfinder/tickets/008-live-hospital-ehr-fhir-ingestion-pipeline.md) |
| **Sister Hospital Transfers** | `MockSisterHospitalGateway` — simulated 30-min SLA acceptance | `HttpSisterHospitalGateway` — Resilience4j circuit breakers, mTLS REST `[STUBBED]` | [Ticket 007](.wayfinder/tickets/007-sister-hospital-gateway-and-prototype-profile.md) |
| **Constraint Solver** | `HeuristicBedAllocationSolver` — pure Java, synchronous `[IMPLEMENTED]` | `TimefoldBedAllocationSolver` — continuous daemon, multi-hospital clusters `[STUBBED]` | [Ticket 010](.wayfinder/tickets/010-advanced-constraint-solver-migration.md) |
| **Authentication** | `PrototypeSecurityFilter` — header-driven `X-User-Role` switching | Spring Security OAuth2/JWT + Singpass OIDC `[STUBBED]` | [Ticket 009](.wayfinder/tickets/009-enterprise-security-rbac-and-im8-audit.md) |
| **Database** | H2 in-memory with `create-drop` + seed data | Clustered PostgreSQL + Flyway migrations (driver included, not configured) | [Ticket 001](.wayfinder/tickets/001-data-model-and-domain-entities.md) |
| **Real-Time Push** | REST + TanStack Query polling (3s interval) | WebSocket / STOMP message broker | [Ticket 003](.wayfinder/tickets/003-realtime-event-notification-mechanism.md) |
| **Audit Log Destination** | Structured SLF4J/MDC to stdout, parsed via CLI scripts | Enterprise SIEM (CloudWatch/Splunk) + WORM storage | [Ticket 009](.wayfinder/tickets/009-enterprise-security-rbac-and-im8-audit.md) |
| **Patient Notifications** | In-app milestone polling + simulation endpoint | SMS / WhatsApp via hospital notification gateways | [Ticket 006](.wayfinder/tickets/006-tracer-bullet-2-patient-tracker-and-turnover-loop.md) |

---

## Testing & Quality

Testing follows a **deliberate pyramid strategy**: >90% line coverage across both frontend (Vitest + V8) and backend (JUnit 5 + JaCoCo), concentrated at the unit and integration test layers where domain invariants provide the highest confidence-per-test. Coverage is measured against the prototype profile — the code that actually executes. Production adapter stubs are tested to verify they fail-fast with `UnsupportedOperationException`, confirming the profile boundary contract.

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
- **No single-implementation interfaces** — concrete service classes directly, not `FooService` / `FooServiceImpl`. Interfaces are used only where genuine runtime polymorphism exists: gateway adapters and solver strategies that swap between prototype mocks and production implementations via Spring profiles.

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
| ------- | ----------- |
| **Backend** | Spring Boot 4.1, Java 25, Spring Security, Spring Data JPA, Bean Validation, Lombok |
| **Frontend** | React 19, TypeScript 6, TanStack Router + Query + Table, shadcn/ui, Tailwind CSS 4, Vite 8 |
| **Testing** | JUnit 5, JaCoCo, Vitest 5, Testing Library, happy-dom |
| **Database** | H2 in-memory (prototype profile) · PostgreSQL (production target — driver included, schema/migration not yet configured) |
| **Deployment** | Docker (multi-stage), Render |
| **Linting** | oxlint (frontend) |
