# 009: Enterprise Security, RBAC & IM8 Audit Logging Architecture (ADR-009)

- **Type**: `wayfinder:prototype`
- **Status**: `closed`
- **Assignee**: `antigravity`
- **Blocked by**: none
- **Blocks**: none

## Question

How should authentication, role-based authorization (RBAC), user identities, and IM8/ARC-compliant audit logging be architected across prototype and production environments? How do we provide zero-friction topbar role-switching for prototype evaluators while maintaining real security contexts and actual structured audit log statements behind the scenes?

## Resolution (ADR-009: Seeded Security Context, Header-Driven Prototype Authentication & Structured IM8 Audit Logging)

### 1. Dual Security Architecture (Default vs. Prototype Profile)

Security is architected so that the Spring `SecurityContextHolder` is **always populated with a valid authenticated principal and authorities**, ensuring that domain business logic and authorization checks (`@PreAuthorize`) operate identically across all environments:

- **Default (Production-Ready) Configuration**:
  - Configures Spring Security with `OAuth2ResourceServer` validating JWT tokens issued by Hospital Active Directory / Keycloak for healthcare professionals, and Singpass/HealthHub OIDC for public patient tracking.
  - Role-based method security (`@PreAuthorize("hasRole('ED_ATTENDING')")`, `@PreAuthorize("hasRole('BMU_COORDINATOR')")`) strictly checks verified token scopes.
- **`prototype` Profile (`@Profile("prototype")`)**:
  - `PrototypeSecurityFilter` intercepts incoming HTTP requests.
  - Evaluates the `X-User-Role` / `X-User-Id` request header sent by the frontend's topbar role switcher.
  - Automatically loads the corresponding seeded user account into Spring's `SecurityContextHolder` with pre-assigned roles and clinical metadata.
  - Allows zero-friction 1-click persona switching in the UI with zero login prompts, while providing genuine authenticated identities to the application layer.

---

### 2. Seeded Prototype User Accounts

The prototype seeds realistic clinical user accounts mapped to topbar personas:

| Topbar Persona | Seeded Username | Display Name & Title | Assigned Roles / Authorities | Clinical Scope |
| :--- | :--- | :--- | :--- | :--- |
| **`[🩺 ED Attending]`** | `dr_tan_ed` | Dr. Tan Ah Teck (Senior Consultant) | `ROLE_ED_ATTENDING`, `ROLE_CLINICIAN` | Emergency Medicine Intake |
| **`[👨‍⚕️ Specialist]`** | `dr_lim_cardio` | Dr. Lim Wei Ling (Consultant Cardiologist) | `ROLE_SPECIALIST`, `ROLE_CLINICIAN` | Cardiology Inpatient Service |
| **`[🏢 BMU Coordinator]`** | `bmu_coord_wong` | Wong Mei Ling (Bed Management Lead) | `ROLE_BMU_COORDINATOR`, `ROLE_OPERATIONS` | Hospital-wide Bed Orchestration |
| **`[📱 Patient Admission Tracker]`** | `patient_p101` | Tan Ah Meng (Admitted Patient) | `ROLE_PATIENT` | Token-gated Personal Queue View |
| **`[🧹 Ward & EVS]`** | `nurse_sarah` | Nurse Sarah / Housekeeper Devan | `ROLE_WARD_NURSE`, `ROLE_HOUSEKEEPING` | Ward 8A Bed Operations & Turnover |

---

### 3. Structured IM8 Audit Logging via Actual Log Statements

Rather than using an ephemeral in-memory list, audit trails are written as **actual structured log statements** via SLF4J / Logback:

1. **Audit Interceptor (`AuditLoggingAspect` / `AuditLogService`)**:
   - Intercepts state-altering clinical and operational actions (admission submission, specialist consult endorsement, bed allocation approval, allocation override, patient vacate, and cleaning sign-off).
   - Extracts the authenticated username from `SecurityContextHolder.getContext().getAuthentication()`.
   - Populates Mapped Diagnostic Context (MDC) with `userId`, `userRole`, `clientIp`, `requestId`.
2. **Structured Log Statement Format**:

   ```
   2026-09-08 09:40:12.450 INFO  [c.h.security.AuditLogger] - [AUDIT] user="dr_tan_ed" role="ROLE_ED_ATTENDING" action="SUBMIT_PRIMARY_ASSESSMENT" target="P101" acuity="TIER_2" diagnosis="NSTEMI"
   2026-09-08 09:41:05.120 INFO  [c.h.security.AuditLogger] - [AUDIT] user="dr_lim_cardio" role="ROLE_SPECIALIST" action="CLAIM_BROADCAST" target="REQ-101" cluster="CARDIOLOGY"
   2026-09-08 09:42:30.880 INFO  [c.h.security.AuditLogger] - [AUDIT] user="bmu_coord_wong" role="ROLE_BMU_COORDINATOR" action="APPROVE_BED_ALLOCATION" target="REQ-101" bed="8A-04" score=70
   2026-09-08 09:43:15.910 WARN  [c.h.security.AuditLogger] - [AUDIT] user="bmu_coord_wong" role="ROLE_BMU_COORDINATOR" action="OVERRIDE_ALLOCATION" target="REQ-102" selectedBed="8A-01" recommendedBed="9A-02" reason="SPECIALIST_DIRECTIVE_OVERRIDE"
   ```

