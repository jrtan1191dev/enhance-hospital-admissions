# 010: Advanced Constraint Solver Migration & Solver Pluggability (ADR-010)

- **Type**: `wayfinder:prototype`
- **Status**: `closed`
- **Assignee**: `antigravity`
- **Blocked by**: none
- **Blocks**: none

## Question

How does the bed allocation engine scale when transitioning from single-hospital prototype evaluation to enterprise-wide, multi-hospital regional healthcare clusters (e.g., SingHealth, NUHS, NHG)? How should the solver abstraction be architected across prototype and production environments?

## Resolution (ADR-010: Pluggable BedAllocationSolver & Timefold Migration Path)

### 1. Pluggable Solver Interface (`BedAllocationSolver`)

To guarantee that BMU controllers and allocation services remain completely decoupled from the underlying mathematical solver implementation, the system introduces a domain solver interface:

```java
public interface BedAllocationSolver {
    List<BedRecommendationDto> computeRecommendations(UUID admissionRequestId, BmuAlgorithmConfig config);
    List<BatchSuggestionDto> detectBatchHoldingOpportunities(BmuAlgorithmConfig config);
    List<CohortSwapDto> detectCohortSwaps(BmuAlgorithmConfig config);
}
```

---

### 2. Dual Solver Implementation by Profile

#### Prototype Profile (`@Profile("prototype")`): `HeuristicBedAllocationSolver`
- **Implementation**: Pure Java synchronous heuristic engine implementing the 2-phase pack-then-batch and dynamic cohort-swap algorithms formulated in **ADR-002**.
- **Performance**: Sub-50 millisecond synchronous evaluation executed on-demand in in-memory H2 datasets without external background threads.
- **Benefits**:
  - Zero third-party solver licenses or native compilation binaries.
  - 100% deterministic results for interactive walkthroughs and automated integration tests.
  - Immediate response times when BMU coordinators inspect bed recommendations or adjust configuration portal sliders.

#### Default (Production-Ready) Implementation: `TimefoldBedAllocationSolver`
- **Implementation**: Enterprise constraint optimization engine powered by **Timefold Solver** (formerly OptaPlanner).
- **Target Scale**: Multi-hospital cluster optimization (e.g. SGH + OCH + SKH across SingHealth).
- **Score Director Mechanics**:
  - *Hard Constraints*: Zero violations of gender cohorting, airborne infection isolation, and ward class limits.
  - *Medium Constraints*: Regional load balancing between acute tertiary wards and step-down community hospitals.
  - *Soft Constraints*: Specialty alignment, nursing travel distances, and patient fall-risk proximity scores.
- **Continuous Optimization**: Runs as a daemon background solver reacting to real-time HL7 ADT bed census events.

---

### 3. Updated Component Matrix

| Capability / Interface | `@Profile("prototype")` Implementation | Default (Production-Ready) Implementation |
| :--- | :--- | :--- |
| **`BedAllocationSolver`** | `HeuristicBedAllocationSolver` (Pure Java 2-phase heuristic engine, synchronous <50ms) | `TimefoldBedAllocationSolver` (Continuous Timefold / OptaPlanner constraint engine for regional clusters) |
| **`HospitalEhrGateway`** | `MockHospitalEhrGateway` (synthetic baseline DTOs for P101–P104) | `FhirHospitalEhrGateway` (HAPI FHIR R4 client ingesting live EHR resources) |
| **`SisterHospitalGateway`** | `MockSisterHospitalGateway` (simulated referral IDs & 30-min SLA timer) | `HttpSisterHospitalGateway` (real mTLS REST / FHIR calls) |
| **Security & Identities** | `PrototypeSecurityFilter` (maps `X-User-Role` header to seeded accounts) | `OAuth2ResourceServerFilter` (validates signed hospital JWTs) |
| **Audit Logging** | Real structured log statements with seeded usernames | Real structured log statements shipped to immutable SIEM |
| **Database** | In-Memory H2 DB (`create-drop`) | Clustered PostgreSQL with managed migrations |
| **Seed Dataset** | Active (`DataInitializer` for Ward 8A, 8B, 9A & P101–P104) | Disabled (live patient records via EHR feeds) |
