# 02: Automated Morning Discharge Sign-Off and Bedside Medication Delivery Loop

**What to build:**
Eliminate mid-day outpatient pharmacy exit delays by streamlining morning discharge authorizations and bedside medication handoffs directly within the ward console. Inpatient attending physicians or charge nurses execute morning discharge authorization via a 1-click "Final Discharge Sign-Off" action (`POST /api/v1/ward/patients/{patientId}/discharge-signoff`), which records `discharge_signoff_at`, transitions medication dispensing status to `PACKING_IN_PROGRESS`, and emits structured audit logs `DISCHARGE_SIGNOFF` and `DISPENSE_MEDICATION`. Ward nurses or runners confirm bedside medication delivery via `POST /api/v1/ward/patients/{patientId}/deliver-medication`, transitioning status to `DELIVERED_BEDSIDE`, advancing the runway stage to `READY_TO_VACATE`, and emitting a `DELIVER_BEDSIDE_MEDICATION` audit log (KPI 21). The patient's mobile journey tracker updates immediately to reflect "Medications Received at Bedside — Ready to Vacate".

**Blocked by:** 01: Multi-Day Runway & Potential Discharge Indicator (D-2 / D-3) with BMU Capacity Forecast

**Status:** ready-for-agent

- [ ] `POST /api/v1/ward/patients/{patientId}/discharge-signoff` records `discharge_signoff_at` on `AdmissionRequest` and sets `medication_delivery_status` to `PACKING_IN_PROGRESS`.
- [ ] Morning discharge sign-off emits structured audit events `DISCHARGE_SIGNOFF` (capturing doctor ID, sign-off time, pre-discharge hour) and `DISPENSE_MEDICATION` (capturing target SLA 11:00 AM).
- [ ] `POST /api/v1/ward/patients/{patientId}/deliver-medication` updates `medication_delivery_status` to `DELIVERED_BEDSIDE` and advances runway stage to `READY_TO_VACATE`.
- [ ] Bedside delivery emits structured audit event `DELIVER_BEDSIDE_MEDICATION` capturing patient ID, bed ID, confirmer user ID, and delivery timestamp (KPI 21).
- [ ] Ward console provides 1-click actions for "Final Discharge Sign-Off" and "Confirm Bedside Delivery", showing clear medication status badge progression on the bed card.
- [ ] Public patient mobile tracker reflects "Medications Received at Bedside — Ready to Vacate" once delivery is confirmed.
- [ ] Integration tests verify sign-off execution, medication status progression, stage transitions, and audit trail generation.
