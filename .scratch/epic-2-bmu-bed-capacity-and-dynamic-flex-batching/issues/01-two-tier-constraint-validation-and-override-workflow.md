# 01: Two-Tier Constraint Validation & Override Workflow

**What to build:**
Enforce the Two-Tier Constraint Hierarchy during bed allocation recommendation and approval. In bed matching and allocation workflows, Absolute Safety Invariants (biological gender cohorting in multi-bed wards, and negative pressure airborne isolation) are strictly non-overridable; any manual or automated attempt to place a patient violating these invariants is rejected with RFC 7807 `422 Unprocessable Entity`. Overridable Operational Constraints (ward class subsidy upgrades and static continuous telemetry equipping) allow BMU coordinators to allocate alternative beds only when accompanied by a mandatory structured reason code (`GOVERNMENT_SUBSIDY_CLASS_UPGRADE`, `EMERGENCY_PORTABLE_TELEMETRY_DEPLOYED`), rejecting unreasoned overrides and generating structured `OVERRIDE_ALLOCATION` audit log records. In the BMU queue interface, recommended candidates show Top 3 match rationale chips (+40 specialty cluster alignment, +30 consolidation packing bonus, +15 fall-risk station proximity), safety-violated beds are disabled with clinical explanations, and selecting operational override candidates triggers a structured reason modal.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Bed allocation recommendation engine evaluates candidate beds against hard constraints and computes multi-criteria scores (+40 specialty cluster match, +30 consolidation packing bonus, +15 proximity for fall risk >= 45).
- [ ] Absolute Safety Invariants (biological gender cohorting in multi-bed wards and negative pressure isolation for airborne infections) are strictly non-overridable; allocation attempts violating these invariants return RFC 7807 `422 Unprocessable Entity` with clinical safety violation details.
- [ ] Overridable Operational Constraints (financial ward class subsidy upgrade and deploying portable telemetry to non-equipped beds) permit allocation only when a valid structured reason code is provided.
- [ ] Attempting an operational constraint override without a valid reason code returns RFC 7807 `400 Bad Request`.
- [ ] Normal bed allocations emit `ALLOCATE_BED` audit log events; overrides emit `OVERRIDE_ALLOCATION` audit events capturing coordinator username, assigned bed, rank, and structured reason code.
- [ ] BMU recommendation UI displays the Top 3 ranked candidate beds with detailed score breakdown chips.
- [ ] BMU UI allows 1-click approval for recommended beds, and provides an override modal requiring structured reason selection when choosing non-recommended or operational-override beds.
- [ ] Automated integration tests verify invariant rejection (422), reason-code validation, scoring weights, and audit log emissions.
