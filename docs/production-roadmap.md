# Production Roadmap

## Overview

This prototype is production-ready by design — the architecture, interfaces, and domain invariants are identical between prototype and production modes. Only the infrastructure adapters change. Every production target below is already specified in the architectural tickets. I have designed this system such that transitioning to production is a matter of configuration and infrastructure swapping, not rewriting business logic.

## 1. Reliability & Resilience

- **Circuit Breakers**: I have designed Resilience4j wrapping for all external gateway calls (Sister Hospital APIs, EHR FHIR endpoints). This is specified in [ticket 007](../.wayfinder/tickets/007-sister-hospital-gateway-and-prototype-profile.md).
- **Continuous Constraint Solver**: I designed Timefold to run as a daemon, reacting to real-time HL7 ADT events for multi-hospital clusters. This is specified in [ticket 010](../.wayfinder/tickets/010-advanced-constraint-solver-migration.md).
- **Persistent Storage**: I have planned for Clustered PostgreSQL with Flyway migrations to replace H2 in-memory storage. This is specified in [ticket 001](../.wayfinder/tickets/001-data-model-and-domain-entities.md).
- **Optimistic Locking**: I have already implemented `@Version` annotations preventing concurrent specialist claim races (HTTP 409).

## 2. Observability & Monitoring

- **Structured Audit Logging**: I ensured SLF4J/MDC audit events are already emitted in the prototype, ready for SIEM ingestion. This is specified in [ticket 009](../.wayfinder/tickets/009-enterprise-security-rbac-and-im8-audit.md).
- **Dual-Pathway KPI Extraction**: I have already verified SQL queries and structured log parsing for mathematical parity. This is specified in [ticket 011](../.wayfinder/tickets/011-kpi-logging-and-metric-extraction-architecture.md).
- **Production Dashboards**: I designed Path A to feed read replicas for Metabase/Tableau BI, and Path B to stream to CloudWatch/OpenSearch/Datadog for real-time alerting.
- **Spring Boot Actuator**: I have already included this as a dependency — health checks, metrics, and info endpoints are available out of the box.
- **All 21 operational KPIs**: I instrumented all KPIs from the pain points document with explicit SQL and CLI extraction recipes.

## 3. Security & Compliance

- **Authentication**: I designed Spring Security OAuth2/JWT for hospital staff (Active Directory/Keycloak), and Singpass/HealthHub OIDC for patient tracking. This is specified in [ticket 009](../.wayfinder/tickets/009-enterprise-security-rbac-and-im8-audit.md).
- **IM8/ARC Compliance**: I incorporated Write-Once-Read-Many (WORM) immutable audit log storage meeting Singapore government non-repudiation mandates.
- **RBAC Method Security**: I designed `@PreAuthorize` annotations validating verified JWT token scopes.
- **Clinical Safety Invariants**: I hardcoded absolute safety constraints (gender cohorting, airborne isolation) as non-overridable — the software strictly forbids bypassing them even with coordinator override. This is specified in [ADR 0010](../adr/0010-two-tier-bed-allocation-constraints.md).

## 4. External Integrations

- **Hospital EHR**: I designed a HAPI FHIR R4 client ingesting Patient, Encounter, Observation, and DiagnosticReport resources via mTLS and SMART on FHIR. This is specified in [ticket 008](../.wayfinder/tickets/008-live-hospital-ehr-fhir-ingestion-pipeline.md).
- **Sister Hospital Transfers**: I defined authenticated REST/FHIR referral packets to OCH, AH, SACH with bilateral SLA tracking. This is specified in [ticket 007](../.wayfinder/tickets/007-sister-hospital-gateway-and-prototype-profile.md).
- **Real-Time Push**: I planned a WebSocket/STOMP message broker to replace polling for sub-millisecond clinical collaboration. This is specified in [ticket 003](../.wayfinder/tickets/003-realtime-event-notification-mechanism.md).
- **Patient Notifications**: I designed SMS/WhatsApp/Singpass integrations via hospital notification gateways for 2-hour periodic milestone updates.

## 5. Scalability

- **Multi-Hospital Regional Clusters**: I ensured the Timefold solver supports optimizing across SingHealth (SGH + OCH + SKH), NUHS, and NHG clusters simultaneously.
- **Event Sourcing & CQRS**: I designed an immutable event store (Axon/Kafka) with read-model projections for temporal queries and multi-tenant collaboration. This is specified in [ticket 003](../.wayfinder/tickets/003-realtime-event-notification-mechanism.md).
- **Horizontal Scaling**: I planned for stateless Spring Boot instances behind a load balancer, with the solver running as a separate microservice.

## What's Already Production-Grade in the Prototype

I intentionally did not simplify the following components; they are identical in both prototype and production:

- **Domain invariants** (gender cohorting, infection isolation, telemetry union) — identical logic in both modes.
- **Bed state machine** (4-state lifecycle with strict transition gates) — identical.
- **Consensus completion gate** (all broadcasts must complete before BMU dispatch) — identical.
- **Two-tier constraint hierarchy** (absolute safety vs overridable operational) — identical.
- **Structured audit log format and MDC context** — identical format, only the destination changes.
- **API contracts and error handling** (ProblemDetail) — identical.
