# ADR-007: Consult-Gated Admission and BMU Queue Entry

## Context
In emergency department operations, bed allocation can either occur concurrently with specialist reviews or be strictly gated upon completing the clinical assessment. Allowing premature allocation before specialist consults can lead to inappropriate bed placements if the specialist later escalates acuity or recommends diversion.

## Decision
Bed allocation in the Bed Management Unit (BMU) is strictly gated on the admission assessment reaching `COMPLETED`:
1. When the ED physician selects a **Direct Admission** (`requiresSpecialistConsult = false`), the assessment is completed immediately upon intake submission and enters the BMU queue in `BED_REQUESTED` status.
2. When the ED physician selects a **Consult-Gated Admission** (`requiresSpecialistConsult = true`), an `AssessmentBroadcast` is published to the specialty cluster pool and the admission request is held in `ASSESSMENT_PENDING`. It is hidden from active BMU allocation until the consulting specialist (or SLA default designee) submits their evaluation, transitioning the broadcast to `COMPLETED` and the admission request to `BED_REQUESTED`.