3. **IM8 Compliance & Enterprise SIEM Integration**:
   - In production, log appenders ship these audit events directly to immutable enterprise log stores (e.g. AWS CloudWatch / OpenSearch / Splunk) with write-once-read-many (WORM) storage for regulatory non-repudiation.
   - In the prototype, these statements output to standard output / application logs, allowing evaluators and developers to inspect authentic audit events directly in the console.

---

### 4. Operational KPI Instrumentation & Automated Extraction

All 21 operational KPIs defined in `pain-points.md` and the four Epic specifications are instrumented directly into the structured audit logging pipeline. Each event provides machine-parseable key-value pairs in the `details` string:

| Operational KPI (pain-points.md) | Emitted Audit Action | Event Details Payload & Metrics Captured |
| :--- | :--- | :--- |
| **Primary ED Turnaround Time** | `SUBMIT_ED_ASSESSMENT` | `PrimaryAcuity`, `WardClass`, `ElapsedMins` |
| **Specialist Pick-Up Latency** | `CLAIM_BROADCAST` | `TargetCluster`, `Specialist`, `ElapsedClaimMins` |
| **Primary vs Specialist Concordance** | `SUBMIT_SPECIALIST_CONSULT` | `PrimaryAcuity`, `SecondaryAcuity`, `Concordant={true\|false}` |
| **BMU Suggestion Acceptance Rate** | `ALLOCATE_BED` / `OVERRIDE_ALLOCATION` | `AssignedBed`, `Score`, `Override={true\|false}`, `OverrideReason` |
| **Sister Hospital Diversion Rate** | `DIVERSION_REFERRAL` | `Facility`, `ReferralId`, `SlaWindowMins=30` |
| **Batch Holding Ward Adoption** | `APPROVE_BATCH_HOLDING_WARD` | `BatchSize`, `PatientIds`, `WardClass`, `Gender` |
| **Cohort-Swap Optimization Yield** | `APPROVE_COHORT_SWAP` | `ReassignedPatient`, `FromBed`, `ToBed`, `UnlockedWard` |
| **Prolonged-Wait Tagging Rate** | `TAG_DELAY_REASON` | `DelayCode`, `DwellMins`, `AcuityTier` |
| **Patient Portal Access Rate** | `TRACK_PATIENT_ACCESS` | `PatientId`, `MilestoneStep`, `EstWaitMins` |
| **2-Hour Periodic Update Delivery** | `DISPATCH_PERIODIC_UPDATE` | `Channel=SMS_PUSH`, `Milestone`, `DeliveryStatus=SUCCESS` |
| **Discharge Before 12:00 PM** | `VACATE_PATIENT` | `BedNumber`, `VacateHour`, `DischargedBeforeNoon={true\|false}` |
| **Bed Turnover Cleaning Latency** | `CLEAN_BED` | `BedNumber`, `ElapsedCleaningMins`, `Within30mSla={true\|false}` |

#### Fast Log Extraction Script Example
```bash
# Calculate 30-Minute Cleaning SLA Compliance directly from logs:
grep 'action="CLEAN_BED"' application.log | \
  sed -n 's/.*ElapsedCleaningMins=\([0-9.]*\).*/\1/p' | \
  awk '{sum+=$1; count++; if($1<=30.0) ok++} END {print "Avg Latency:", sum/count, "mins | SLA Compliance:", (ok/count)*100, "%"}'
```

---

### 5. Summary Matrix: Security & Audit Across Profiles

| Capability | `@Profile("prototype")` Implementation | Default (Production-Ready) Implementation |
| :--- | :--- | :--- |
| **Authentication Filter** | `PrototypeSecurityFilter` (maps `X-User-Role` header to seeded accounts) | `OAuth2ResourceServerFilter` (validates signed hospital JWTs) |
| **User Identity** | Seeded accounts (`dr_tan_ed`, `bmu_coord_wong`, etc.) | Active Directory / Keycloak / Singpass verified identities |
| **Audit Logging** | Real structured log statements with seeded usernames | Real structured log statements shipped to immutable SIEM |
| **Method Authorization** | Spring Security `@PreAuthorize` enabled | Spring Security `@PreAuthorize` enabled |
| **KPI Observability** | Console/Log script KPI extraction (`jq`, `grep`, `awk`) | SIEM / OpenSearch dashboards & metric queries |
