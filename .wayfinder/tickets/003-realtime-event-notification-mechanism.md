# 003: Real-Time Event & Notification Mechanism (ADR-003)

- **Type**: `wayfinder:research`
- **Status**: `closed`
- **Assignee**: `research-agent`
- **Blocked by**: none
- **Blocks**: [004-api-surface-and-contract-design.md](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/004-api-surface-and-contract-design.md), [005-tracer-bullet-1-ed-to-bmu-architecture.md](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/005-tracer-bullet-1-ed-to-bmu-architecture.md)

## Question

What is the most lightweight, robust real-time communication mechanism in Spring Boot + React to power:

1. Multi-doctor assessment broadcast push notifications to on-call specialty feeds.
2. Real-time BMU bed queue updates and live 3-state (`White`/`Green`/`Grey`) grid refreshes.
3. Patient & Family Milestone Tracker updates and 2-hour periodic refreshes.
Should we use Spring Boot WebSockets with STOMP, Server-Sent Events (SSE), or standard polling for the prototype?

## Resolution (ADR-003: Real-Time & State Refresh Strategy — Target Architecture vs. Prototype Approach)

### Track 1: Documented Target Production Architecture (Future Roadmap)

- **Pattern**: Full **Event Sourcing & CQRS** with an immutable append-only event store (e.g. Axon Framework, Kafka, or EventStoreDB) paired with read-model projections in PostgreSQL/Elasticsearch.
- **Real-Time Streaming**: Bi-directional full-duplex WebSocket connections with STOMP broker for sub-millisecond multi-client collaboration, clinical whiteboarding, and multi-tenant EHR sync.
- **Audit & Compliance**: Tamper-proof audit logs for medical-legal compliance, supporting temporal queries ("what was the bed state and attending review at 03:14 AM?").

---

### Track 2: Prototype Implementation Decision (Deliberate Prototype Simplification)

- **Decision**: **Explicitly bypass Event Sourcing, CQRS, and complex messaging brokers.** Implement **Direct REST CRUD + In-Place Relational Mutations + Lightweight Client Polling**.
- **Rationale**:
  - **Radical Simplicity**: Building a prototype requires the fastest path to validate user journeys. Event Sourcing and CQRS introduce massive accidental complexity (event schemas, versioning, upcasters, projection replays, eventual consistency lag).
  - **Direct Relational Mutations**: A standard relational schema (Spring Data JPA updating rows in-place) is immediately understandable, easily inspectable in H2/Postgres consoles, and trivial to reset between demo runs.
  - **Frontend State Refresh**: The React frontend uses straightforward data fetching (via TanStack Query / standard fetch with polling intervals e.g. 2–3s on active queue screens, or manual refetch on action triggers).
  - **Zero Protocol Overhead**: No socket lifecycle debugging, no STOMP frame parsing, and no connection proxy issues in local Vite development.
- **Summary**: Target production architecture is documented for reference in the Technical Architecture Document, while prototype execution remains lean, robust, and fast to build.
