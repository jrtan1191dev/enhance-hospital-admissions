# 04: Acuity-Driven SLA Countdown Timers & Cluster-Scoped Auto-Escalation

**What to build:**
Display live SLA countdown timers on active broadcast feeds based on patient acuity (15 minutes for Tiers 1–2; 30 minutes for Tiers 3–5). A backend scheduled evaluator (`@Scheduled`) detects unclaimed broadcasts exceeding their SLA threshold, transitions them to `AUTO_ESCALATED`, assigns them strictly to the designated default on-call specialist for that targeted cluster, and triggers high-priority visual notifications and audit logging.

**Blocked by:** 02: Consult-Gated Admission & Multi-Cluster Broadcast Pool with Atomic Claiming

**Status:** ready-for-agent

- [ ] Frontend displays dynamic countdown timers and overdue badges on active broadcast cards based on `primaryAcuityTier` SLA thresholds.
- [ ] Backend defines cluster-to-default-specialist mappings (Cardiology $\rightarrow$ `dr_lim_cardio`, General Medicine $\rightarrow$ `dr_tan_genmed`, Surgery $\rightarrow$ `dr_kumar_surg`, Orthopaedics $\rightarrow$ `dr_lee_ortho`).
- [ ] Background job (`@Scheduled(fixedRate = 30000)`) scans for `OPEN` broadcasts exceeding their acuity-based SLA duration (`Tier 1-2: 15m`, `Tier 3-5: 30m`).
- [ ] Overdue broadcasts atomically transition to `status = AUTO_ESCALATED` and assign `claimedBySpecialistId` to the cluster's designated default lead.
- [ ] Auto-escalation emits an `AUTO_ESCALATE_BROADCAST` audit log with MDC metadata.
- [ ] Escalated cases surface high-priority alert banners on the default specialist's dashboard and ED tracking board.
- [ ] Unit and scheduled integration tests verify SLA computation, auto-assignment accuracy, and cluster isolation.
