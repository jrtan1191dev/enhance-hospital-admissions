# ADR-0010: Two-Tier Bed Allocation Constraint Hierarchy

## Context
During high-volume emergency surges, Bed Management Unit (BMU) coordinators encounter situations where available beds do not meet all patient requirements. Allowing unrestricted manual overrides risks critical patient safety breaches (such as cross-gender cohorting in open wards or exposing vulnerable patients to airborne pathogens). Conversely, making all constraints completely un-overridable paralyzes hospital throughput when legitimate operational adaptations exist (such as administrative ward class subsidy upgrades or deploying portable telemetry packs).

## Decision
The bed allocation engine and allocation API enforce a strict **Two-Tier Constraint Hierarchy**:

1. **Absolute Safety Invariants (Zero Override Allowed)**:
   - Biological gender cohorting across open multi-bed wards.
   - Negative pressure isolation for airborne and droplet infectious diseases.
   Attempting to allocate or override a bed violating an Absolute Safety Invariant is strictly rejected at the API boundary with RFC 7807 `422 Unprocessable Entity`, regardless of user role or supplied reason code.

2. **Overridable Operational Constraints**:
   - Financial ward class subsidy entitlement (e.g., accommodating a Class C subsidized patient in an available Class B2 bed during a capacity crunch).
   - Continuous cardiac telemetry availability on statically unequipped beds (when clinical operations arrange a mobile wireless telemetry transmitter).
   Coordinators may allocate beds that diverge from operational constraints only by supplying a mandatory, structured institutional justification code (`GOVERNMENT_SUBSIDY_CLASS_UPGRADE`, `EMERGENCY_PORTABLE_TELEMETRY_DEPLOYED`), generating an immutable, auditable `OVERRIDE_ALLOCATION` security event.
