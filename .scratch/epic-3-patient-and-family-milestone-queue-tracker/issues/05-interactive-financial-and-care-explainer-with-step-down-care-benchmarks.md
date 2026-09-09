# 05: Interactive Financial & Care Explainer with Step-Down Care Benchmarks

**What to build:**
Relieve patient and caregiver anxiety regarding hospitalization costs and alternative care pathways through proactive, non-intimidating "FYI Insights". Based on the patient's requested ward subsidy class (`CLASS_A`, `CLASS_B1`, `CLASS_B2`, `CLASS_C`), the tracker provides estimated daily out-of-pocket co-pays, means-tested government subsidy percentages (e.g., up to 70% for Class B2/C), and MediShield Life coverage estimates, alongside a clear reassurance statement that figures are informational estimates requiring no upfront deposits or signatures. When a patient is recommended for care diversion (`diversionRecommended == true`), the explainer displays step-down care guidance: Community Hospital rehabilitation benchmarks (14 to 21-day average stay, daily sub-acute costs) or Mobile Inpatient Care at Home (MIC@Home) virtual ward details (visiting nurse schedules, remote vitals monitoring, home equipment delivery).

**Blocked by:** 01: Quiescent Triage State, 3-Stage Milestone Stepper, and Token Access Auditing

**Status:** ready-for-agent

- [ ] `PatientMilestoneResponse` returns structured financial insights and care guidance based on `requestedWardClass`, `diversionRecommended`, and `diversionPathway`.
- [ ] Financial advisory card displays daily out-of-pocket co-pay estimates, government subsidy tiers (up to 70% for Class B2/C), and MediShield Life applicability.
- [ ] Interface prominently displays an informational peace-of-mind disclaimer ("FYI Insights") clarifying that estimates do not require upfront deposits or digital signatures.
- [ ] For patients recommended for Community Hospital transfer, the explainer displays rehabilitation length of stay benchmarks (14–21 days) and sub-acute cost expectations.
- [ ] For patients recommended for MIC@Home, the explainer outlines virtual ward monitoring, home equipment delivery, and visiting nurse schedules.
- [ ] Unit and component tests verify correct subsidy calculations, text copy per ward class, and conditional rendering of step-down care pathways.
