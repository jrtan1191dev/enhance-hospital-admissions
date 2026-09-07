# 006: MVP Tracer Bullet Vertical Slice 2 Architecture (Patient Tracker & Turnover Loop)

- **Type**: `wayfinder:prototype`
- **Status**: `open`
- **Assignee**: `unassigned`
- **Blocked by**: [005-tracer-bullet-1-ed-to-bmu-architecture.md](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/005-tracer-bullet-1-ed-to-bmu-architecture.md)
- **Blocks**: none

## Question

How does Tracer Bullet 2 complete the circular hospital lifecycle?
1. Public Mobile View: Dispatch-activated token-based tracker showing Milestone progression, estimated wait, and pax in queue.
2. Ward Nurse UI: "Patient Vacated" button triggering bed status change from `Grey` $\rightarrow$ "Turnover Cleaning In Progress".
3. Housekeeping Mobile UI: 30-minute cleaning SLA countdown timer and "Terminal Cleaning Complete" sign-off flipping bed status back to `White` for immediate BMU re-allocation.
