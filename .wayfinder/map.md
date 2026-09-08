# Wayfinder Map: Intelligent Patient Flow & Bed Capacity Orchestration System

## Destination

Produce a Comprehensive Technical Architecture Document (TAD) & Implementation Specification, locking core Algorithm & Domain Decisions (ADRs), and defining the MVP Tracer Bullet execution path for a lightweight React (TypeScript) + Spring Boot (Java) prototype.

## Notes

- **Domain**: Hospital Emergency Department (ED) clinical intake, Bed Management Unit (BMU) capacity orchestration, patient queue transparency, and inpatient discharge turnover.
- **Source Specification**: [`product-idea/user-stories.md`](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/product-idea/user-stories.md) and [`product-idea/pain-points.md`](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/product-idea/pain-points.md).
- **Core Technology Stack**: Spring Boot (Java) backend + React (TypeScript) frontend.
- **Prototype Strategy vs Target Production Architecture**:
  - *Spring Profile Boundary (Production-Ready Default vs Prototype Profile Isolation)*: The codebase is **production-ready by default**. When no profile is active, default beans connect to real persistent databases and external HTTP/FHIR adapters. All prototype decisions, constraints, synthetic datasets, and mocked integration behaviors discussed to date are **strictly tied to the `prototype` profile only** (`@Profile("prototype")`). Future profiles such as `dev` and `qa` remain separate and distinct environments with their own forthcoming specifications, and are strictly decoupled from prototype mocks.
  - *Documented Target Architecture*: Full enterprise design (e.g., Event Sourcing, CQRS, distributed STOMP streaming, Timefold solvers, and production Sister Hospital FHIR APIs) is documented for architectural completeness and future roadmapping.
  - *Prototype Execution*: Deliberately takes a streamlined, direct CRUD approach (standard relational mutations, pure Java heuristics, and TanStack Query polling).
- **Skills to Consult**: `domain-modeling`, `grilling`, `prototype`, `research`.

## Decisions so far

- [001: Data Model & Domain Entities](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/001-data-model-and-domain-entities.md): UUID primary keys with standard bidirectional JPA relationships, in-memory H2 database under `@Profile("prototype")`, and 5 core relational entities (`Ward`, `Bed`, `Patient`, `AdmissionRequest`, `AssessmentBroadcast`) under a strict Level -> Ward -> Bed hierarchy (cubicle entity explicitly eliminated).
- [002: Bare-Minimum Heuristic Bed Constraint & Batching Algorithm](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/002-heuristic-bed-constraint-and-batching-algo.md): Four bed states (`MUSTARD YELLOW` [`EMPTY_PENDING_CLEANING`], `WHITE` [`EMPTY_CLEANED`], `GREEN` [`EMPTY_ASSIGNED`], `GREY` [`OCCUPIED_TAKEN`]), configurable weights via BMU portal, acuity-first queue sorting, two-phase pack-then-batch heuristics, and synchronous on-demand evaluation in Spring Boot.
- [003: Real-Time Event & Notification Mechanism](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/003-realtime-event-notification-mechanism.md): Target production architecture (CQRS & event-driven pub/sub) is documented, but the prototype deliberately takes a simplified direct REST + lightweight polling approach (no CQRS, no event sourcing).
- [004: API Surface & Contract Design](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/004-api-surface-and-contract-design.md): Consolidated into exactly 3 Spring Web controllers (`ClinicianController`, `BmuController`, `PatientTrackerController`) using direct domain DTOs and native Spring ProblemDetail error handling without third-party wrapper bloat.
- [005: MVP Tracer Bullet Vertical Slice 1 Architecture](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/005-tracer-bullet-1-ed-to-bmu-architecture.md): Defined monorepo skeleton, TanStack Router with access-matrix route guards, no-auth topbar role-switcher with `[📱 Patient Admission Tracker]`, realistic seed dataset under `@Profile("prototype")`, and 3-step test flow (ED Attending -> Specialist Consult -> BMU 1-click allocation).
- [006: MVP Tracer Bullet Vertical Slice 2 Architecture](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/006-tracer-bullet-2-patient-tracker-and-turnover-loop.md): Completed circular lifecycle with mobile frame patient milestone tracker, ward nurse check-in and vacate triggers (flipping bed to Mustard Yellow), and housekeeping 30-min cleaning SLA sign-off flipping beds from Mustard Yellow back to White (empty, cleaned).
- [007: Sister Hospital Gateway & Prototype Profile Architecture](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/007-sister-hospital-gateway-and-prototype-profile.md): Gateway interface pattern separating `@Profile("prototype") MockSisterHospitalGateway` (simulating instant 30-min SLA acceptance and external referral IDs for OCH, AH, SACH, and MIC@Home) from default production-ready `HttpSisterHospitalGateway` adapters, ensuring complete maintainability via Spring DI.
- [008: Live Hospital EHR & HL7/FHIR Ingestion Pipeline](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/008-live-hospital-ehr-fhir-ingestion-pipeline.md): Gateway interface pattern separating `@Profile("prototype") MockHospitalEhrGateway` (providing synthetic pre-population diagnostic baselines for P101–P104) from default production-ready `FhirHospitalEhrGateway` (HAPI FHIR R4 client ingesting Observation, Encounter, and DiagnosticReport resources from live EHRs).
- [009: Enterprise Security, RBAC & IM8 Audit Logging Architecture](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/009-enterprise-security-rbac-and-im8-audit.md): Production-ready Spring Security OAuth2/JWT and IM8 audit logging by default; prototype profile uses `PrototypeSecurityFilter` mapping topbar role switches to seeded clinical accounts (`dr_tan_ed`, `bmu_coord_wong`) with real structured audit log statements emitted via SLF4J/MDC. Instruments all 21 operational KPIs defined in `pain-points.md` with explicit database SQL and CLI log-parsing (`jq`/`grep`/`awk`) extraction recipes.
- [010: Advanced Constraint Solver Migration & Solver Pluggability](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/010-advanced-constraint-solver-migration.md): Domain solver interface `BedAllocationSolver` separating `@Profile("prototype") HeuristicBedAllocationSolver` (fast, deterministic, pure Java two-phase heuristic engine) from default enterprise continuous solver `TimefoldBedAllocationSolver` (Timefold / OptaPlanner) for future regional multi-hospital cluster optimization.
- [011: Operational KPI Logging & Metric Extraction Architecture](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/011-kpi-logging-and-metric-extraction-architecture.md): Formalized dual-pathway metric architecture (relational entity audit fields + structured SLF4J/MDC audit logs) covering all 21 hospital operational KPIs from pain-points.md with explicit SQL and CLI/log extraction formulas.

## Not yet specified

*(All known architectural dimensions, integration gateways, and former unspecified items have been formally specified and resolved into ADR-001 through ADR-011).*

## Out of scope

<!-- Consciously ruled out of this prototype effort -->

- **Event Sourcing & Full CQRS Frameworks**: Explicitly bypassed in the prototype to maintain minimal cognitive and operational overhead; state is mutated directly in-place via standard relational tables.
- **Hardware & IoT Sensor Integration**: RFID/Infrared bed presence sensors or physical smart badges.
- **Financial Clearing & Payment Processing**: Real-time payment processing or direct CPF/MediSave claims settlement (informational financial explainer cards only).
- **Commercial Solver Licensing & Heavy Cloud Infrastructure**: Full enterprise Timefold/OptaPlanner deployment in early prototype.
