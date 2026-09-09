# 03: Dynamic Holding Ward Batching for Surge Clusters

**What to build:**
Enable automated surge cluster detection and bulk flex ward allocation to rapidly absorb patient surges without creating fragmented ghost capacity. The backend continuously scans pending `BED_REQUESTED` queues to identify surge clusters of $\ge 3$ patients sharing the same Ward Class, biological gender, and infection profile. When an all-`White` (`EMPTY_CLEANED`) flex ward is available, the BMU console surfaces a high-visibility "Batch Holding Ward Suggestion" card detailing the matching cluster, destination ward, and unlocked capacity. Upon coordinator approval, the batch engine assigns candidate beds simultaneously, applies greedy FIFO dwell-time slicing if the cluster size exceeds ward capacity, locks the ward cohort attributes (`lockedGender`, `lockedInfectionStatus`, `isHoldingWard = true`), and atomically transitions assigned beds to `EMPTY_ASSIGNED` (`Green`). Any residual beds remain `EMPTY_CLEANED` under the new cohort lock for rapid consolidation packing (+30 bonus).

**Blocked by:** 02: Four-State Bed Lifecycle Machine & EVS Turnover Loop

**Status:** completed

- [x] Batch detection engine scans pending admission requests to identify surge clusters of $\ge 3$ patients matching ward class, gender, and infection status.
- [x] Endpoint `GET /api/v1/bmu/batch-suggestions` surfaces active cluster suggestions paired with available all-`White` flex wards.
- [x] Endpoint `POST /api/v1/bmu/batch-holding-wards/approve` atomically assigns cluster patients to the target flex ward in a single transaction.
- [x] When cluster size exceeds flex ward capacity, the batch engine prioritizes patients using greedy FIFO dwell-time slicing (earliest `requestedAt` first), leaving remaining cluster patients at the top of the queue.
- [x] Flex ward cohort locks (`lockedGender`, `lockedInfectionStatus`, `isHoldingWard = true`) are set upon approval.
- [x] Residual clean beds within the batch-allocated ward remain `EMPTY_CLEANED` under the established cohort lock, receiving the +30 consolidation bonus for matching individual arrivals.
- [x] BMU console displays prominent Batch Suggestion cards with 1-click approval and patient list inspection.
- [x] Structured audit log event `APPROVE_BATCH_HOLDING_WARD` is emitted recording coordinator ID, ward ID, batch size, and patient IDs.
- [x] Unit and integration tests verify cluster grouping logic, FIFO dwell slicing, atomic multi-bed assignment, and residual bed cohort locking.
