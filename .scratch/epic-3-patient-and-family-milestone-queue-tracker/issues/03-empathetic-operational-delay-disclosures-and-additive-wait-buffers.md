# 03: Empathetic Operational Delay Disclosures and Additive Wait Buffers

**What to build:**
Transform technical bed-management bottlenecks into compassionate, transparent disclosures that alleviate patient anxiety. When BMU coordinators attach operational delay tags to an admission request, the tracking service translates these codes into empathetic, plain-language explanations (such as reassuring families that a specialized room is undergoing mandatory UV disinfection for patient safety) and includes direct contact info for the ward liaison hotline. The service dynamically adds defined operational wait buffers (+20m for housekeeping, +30m for bed shortage, +30m for isolation UV disinfection, +45m for surge trauma events) to the estimated wait duration. When the request advances to bed allocation (`BED_ALLOCATED`), any active delay tags are automatically archived and cleared.

**Blocked by:** 01: Quiescent Triage State, 3-Stage Milestone Stepper, and Token Access Auditing

**Status:** ready-for-agent

- [ ] Standard delay tags (`HOUSEKEEPING_DELAY`, `BED_SHORTAGE`, `SPECIALIZED_ISOLATION_CLEANING`, `SURGE_TRAUMA_EVENT`) map to predefined compassionate disclosures explaining the operational context and safety rationale.
- [ ] Free-text coordinator remarks or `OTHER` delay tags fall back to empathetic clinical coordination copy with liaison contact details.
- [ ] Operational delay buffers (+20 mins for housekeeping, +30 mins for bed shortage/isolation cleaning, +45 mins for trauma surge) are additively included in `estimatedWaitMinutes`.
- [ ] `PatientMilestoneResponse` includes `delayReason` and `delayContactHotline` fields when an operational delay is active.
- [ ] Active delay tags and reasons are automatically archived into `archivedDelayReasonTag` and `archivedOperationalDelayReason` upon advancing to `BED_ALLOCATED`.
- [ ] Mobile tracker interface renders a distinct, empathetic delay card with liaison contact details when a delay is active, and hides it when no delay is present.
- [ ] Integration tests verify delay tag translation, additive wait buffer arithmetic, active delay clearing on bed allocation, and response serialization.
