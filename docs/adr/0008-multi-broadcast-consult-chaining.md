# ADR-008: Multi-Broadcast Specialist Consult Chaining and Consensus Gate

## Context
Complex acute emergency patients frequently present with multi-system pathologies requiring input from multiple disciplines (e.g., polytrauma requiring Orthopaedics and General Surgery, or acute coronary syndrome with renal complications). Restricting consults to a single specialty cluster fails to reflect clinical reality and risks uncoordinated inpatient care.

## Decision
1. **Multi-Cluster Broadcasts**: The primary ED attending can select one or more target specialty clusters (`Set<SpecialtyCluster>`), emitting concurrent `AssessmentBroadcast` entities linked to the parent `AdmissionRequest`.
2. **Specialist Chaining**: Consulting specialists can spawn additional broadcast requests to other specialty clusters during their review before finalizing their assessment.
3. **Cluster-Scoped Auto-Escalation**: Unclaimed broadcasts strictly escalate to the designated default on-call specialist of the broadcast's targeted cluster.
4. **Consensus Completion Gate**: The parent `AdmissionRequest` remains in `ASSESSMENT_PENDING` until **all** emitted broadcasts reach `COMPLETED`. Only when all consults are closed does the request calculate overall discordance, elevate `effectiveAcuityTier` to the highest acuity across all respondents, enforce continuous telemetry if flagged by any participant, and transition to `BED_REQUESTED` to enter the active BMU queue.
