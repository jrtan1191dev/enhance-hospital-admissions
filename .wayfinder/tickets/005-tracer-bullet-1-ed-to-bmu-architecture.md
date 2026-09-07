# 005: MVP Tracer Bullet Vertical Slice 1 Architecture (ED to BMU Allocation)

- **Type**: `wayfinder:prototype`
- **Status**: `open`
- **Assignee**: `unassigned`
- **Blocked by**: [001-data-model-and-domain-entities.md](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/001-data-model-and-domain-entities.md), [002-heuristic-bed-constraint-and-batching-algo.md](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/002-heuristic-bed-constraint-and-batching-algo.md), [004-api-surface-and-contract-design.md](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/004-api-surface-and-contract-design.md)
- **Blocks**: [006-tracer-bullet-2-patient-tracker-and-turnover-loop.md](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/006-tracer-bullet-2-patient-tracker-and-turnover-loop.md)

## Question

What is the bare-minimum executable code skeleton and UI stub structure connecting React to Spring Boot for Tracer Bullet 1?
1. Seed dataset: Synthetic ward layout (Ward 8A with cubicles, `White`/`Green`/`Grey` beds) and simulated waiting ED patients.
2. ED Attending UI: 1-click submit pre-populated assessment.
3. Inpatient Specialist UI: Service-cluster on-call consult feed view.
4. BMU Dashboard UI: Live queue, Top 3 bed recommendation card, and 1-click approval transitioning bed `White` $\rightarrow$ `Green` $\rightarrow$ `Grey`.
