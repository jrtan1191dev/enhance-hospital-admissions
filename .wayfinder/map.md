# Wayfinder Map: Intelligent Patient Flow & Bed Capacity Orchestration System

## Destination

Produce a Comprehensive Technical Architecture Document (TAD) & Implementation Specification, locking core Algorithm & Domain Decisions (ADRs), and defining the MVP Tracer Bullet execution path for a lightweight React (TypeScript) + Spring Boot (Java) prototype.

## Notes

- **Domain**: Hospital Emergency Department (ED) clinical intake, Bed Management Unit (BMU) capacity orchestration, patient queue transparency, and inpatient discharge turnover.
- **Source Specification**: [`product-idea/user-stories.md`](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/product-idea/user-stories.md) and [`product-idea/pain-points.md`](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/product-idea/pain-points.md).
- **Core Technology Stack**: Spring Boot (Java) backend + React (TypeScript) frontend.
- **Prototype Strategy vs Target Production Architecture**:
  - *Documented Target Architecture*: We document the full-fledged enterprise design (e.g., Event Sourcing, CQRS, distributed Kafka/STOMP streaming, Timefold constraint solvers, and FHIR interoperability) in the Technical Architecture Document (TAD) for architectural completeness and future roadmapping.
  - *Prototype Execution*: **Deliberately take a streamlined, direct CRUD approach**. Specifically: **NO Event Sourcing, NO CQRS, NO complex distributed messaging**. The prototype performs direct in-place relational mutations via standard Spring Data JPA repositories and straightforward REST endpoints, with simple client polling / state refetches for the UI.
- **Skills to Consult**: `domain-modeling`, `grilling`, `prototype`, `research`.

## Decisions so far

- [001: Data Model & Domain Entities](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/001-data-model-and-domain-entities.md): UUID primary keys with standard bidirectional JPA relationships, in-memory H2 database with automatic boot DataInitializer, and 6 core relational entities (`Ward`, `Cubicle`, `Bed`, `Patient`, `AdmissionRequest`, `AssessmentBroadcast`).
- [003: Real-Time Event & Notification Mechanism](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/003-realtime-event-notification-mechanism.md): Target production architecture (CQRS & event-driven pub/sub) is documented, but the prototype deliberately takes a simplified direct REST + lightweight polling approach (no CQRS, no event sourcing).

## Not yet specified

<!-- Fog of war: in-scope questions that will graduate into tickets as the frontier advances -->

- **Live Hospital HL7/FHIR Ingestion Pipeline**: Ingesting real-time Observation, Encounter, and DiagnosticReport FHIR resources from hospital EHRs.
- **Sister Hospital & MIC@Home External Gateway**: API adapters for bilateral communication with Community Hospital (OCH/AH/SACH) bed boards and virtual ward telemetry providers.
- **Enterprise Security & Compliance Hardening**: Singpass/HealthHub OIDC authentication, IM8/ARC audit log immutability, and role-based access control (RBAC).
- **Advanced Constraint Solver Migration**: Scaling the heuristic engine to enterprise solvers (Timefold / OptaPlanner) when moving beyond prototype to multi-hospital clustering.

## Out of scope

<!-- Consciously ruled out of this prototype effort -->

- **Event Sourcing & Full CQRS Frameworks**: Explicitly bypassed in the prototype to maintain minimal cognitive and operational overhead; state is mutated directly in-place via standard relational tables.
- **Hardware & IoT Sensor Integration**: RFID/Infrared bed presence sensors or physical smart badges.
- **Financial Clearing & Payment Processing**: Real-time payment processing or direct CPF/MediSave claims settlement (informational financial explainer cards only).
- **Commercial Solver Licensing & Heavy Cloud Infrastructure**: Full enterprise Timefold/OptaPlanner deployment in early prototype.
