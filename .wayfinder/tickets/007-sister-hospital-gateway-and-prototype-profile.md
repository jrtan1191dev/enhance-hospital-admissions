# 007: Sister Hospital Gateway & Prototype Profile Architecture (ADR-007)

- **Type**: `wayfinder:prototype`
- **Status**: `closed`
- **Assignee**: `antigravity`
- **Blocked by**: none
- **Blocks**: none

## Question

How should external Sister Community Hospital (OCH, AH, SACH) and MIC@Home (Hospital-at-Home) diversion integrations be architected across prototype vs. production environments? How should Spring profiles govern mocked components, synthetic seed datasets, and future production service implementations?

## Resolution (ADR-007: Sister Hospital Gateway & Spring Profile Boundary Architecture)

### 1. Spring Profile Strategy: Production-Ready Default vs. Prototype Profile Isolation

To maintain clean separation of concerns, high maintainability, and zero risk of synthetic mock data or mock behaviors leaking into live environments, the application is **production-ready by default**:

- **Default Profile (`default`) — Production-Ready**:
  - Active whenever the application runs without explicitly overriding profiles, or in live hospital environments.
  - Connects to enterprise persistent PostgreSQL database.
  - `DataInitializer` seed runner is completely inactive; patient data originates exclusively from hospital EHR/ADT feeds.
  - Wires production HTTP adapters (`HttpSisterHospitalGateway`) that execute authenticated mTLS/OAuth2 REST or FHIR calls against real cluster endpoints.
  - Core domain services (`AllocationService`, `HeuristicEngine`, `AssessmentService`) depend solely on gateway interfaces, ensuring zero business logic modification between environments.
- **`prototype` Profile (`@Profile("prototype")`) — Strictly Isolated Prototype Scope**:
  - All prototype decisions, constraints, synthetic datasets, and mocked integration behaviors discussed to date are **strictly and exclusively bound to the `prototype` profile**.
  - Must be explicitly activated (e.g. `spring.profiles.active=prototype`) for local prototype evaluation, interactive reviewer walkthroughs, and automated integration tests.
  - Activates in-memory H2 database (`jdbc:h2:mem:hospital_db;DB_CLOSE_DELAY=-1`) with `create-drop`.
  - Activates `DataInitializer` `CommandLineRunner` to seed synthetic wards, beds, and waiting ED patients.
  - Injects mocked external integration gateways (`MockSisterHospitalGateway`).
  - Enables simulated latency and automatic bilateral SLA timers.
- **`dev` and `qa` Profiles (Separate Staging / Testing Environments)**:
  - Reserved for future staging and QA environments where specific configurations and data sources will be defined later.
  - **Strictly decoupled from `prototype`**: `dev` and `qa` serve completely different operational intentions and will **not** inherit or mix with prototype mock data, in-memory H2, or prototype-specific shortcuts.

---

### 2. Sister Hospital Gateway Contract & Mock Implementation

#### Interface (`SisterHospitalGateway`)
```java
public interface SisterHospitalGateway {
    TransferDispatchResult dispatchReferral(UUID admissionRequestId, String targetHospitalCode, String clinicalNotes);
    TransferStatusDto checkTransferStatus(String externalReferralId);
}
```

#### Prototype Implementation (`MockSisterHospitalGateway`)
- Annotated with `@Component` and `@Profile("prototype")`.
- Simulates supported external transfer destinations:
  - `OCH`: Outram Community Hospital (Subacute step-down & rehabilitation)
  - `AH`: Alexandra Hospital (Subacute convalescent care)
  - `SACH`: St. Andrew's Community Hospital (Subacute rehabilitation)
  - `MIC_AT_HOME`: Mobile Inpatient Care at Home (Virtual acute ward)
- Generates synthetic external referral IDs (e.g. `OCH-REF-2026-9104`).
- Simulates successful electronic transmission and bilateral acceptance.
- Initiates the 30-minute bilateral SLA countdown timer.
- Updates `AdmissionRequest.status` to `DIVERTED_SISTER_HOSPITAL` or `DIVERTED_HAH`.
- Emits diversion payload to the Patient Admission Tracker, rendering the rehabilitation care explainer card.

#### Production-Ready Default Implementation (`HttpSisterHospitalGateway`)
- Annotated with `@Component` and `@Profile("default")` (or `@Profile("!prototype")`).
- Default enterprise adapter calling real Sister Hospital EHR APIs or national FHIR referral endpoints using Spring `RestClient` / `WebClient` with mTLS certificates, OAuth2 Bearer tokens, and circuit-breaker resilience (Resilience4j).

---

### 3. Extension of `@Profile("prototype")` Across All Mocked Boundaries

| Component / Responsibility | `@Profile("prototype")` Implementation | Default (Production-Ready) Implementation |
| :--- | :--- | :--- |
| **Sister Hospital Integration** | `MockSisterHospitalGateway` (simulated referral IDs & SLA timers) | `HttpSisterHospitalGateway` (real mTLS REST / FHIR calls) |
| **Database & Persistence** | In-Memory H2 DB (`create-drop`) | Clustered PostgreSQL with Flyway / Liquibase migrations |
| **Seed Dataset** | `DataInitializer` (Ward 8A/8B/9A + P101–P104) | Disabled (live patient records via EHR ADT A01/A02 feeds) |
| **Diagnostic Scan Synthesis** | Synthetic pre-population DTOs | Real hospital PACS / LIS / EHR diagnostic report ingestion |
| **Patient Notification Push** | In-app polling & state reflection | Real SMS / WhatsApp / Singpass notification dispatch |
