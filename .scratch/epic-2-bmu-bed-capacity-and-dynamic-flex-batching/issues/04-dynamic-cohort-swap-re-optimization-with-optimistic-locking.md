# 04: Dynamic Cohort-Swap Re-Optimization with Optimistic Locking

**What to build:**
Empower BMU coordinators to liberate flex wards obstructed by 1–2 isolated patient reservations so that large incoming surge clusters can be placed. The optimization engine detects when an otherwise empty flex ward is blocked by 1 or 2 isolated `EMPTY_ASSIGNED` (`Green`) beds whose patients are still waiting in the ED, and identifies viable non-conflicting transfer beds in matching partially occupied wards. The BMU console renders a "Dynamic Cohort-Swap Proposal" comparing current versus proposed bed assignments and displaying the net unlocked capacity. Upon coordinator approval, the system atomically re-points the isolated patient's reservation to the target partially occupied bed and clears the flex ward bed back to `EMPTY_CLEANED`. The transaction uses optimistic locking and a transit gate: if the patient has already departed the ED or the target bed has been claimed, the swap is rejected with RFC 7807 `409 Conflict`.

**Blocked by:** 03: Dynamic Holding Ward Batching for Surge Clusters

**Status:** ready-for-agent

- [ ] Cohort-swap detection engine scans flex wards to identify those blocked by 1 or 2 isolated `EMPTY_ASSIGNED` beds where the assigned patient has not departed the ED (`waitingInEd = true`).
- [ ] Engine searches for candidate replacement beds in partially occupied wards satisfying all hard constraints (ward class, gender, telemetry, infection status) without introducing new cohort conflicts.
- [ ] Endpoint `GET /api/v1/bmu/cohort-swap-suggestions` returns calculated swap proposals including blocked patient, current bed, target bed, target ward, and unlocked capacity count.
- [ ] Endpoint `POST /api/v1/bmu/cohort-swap/approve` atomically swaps the patient's reservation from the flex bed to the target bed, returning the flex bed to `EMPTY_CLEANED`.
- [ ] Atomic swap execution validates optimistic locking / transit status; if the patient is no longer waiting in the ED or the target bed is not `EMPTY_CLEANED`, it immediately rejects with RFC 7807 `409 Conflict`.
- [ ] BMU UI presents a side-by-side Cohort Swap proposal card with 1-click coordinator approval and rejection handling.
- [ ] Structured audit log event `APPROVE_COHORT_SWAP` is emitted with coordinator identity, reallocated patient ID, origin and destination bed numbers, and liberated ward ID.
- [ ] Integration tests verify detection of blocked flex wards, candidate bed matching, atomic swap execution, and conflict rejection on ED departure.
