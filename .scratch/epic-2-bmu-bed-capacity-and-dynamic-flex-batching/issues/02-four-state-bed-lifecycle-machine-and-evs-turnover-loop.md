# 02: Four-State Bed Lifecycle Machine & EVS Turnover Loop

**What to build:**
Implement the deterministic four-state bed machine (`Mustard Yellow` / `White` / `Green` / `Grey`) across ward admissions and turnover. When BMU approves an allocation, an available `EMPTY_CLEANED` (`White`) bed transitions immediately to `EMPTY_ASSIGNED` (`Green`), locking it against concurrent allocations. If an allocation is cancelled before physical patient arrival, Deallocation reverts the bed directly back to `EMPTY_CLEANED` (`White`) without housekeeping involvement. Ward nurses can confirm physical patient arrival, transitioning the bed from `EMPTY_ASSIGNED` (`Green`) to `OCCUPIED_TAKEN` (`Grey`) and advancing admission status to `ADMITTED_INPATIENT`. Upon patient discharge, the nurse marks the bed vacated, transitioning it to `EMPTY_PENDING_CLEANING` (`Mustard Yellow`) and kicking off an urgent turnover task with a 30-minute cleaning SLA timer on the Environmental Services (EVS) housekeeping board. Housekeeping sign-off transitions the bed from `EMPTY_PENDING_CLEANING` to `EMPTY_CLEANED` (`White`), triggering an All-Clean Ward Reset (releasing `lockedGender`, `lockedInfectionStatus`, and `isHoldingWard`) strictly when all beds in that ward reach `White`.

**Blocked by:** 01: Two-Tier Constraint Validation & Override Workflow

**Status:** ready-for-agent

- [ ] Bed entities strictly enforce state transitions: `EMPTY_CLEANED` -> `EMPTY_ASSIGNED` -> `OCCUPIED_TAKEN` -> `EMPTY_PENDING_CLEANING` -> `EMPTY_CLEANED`.
- [ ] Deallocation API reverts an `EMPTY_ASSIGNED` bed directly to `EMPTY_CLEANED` if patient has not physically arrived, resetting the admission request to `BED_REQUESTED`.
- [ ] Ward console provides an arrival confirmation action that transitions the bed to `OCCUPIED_TAKEN` (`Grey`) and admission status to `ADMITTED_INPATIENT`.
- [ ] Ward console provides a patient discharge/vacate action that transitions the bed from `OCCUPIED_TAKEN` to `EMPTY_PENDING_CLEANING` (`Mustard Yellow`) and sets `cleaningStartedAt`.
- [ ] Beds in `EMPTY_PENDING_CLEANING` (`Mustard Yellow`) are excluded from recommendation candidate pools.
- [ ] Housekeeping terminal clean sign-off transitions the bed from `EMPTY_PENDING_CLEANING` to `EMPTY_CLEANED` (`White`), records `lastCleanedAt`, and logs elapsed turnover time against the 30-minute SLA.
- [ ] An All-Clean Ward Reset is executed automatically upon clean sign-off if all beds in the ward are `EMPTY_CLEANED`, clearing `lockedGender`, `lockedInfectionStatus`, and `isHoldingWard`.
- [ ] Ward and BMU UI boards reflect bed states with distinct color coding (`Mustard Yellow`, `White`, `Green`, `Grey`) and live turnover countdown timers.
- [ ] Comprehensive integration tests verify lifecycle transitions, invalid transition rejections, deallocation, and All-Clean Ward Reset triggers.
