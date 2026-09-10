# 01: Multi-Day Runway & Potential Discharge Indicator (D-2 / D-3) with BMU Capacity Forecast

**What to build:**
Enable inpatient clinicians to establish and maintain an advance discharge runway 48 to 72 hours ahead of patient departure. Attending physicians and ward charge nurses can record and update an Estimated Date of Discharge (EDD) paired with a clinical confidence rating (`HIGH`, `MEDIUM`, `LOW`) and clinical rationale during morning ward rounds via `POST /api/v1/ward/patients/{patientId}/edd`. The system dynamically evaluates multi-day runway stages (`RUNWAY_D3`, `RUNWAY_D2`, `RUNWAY_D1`, `READY_FOR_MORNING_SIGNOFF`) via `GET /api/v1/ward/runway`, highlighting imminent departures with D-2 and D-3 Potential Discharge Indicator badges across the ward bed roster. In the BMU interface, coordinators view aggregate 24 to 72-hour discharge capacity projections grouped by ward and specialty cluster. On the public patient mobile tracker, the planned EDD is displayed so caregivers have clear discharge expectations. Every EDD change emits a structured `RECORD_EDD` audit log event (KPI 20).

**Blocked by:** None (can start immediately)

**Status:** completed

- [x] `POST /api/v1/ward/patients/{patientId}/edd` sets or updates `edd`, `eddConfidence`, and optional rationale on `AdmissionRequest`, validating confidence values (`HIGH`, `MEDIUM`, `LOW`).
- [x] Setting or updating an EDD emits a structured `RECORD_EDD` audit log event capturing patient ID, target EDD, confidence rating, and runway stage (KPI 20).
- [x] `GET /api/v1/ward/runway` computes active discharge runway entries across wards, dynamically calculating stages (`RUNWAY_D3`, `RUNWAY_D2`, `RUNWAY_D1`, `READY_FOR_MORNING_SIGNOFF`).
- [x] Ward console bed roster highlights patients approaching discharge with prominent D-2 and D-3 Potential Discharge Indicator badges.
- [x] BMU interface provides upcoming discharge capacity forecasts 24 to 72 hours out categorized by ward and specialty cluster.
- [x] Public patient mobile milestone tracker displays the planned Estimated Date of Discharge to patients and families.
- [x] Automated tests verify EDD updates, dynamic runway stage calculation, BMU capacity projection aggregations, and structured audit log emissions.
