# Production Roadmap

## Overview

This prototype is **production-architected by design** — the architecture, interfaces, and domain invariants are identical between prototype and production modes. Only the infrastructure adapters change. Every production target below is already specified in the architectural tickets.

The domain logic, constraint rules, state machines, and API contracts are production-ready today — zero rewrite required. Transitioning to production requires **implementing** the integration adapters behind the existing gateway interfaces (EHR FHIR client, sister hospital HTTP client, OAuth2 security, PostgreSQL migrations, real-time push). The interfaces and profile boundary are designed so this work is additive — new adapter implementations behind existing interfaces, not architectural changes. Each adapter can be implemented independently without touching domain logic or other adapters.

> [!NOTE]
> Items marked `[IMPLEMENTED]` are working in the prototype today. Items marked `[DESIGNED]` have their interface contracts and architectural patterns specified but the production adapter is stubbed with `UnsupportedOperationException`. Items marked `[STUBBED]` indicate the default bean exists as a placeholder only.

## 1. Reliability & Resilience

- **Circuit Breakers** `[DESIGNED]`: I have designed Resilience4j wrapping for all external gateway calls (Sister Hospital APIs, EHR FHIR endpoints). This is specified in [ticket 007](../.wayfinder/tickets/007-sister-hospital-gateway-and-prototype-profile.md).
- **Continuous Constraint Solver** `[DESIGNED]`: I designed Timefold to run as a daemon, reacting to real-time HL7 ADT events for multi-hospital clusters. The `TimefoldBedAllocationSolver` stub exists as the default bean; the prototype uses `HeuristicBedAllocationSolver` instead. This is specified in [ticket 010](../.wayfinder/tickets/010-advanced-constraint-solver-migration.md).
- **Persistent Storage** `[DESIGNED]`: I have planned for Clustered PostgreSQL with Flyway migrations to replace H2 in-memory storage. The PostgreSQL driver is included as a dependency; schema and migration scripts are not yet configured. This is specified in [ticket 001](../.wayfinder/tickets/001-data-model-and-domain-entities.md).
- **Optimistic Locking** `[IMPLEMENTED]`: I have already implemented `@Version` annotations preventing concurrent specialist claim races (HTTP 409).

## 2. Observability & Monitoring

- **Structured Audit Logging** `[IMPLEMENTED]`: I ensured SLF4J/MDC audit events are already emitted in the prototype, ready for SIEM ingestion. This is specified in [ticket 009](../.wayfinder/tickets/009-enterprise-security-rbac-and-im8-audit.md).
- **Dual-Pathway KPI Extraction** `[IMPLEMENTED]`: I have already verified SQL queries and structured log parsing for mathematical parity. This is specified in [ticket 011](../.wayfinder/tickets/011-kpi-logging-and-metric-extraction-architecture.md).
- **Production Dashboards** `[DESIGNED]`: I designed Path A to feed read replicas for Metabase/Tableau BI, and Path B to stream to CloudWatch/OpenSearch/Datadog for real-time alerting.
- **Spring Boot Actuator** `[IMPLEMENTED]`: I have already included this as a dependency — health checks, metrics, and info endpoints are available out of the box.
- **All 21 operational KPIs** `[IMPLEMENTED]`: I instrumented all KPIs from the pain points document with explicit SQL and CLI extraction recipes.

## 3. Security & Compliance

- **Authentication** `[DESIGNED]`: I designed Spring Security OAuth2/JWT for hospital staff (Active Directory/Keycloak), and Singpass/HealthHub OIDC for patient tracking. The default security configuration denies all API access without valid authentication; the prototype profile relaxes this with header-driven persona switching. This is specified in [ticket 009](../.wayfinder/tickets/009-enterprise-security-rbac-and-im8-audit.md).
- **IM8/ARC Compliance** `[DESIGNED]`: I designed the audit logging pipeline so that structured logs are WORM-storage-ready — the log format and MDC context are identical in both modes. In the prototype, logs are emitted to stdout; in production, the same structured output would be routed to an immutable SIEM backend.
- **RBAC Method Security** `[DESIGNED]`: I designed `@PreAuthorize` annotations validating verified JWT token scopes. The prototype uses header-driven role mapping instead.
- **Clinical Safety Invariants** `[IMPLEMENTED]`: I hardcoded absolute safety constraints (gender cohorting, airborne isolation) as non-overridable — the software strictly forbids bypassing them even with coordinator override. This is specified in [ADR 0010](../adr/0010-two-tier-bed-allocation-constraints.md).

## 4. External Integrations

- **Hospital EHR** `[DESIGNED]`: I designed a HAPI FHIR R4 client ingesting Patient, Encounter, Observation, and DiagnosticReport resources via mTLS and SMART on FHIR. The `FhirHospitalEhrGateway` stub exists as the default bean; the prototype uses `MockHospitalEhrGateway` with synthetic patient data. This is specified in [ticket 008](../.wayfinder/tickets/008-live-hospital-ehr-fhir-ingestion-pipeline.md).
- **Sister Hospital Transfers** `[DESIGNED]`: I designed authenticated REST/FHIR referral packets to OCH, AH, SACH with bilateral SLA tracking. The `HttpSisterHospitalGateway` stub exists as the default bean; the prototype uses `MockSisterHospitalGateway`. This is specified in [ticket 007](../.wayfinder/tickets/007-sister-hospital-gateway-and-prototype-profile.md).
- **Real-Time Push** `[DESIGNED]`: I designed a WebSocket/STOMP message broker to replace polling for sub-millisecond clinical collaboration. The prototype uses REST + TanStack Query polling (3s interval). This is specified in [ticket 003](../.wayfinder/tickets/003-realtime-event-notification-mechanism.md).
- **Patient Notifications** `[DESIGNED]`: I designed SMS/WhatsApp/Singpass integrations via hospital notification gateways for 2-hour periodic milestone updates. The prototype uses in-app milestone polling.

## 5. Scalability

- **Multi-Hospital Regional Clusters** `[DESIGNED]`: I designed the Timefold solver to support optimizing across SingHealth (SGH + OCH + SKH), NUHS, and NHG clusters simultaneously. The `TimefoldBedAllocationSolver` stub is the placeholder for this capability.
- **Event Sourcing & CQRS** `[DESIGNED]`: I designed an immutable event store (Axon/Kafka) with read-model projections for temporal queries and multi-tenant collaboration. This is specified in [ticket 003](../.wayfinder/tickets/003-realtime-event-notification-mechanism.md).
- **Horizontal Scaling** `[DESIGNED]`: I designed for stateless Spring Boot instances behind a load balancer, with the solver running as a separate microservice.

## What's Already Production-Grade in the Prototype

I intentionally did not simplify the following components; they are identical in both prototype and production:

- **Domain invariants** (gender cohorting, infection isolation, telemetry union) — identical logic in both modes.
- **Bed state machine** (4-state lifecycle with strict transition gates) — identical.
- **Consensus completion gate** (all broadcasts must complete before BMU dispatch) — identical.
- **Two-tier constraint hierarchy** (absolute safety vs overridable operational) — identical.
- **Structured audit log format and MDC context** — identical format, only the destination changes.
- **API contracts and error handling** (ProblemDetail) — identical.
