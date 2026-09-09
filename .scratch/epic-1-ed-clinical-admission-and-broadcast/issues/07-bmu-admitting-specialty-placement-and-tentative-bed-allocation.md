# 07: BMU Admitting Specialty Placement & Tentative Bed Allocation

**What to build:**
Empower BMU coordinators to review completed multi-specialty consult directives, view ward availability, and select the final `AdmittingSpecialtyCluster` on an admission request prior to triggering the bed allocation solver. Once a bed is assigned, it is held in `status = BED_ALLOCATED` as a **Tentative Bed Allocation** (reservation), permitting dynamic reallocation for hospital capacity balancing or diversion referral handoff prior to physical patient arrival.

**Blocked by:** 05: Consensus Completion Gate, Safety-First Discordance & BMU Queue Dispatch

**Status:** completed

- [x] `POST /api/v1/bmu/requests/{id}/admitting-cluster` allows BMU coordinators to select and save the authoritative `admittingSpecialtyCluster` based on multi-disciplinary consult impressions.
- [x] Bed allocation solver consumes the assigned `admittingSpecialtyCluster` to match candidate wards alongside `requestedWardClass` and `effectiveTelemetry`.
- [x] Bed allocation transitions the request to `status = BED_ALLOCATED` and associates the selected `Bed`, marking it as a Tentative Bed Allocation.
- [x] BMU UI permits reallocating a tentatively allocated bed to a different available bed if capacity optimization or clinical updates require it.
- [x] Admission dossiers with `diversionPathway != NONE` display 1-click BMU actions to trigger the sister hospital or MIC@Home referral handoff (linking to Epic 2).
- [x] Audit logs emit `ASSIGN_ADMITTING_CLUSTER` and bed allocation events with modifying coordinator identity.
- [x] Comprehensive tests verify admitting cluster assignment, bed solver constraint satisfaction, and dynamic reallocation workflows.
