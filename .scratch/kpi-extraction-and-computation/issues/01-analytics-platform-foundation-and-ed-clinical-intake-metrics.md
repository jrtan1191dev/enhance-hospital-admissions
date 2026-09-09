# 01: Analytics Platform Foundation & ED Clinical Intake Metrics

**What to build:**
Establish the core hospital analytics architecture and the Epic 1 Emergency Department clinical collaboration metrics. In the backend, implement `KpiMetricsService` and `KpiAnalyticsController` exposing `GET /api/v1/analytics/kpis/summary` with temporal query parameters (`startDate`, `endDate`), returning division-by-zero guarded metrics for Epic 1: average and P95 primary ED assessment turnaround times, average specialist claim latency, primary vs. specialist concordance percentage, and total digital bed request volume. In the frontend, introduce the `/analytics` route in `router.tsx` and an "Analytics" navigation tab in `Header.tsx`. Build the executive dashboard layout featuring temporal filter buttons (`All Time`, `Today (Last 24h)`, `Past 7 Days`, custom date inputs), 5-second background polling (`refetchInterval: 5000`), a manual "Refresh Metrics" action, and stat cards for the Clinical Intake & Specialist Collaboration domain with benchmark indicators and status health badges (Green/Amber/Red).

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] `KpiMetricsService` calculates Epic 1 operational metrics: `avgEdTurnaroundMinutes`, `edTurnaroundP95Minutes`, `specialistClaimLatencyAvgMinutes`, `primarySpecialistConcordanceRatePct`, and `digitalBedRequestCount`.
- [ ] All rate, percentage, and average formulas are division-by-zero guarded, returning `0.0` for empty cohorts.
- [ ] `GET /api/v1/analytics/kpis/summary` supports optional ISO-8601 `startDate` and `endDate` query parameters, defaulting to all-time reporting when omitted.
- [ ] Top-level `/analytics` route is registered in `router.tsx` and an "Analytics" navigation item is added to `Header.tsx`.
- [ ] Executive dashboard layout renders temporal filter controls (`All Time`, `Today`, `Past 7 Days`, date pickers) and an active 5-second background polling cycle (`refetchInterval: 5000`) with manual refresh button.
- [ ] Clinical Intake domain section renders executive stat cards showing metric value, units, benchmark targets (e.g., target turnaround < 15 mins), and health status badges.
- [ ] Unit and MockMvc tests verify calculation formulas, 0-division guards, temporal filtering, and JSON schema compliance.
