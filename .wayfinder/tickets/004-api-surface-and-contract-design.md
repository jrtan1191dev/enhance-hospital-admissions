# 004: API Surface & Contract Design (REST Endpoints)

- **Type**: `wayfinder:grilling`
- **Status**: `open`
- **Assignee**: `unassigned`
- **Blocked by**: none (previously 001, 003 — both closed)
- **Blocks**: [005-tracer-bullet-1-ed-to-bmu-architecture.md](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/005-tracer-bullet-1-ed-to-bmu-architecture.md)

## Question

What are the explicit RESTful endpoints, DTO payloads, and status codes for:
1. ED Assessment & Broadcast: `POST /api/assessments/primary`, `GET /api/broadcasts/feed`, `POST /api/broadcasts/{id}/claim`, `POST /api/assessments/consult`
2. BMU Allocation & Batching: `GET /api/bmu/queue`, `GET /api/bmu/beds/recommendations/{requestId}`, `POST /api/bmu/allocations/approve`, `POST /api/bmu/batch-holding-rooms/approve`, `POST /api/bmu/cohort-swap`
3. Patient Milestone Tracker: `GET /api/public/queue-status/{token}`
4. Inpatient & Turnover: `POST /api/inpatient/vacate`, `POST /api/housekeeping/signoff`
