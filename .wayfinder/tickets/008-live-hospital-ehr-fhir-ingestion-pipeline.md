# 008: Live Hospital EHR & HL7/FHIR Ingestion Pipeline (ADR-008)

- **Type**: `wayfinder:prototype`
- **Status**: `closed`
- **Assignee**: `antigravity`
- **Blocked by**: none
- **Blocks**: none

## Question

How should the clinical baseline data, diagnostic scans, laboratory results (e.g., Troponin, arterial blood gas), triage vitals, and ED encounter admissions flow into the system across prototype and production environments?

## Resolution (ADR-008: Hospital EHR Gateway & HL7/FHIR Ingestion Strategy)

### 1. Gateway Pattern Architecture & Profile Separation

In alignment with our Spring Profile strategy (production-ready default vs. prototype profile isolation), EHR communication is encapsulated behind a clean domain interface:

```java
public interface HospitalEhrGateway {
    AssessmentPrepopDto getPatientClinicalSummary(UUID patientId);
    List<PendingAdmissionOrderDto> fetchPendingAdmissionOrders();
}
```

### 2. Default (Production-Ready) Implementation: `FhirHospitalEhrGateway`

- **Component**: Annotated with `@Component` and active by default (e.g., `@Profile("default")` or `@ConditionalOnMissingBean`).
- **FHIR Standards**: Uses **HAPI FHIR Client (HL7 FHIR R4)** to query hospital EHR systems (e.g., Epic, Cerner, SAP Healthcare):
  - `Patient`: Demographics, biological sex, contact info.
  - `Encounter`: ED intake timestamp, triage acuity score, attending physician.
  - `Observation`: Vital signs (BP, SpO2, HR), fall risk scores, and point-of-care lab values (e.g. Troponin, Lactate).
  - `DiagnosticReport`: Radiology reports (Chest X-Ray, CT scans, 12-lead ECG impressions).
- **Security**: Authenticates via mTLS with hospital EHR integration brokers, using SMART on FHIR / OAuth2 Bearer tokens.

---

### 3. Prototype Implementation (`MockHospitalEhrGateway`)

- **Component**: Annotated with `@Component` and strictly gated under `@Profile("prototype")`.
- **Behavior**:
  - Serves synthetic, deterministic pre-population clinical baselines (`AssessmentPrepopDto`) for simulated ED patients:
    - **P101 (Tan Ah Meng)**: NSTEMI baseline, Troponin 150 ng/L, ECG showing ST-depression, recommended Acuity Tier 2.
    - **P102 (Siti Rahmah)**: Atypical chest pain, normal Troponin, recommended Acuity Tier 3.
    - **P103 (Kowsalya)**: Respiratory distress non-infectious, SpO2 93%, recommended Acuity Tier 3.
    - **P104 (Mdm Lee)**: Mild respiratory infection, recommended Acuity Tier 3.
  - Zero external network dependencies, ensuring 100% reliability and immediate response times during local evaluations and demos.

---

### 4. Extension of Profile Component Matrix

| Component / Interface | `@Profile("prototype")` Implementation | Default (Production-Ready) Implementation |
| :--- | :--- | :--- |
| **`HospitalEhrGateway`** | `MockHospitalEhrGateway` (synthetic baseline DTOs for P101–P104) | `FhirHospitalEhrGateway` (HAPI FHIR R4 client ingesting live EHR resources) |
| **`SisterHospitalGateway`** | `MockSisterHospitalGateway` (synthetic referral IDs & 30-min SLA timer) | `HttpSisterHospitalGateway` (real mTLS REST / FHIR calls) |
| **`DataInitializer`** | Active (Ward 8A, 8B, 9A & P101–P104) | Inactive (patient data from live EHR feeds) |
| **Database** | In-Memory H2 DB (`create-drop`) | Clustered PostgreSQL with managed migrations |
