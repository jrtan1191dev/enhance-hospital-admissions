# 002: Bare-Minimum Heuristic Bed Constraint & Batching Algorithm (ADR-002)

- **Type**: `wayfinder:prototype`
- **Status**: `closed`
- **Assignee**: `antigravity`
- **Blocked by**: none
- **Blocks**: [005-tracer-bullet-1-ed-to-bmu-architecture.md](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/005-tracer-bullet-1-ed-to-bmu-architecture.md)

## Question

How should the two-phase pack-then-batch and dynamic cohort-swap algorithm be implemented in lightweight Java/Spring Boot without external solvers?

1. Exact scoring functions for hard constraints (gender cohorting, isolation, ward class tier) and soft optimization (service clustering, fall risk bed proximity).
2. Phase 1 Consolidation: Algorithm logic for identifying and filling matching partially filled wards (`Grey`/`Green` + `White` beds).
3. Phase 2 Holding Ward Creation: Algorithm logic for detecting clusters ($\ge 3$ patients) and selecting candidate all-`White` flex wards.
4. Dynamic Cohort-Swap Re-Optimization: Detection logic for isolated `Green` beds blocking an otherwise empty flex ward and proposing swaps to BMU before ED departure.

## Resolution (ADR-002: Bare-Minimum Heuristic Bed Allocation & Dynamic Batching Algorithm)

### 1. Refined Bed Status Machine

Bed status encompasses four distinct operational states:

1. `EMPTY_PENDING_CLEANING`: Bed is physically vacated by discharged patient, but 30-min housekeeping sanitization is pending/in progress. (Ineligible for assignment).
2. `EMPTY_CLEANED` (`WHITE`): Vacant, sanitized, inspected, and immediately available for matching.
3. `EMPTY_ASSIGNED` (`GREEN`): Allocated to an ED patient by BMU; patient has not yet physically arrived at the ward.
4. `OCCUPIED_TAKEN` (`GREY`): Patient has arrived at the ward and is occupying the bed.

---

### 2. Configurable Weights via BMU Configuration Portal

Algorithm scoring parameters are stored in a configurable entity (`BmuAlgorithmConfig`), editable in real time via the BMU Configuration Portal:

- `wardClassMatchWeight`: Default `100` (Hard constraint filter)
- `genderCohortMatchWeight`: Default `100` (Hard constraint filter)
- `infectionClusterMatchWeight`: Default `100` (Hard constraint filter)
- `serviceSpecialtyClusterWeight`: Default `40` (Soft score bonus)
- `consolidationPackingWeight`: Default `30` (Soft score bonus for filling existing partially occupied wards)
- `fallRiskProximityWeight`: Default `15` (Soft score bonus for high fall-risk patients placed near nursing station)
- `batchHoldingWardThreshold`: Default `3` patients (Threshold to trigger Batch Holding Ward recommendations)

---

### 3. Queue Ordering & Ranking Logic

1. **Primary Queue Sort**: Sorted by clinical acuity severity:
   $$\text{Tier 1 (Critical)} > \text{Tier 2 (Acute Urgent)} > \text{Tier 3 (Acute Stable)} > \text{Tier 4 (Subacute)} > \text{Tier 5 (Observation)}$$
2. **Secondary Queue Sort**: For patients within the same acuity tier, sorted by `requestedAt` timestamp (FIFO dwell time).

---

### 4. Two-Phase Algorithm & Dynamic Cohort-Swap

- **Phase 1 (Consolidation Packing)**:
  - Evaluates all `EMPTY_CLEANED` beds against the top-priority waiting patient.
  - **Hard Filters**: Disqualify beds if ward class mismatches, if ward locked gender or locked infection status contradicts the patient, or if mandatory telemetry/negative pressure is missing.
  - **Soft Scoring**: Scores surviving beds:
    $$\text{Score} = \text{Specialty Match} + \text{Consolidation Bonus} + \text{Proximity Bonus}$$
  - Beds in partially occupied wards (`OCCUPIED_TAKEN` or `EMPTY_ASSIGNED` present) receive the +30 consolidation bonus, naturally protecting completely empty flex wards.
  - Returns the **Top 3 Recommended Beds** with match rationale.
- **Phase 2 (Dynamic Holding Ward Batching)**:
  - Scans the waiting queue and clusters patients by $\langle \text{Ward Class}, \text{Gender}, \text{Infection Cluster} \rangle$.
  - When cluster size $\ge$ threshold (e.g. $\ge 3$), identifies candidate all-`EMPTY_CLEANED` flex wards and presents a proactive **"Batch Holding Ward Suggestion"** card to BMU coordinators for 1-click batch allocation.
- **Dynamic Cohort-Swap Re-Optimization**:
  - Detects sub-optimal lockouts where an otherwise empty ward has only 1–2 `EMPTY_ASSIGNED` beds, while a high-density cluster of another cohort is waiting.
  - Generates a 1-click proposal to reassign the isolated `EMPTY_ASSIGNED` patient(s) to alternative matching partially filled wards, freeing the flex ward to batch-admit the surge cluster.

---

### 5. Architectural Strategy: Target vs. Prototype

- **Target Production Architecture (Future Roadmap)**: Event-driven reactive pipeline where domain events (e.g. `PatientVacatedEvent`, `BedCleanedEvent`) trigger asynchronous recalculations via an event bus.
- **Prototype Implementation Decision**: **Pure Java synchronous on-demand evaluation.**
  - Recommendations and batch suggestions are computed synchronously in-memory by Spring Boot services whenever `GET /api/bmu/queue` or `GET /api/bmu/beds/recommendations/{requestId}` is called.
  - Easiest, most direct implementation: zero complex event bus plumbing, zero async race conditions, and instant response times on mocked H2 datasets.
