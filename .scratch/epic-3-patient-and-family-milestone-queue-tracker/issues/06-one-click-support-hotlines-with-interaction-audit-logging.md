# 06: One-Click Support Hotlines with Interaction Audit Logging

**What to build:**
Empower patients and caregivers experiencing financial distress or care concerns to connect directly with hospital support services while auditing patient engagement. Within the financial and care explainer cards on the mobile tracker, caregivers have 1-click action buttons to initiate direct calls to Medical Social Work (MSW) or Hospital Financial Counseling. Clicking either action records the engagement in the backend via an unauthenticated, token-scoped endpoint (`POST /api/v1/patients/track/{token}/actions`), persisting an audit record in `patient_audit_interactions` (`token`, `admission_id`, `action_type`, `created_at`) and emitting structured `CONNECT_MSW_HOTLINE` or `CONNECT_FINANCIAL_COUNSELING` audit logs to support automated KPI 18 extraction.

**Blocked by:** 05: Interactive Financial & Care Explainer with Step-Down Care Benchmarks

**Status:** ready-for-agent

- [ ] Mobile tracker interface renders prominent 1-click hotline action buttons for "Call Medical Social Work (MSW)" and "Call Financial Counseling".
- [ ] Endpoint `POST /api/v1/patients/track/{token}/actions` accepts patient action types (`MSW_CALL`, `FINANCE_CALL`) without requiring user authentication.
- [ ] Action invocations persist interaction records in `patient_audit_interactions` table and emit structured audit events `CONNECT_MSW_HOTLINE` and `CONNECT_FINANCIAL_COUNSELING` (KPI 18).
- [ ] Initiating hotline action triggers device telephone protocol (`tel:`) while logging the audit interaction.
- [ ] Integration tests verify unauthenticated token-action persistence, structured audit logging, and counseling connect metrics extraction.
