# 002: Bare-Minimum Heuristic Bed Constraint & Batching Algorithm (ADR-002)

- **Type**: `wayfinder:prototype`
- **Status**: `open`
- **Assignee**: `unassigned`
- **Blocked by**: none
- **Blocks**: [005-tracer-bullet-1-ed-to-bmu-architecture.md](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/005-tracer-bullet-1-ed-to-bmu-architecture.md)

## Question

How should the two-phase pack-then-batch and dynamic cohort-swap algorithm be implemented in lightweight Java/Spring Boot without external solvers?
1. Exact scoring functions for hard constraints (gender cohorting, isolation, ward class tier) and soft optimization (service clustering, fall risk bed proximity).
2. Phase 1 Consolidation: Algorithm logic for identifying and filling matching partially filled wards (`Grey`/`Green` + `White` beds).
3. Phase 2 Holding Ward Creation: Algorithm logic for detecting clusters ($\ge 3$ patients) and selecting candidate all-`White` flex wards.
4. Dynamic Cohort-Swap Re-Optimization: Detection logic for isolated `Green` beds blocking an otherwise empty flex ward and proposing swaps to BMU before ED departure.
