# 006: MVP Tracer Bullet Vertical Slice 2 Architecture (Patient Tracker & Turnover Loop)

- **Type**: `wayfinder:prototype`
- **Status**: `closed`
- **Assignee**: `antigravity`
- **Blocked by**: none (previously 005 — closed)
- **Blocks**: none

## Question

How does Tracer Bullet 2 complete the circular hospital lifecycle?

1. Public Mobile View: Dispatch-activated token-based tracker showing Milestone progression, estimated wait, and pax in queue.
2. Ward Nurse UI: "Patient Vacated" button triggering bed status change from `Grey` $\rightarrow$ `Mustard Yellow` ("Vacated, Pending Cleaning").
3. Housekeeping Mobile UI: 30-minute cleaning SLA countdown timer and "Terminal Cleaning Complete" sign-off flipping bed status from `Mustard Yellow` back to `White` ("empty, cleaned") for immediate BMU re-allocation.

## Resolution (ADR-006: MVP Tracer Bullet Vertical Slice 2 Architecture)

### 1. Public Patient View (`/patient`)

- **Mobile Smartphone Simulator Frame**: Rendered in a clean mobile frame with an evaluator quick-selector dropdown at the top to test any active patient's tracking view with 1 click (no manual token copy-pasting required).
- **Milestone Progression (Stages 1–4)**:
  - `Milestone 1: Admission Decision Confirmed & Bed Queued`
  - `Milestone 2: Bed Assigned & Preparing Room`
  - `Milestone 3: Transfer to Inpatient Ward in Progress`
  - `Milestone 4: Admitted to Inpatient Ward Bed`
- **Transparency Metrics**: Displays estimated wait duration in minutes, number of patients ahead in matching ward category, and transparent nurse/BMU delay reason notes (e.g. specialized isolation terminal cleaning in progress).
- **FYI Care & Financial Explainer Card**: Displays subsidy co-pay estimates, step-down rehabilitation timeline (if diversion candidate), and 1-click hotline contact to Medical Social Work (MSW).

---

### 2. Inpatient Ward Nurse & Housekeeping Workflow (`/ward`)

- **Ward Nurse View**:
  - Bed roster displays admitted and in-transit patients.
  - "Check-In Patient": Confirms physical arrival at bed $\rightarrow$ bed flips from `EMPTY_ASSIGNED` (`GREEN`) to `OCCUPIED_TAKEN` (`GREY`).
  - "Patient Vacated": Marks morning discharge completion $\rightarrow$ calls `POST /api/clinicians/ward/vacate`.
  - Bed status transitions immediately from `OCCUPIED_TAKEN` (`GREY`) $\rightarrow$ `EMPTY_PENDING_CLEANING` (`MUSTARD YELLOW` - vacated, empty, pending clean).
- **Housekeeping / EVS View**:
  - Displays beds currently undergoing turnover with an active 30-minute SLA countdown timer.
  - "Terminal Cleaning Complete & Inspected": EVS specialist submits clean sign-off $\rightarrow$ calls `POST /api/bmu/beds/{id}/clean`.
  - Bed status immediately transitions from `EMPTY_PENDING_CLEANING` (`MUSTARD YELLOW`) back to **`EMPTY_CLEANED` (`WHITE` - empty, cleaned)**.
  - BMU allocation algorithms immediately detect the new `EMPTY_CLEANED` bed in real time for the next waiting ED patient.

---

### 3. Circular Lifecycle Completion

Tracer Bullet 2 connects seamlessly with Tracer Bullet 1 to complete the full hospital operational loop:
$$\text{ED Assessment (White [empty, cleaned] Bed Selected)} \to \text{BMU Assigned (Green)} \to \text{Ward Check-in (Grey)} \to \text{Patient Vacated (Mustard Yellow - Pending Clean)} \to \text{Housekeeping Sign-off (White - empty, cleaned)}$$

---

### 4. Tracer Bullet 2 KPI Verification & Measurement Trace

Executing Tracer Bullet 2 emits and verifies the patient transparency and turnover operational KPIs:
- **Patient Portal Access Rate**: Emits `TRACK_PATIENT_ACCESS` with `Token` and `MilestoneStep`.
- **2-Hour Periodic Update Delivery**: Emits `DISPATCH_PERIODIC_UPDATE` on active dwell timer milestones.
- **Discharge Before 12:00 PM**: Emits `VACATE_PATIENT` recording the vacancy timestamp and noon compliance.
- **30-Minute Cleaning Turnover SLA**: Emits `CLEAN_BED` recording elapsed cleaning minutes against the 30-minute SLA countdown.

