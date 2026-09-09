# ADR-009: Separation of Clinical Consults and BMU Admitting Service Placement

## Context
When multi-specialty broadcasts are conducted in parallel (e.g. Cardiology, Surgery, Orthopaedics), forcing doctors to negotiate or arbitrarily select a primary admitting ward cluster during emergency triage creates inter-departmental friction and distracts clinicians from diagnostic care.

## Decision
Clinicians focus strictly on clinical evaluations (acuity ratings, telemetry constraints, clinical directives, and diversion endorsements) across all consulted specialty clusters. 

Once all broadcasts reach `COMPLETED` and the case enters the BMU queue, the **BMU Coordinator** reviews the aggregated clinical impressions and hospital bed availability to select the final **Admitting Specialty Cluster** before triggering bed allocation.
