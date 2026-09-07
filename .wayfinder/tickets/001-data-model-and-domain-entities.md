# 001: Data Model & Domain Entities (ADR-001)

- **Type**: `wayfinder:grilling`
- **Status**: `closed`
- **Assignee**: `antigravity`
- **Blocked by**: none
- **Blocks**: [004-api-surface-and-contract-design.md](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/004-api-surface-and-contract-design.md), [005-tracer-bullet-1-ed-to-bmu-architecture.md](file:///Users/tjunrong/Documents/playground/enhance-hospital-admissions/.wayfinder/tickets/005-tracer-bullet-1-ed-to-bmu-architecture.md)

## Question

What is the exact relational domain model and Spring Data JPA entity schema for:

1. `Patient` (demographics, gender, infection status, requested Ward Class A/B1/B2/C, ADL/mobility, fall risk)
2. `AdmissionRequest` (acuity tiers 1–5, primary ED assessment, specialist consult assessments, effective BMU tier, queue timestamps)
3. `AssessmentBroadcast` (service cluster, targeted on-call roster, claimed specialist, SLA timeout timer, discordance flag)
4. `Bed` (bed number, current state: `WHITE`, `GREEN`, `GREY`, equipment capabilities: telemetry, negative pressure)
5. `Ward` (ward code, level/floor number, ward class, gender/infection cohort lock, service cluster, holding ward flag)

## Resolution (ADR-001: Relational Domain Entities & In-Memory H2 Persistence)

### 1. Identity & Relationship Architecture

- **Primary Keys**: `java.util.UUID` with `@GeneratedValue(strategy = GenerationType.UUID)` for globally unique, standards-compliant IDs.
- **JPA Mappings**: Standard bidirectional object graph (`@OneToMany`, `@ManyToOne`, `@OneToOne`) with DTO projections and Jackson `@JsonIgnoreProperties` / `@JsonBackReference` to prevent circular serialization.
- **Hierarchy**: Strict physical hierarchy of **Level $\rightarrow$ Ward $\rightarrow$ Bed** (cubicle entity explicitly eliminated).
- **Database Engine**: In-memory **H2 Database** (`jdbc:h2:mem:hospital_db;DB_CLOSE_DELAY=-1`) with `spring.jpa.hibernate.ddl-auto=create-drop`.
- **Seed Data**: A Spring Boot `CommandLineRunner` (`DataInitializer`) automatically populates levels and wards (e.g. Level 8 Ward 8A, Level 8 Ward 8B, Level 9 Ward 9A), beds in initial `WHITE`/`GREEN`/`GREY` states, and synthetic waiting ED patients upon application boot.

---

### 2. Core Entities & Enums

#### A. Enums

- `WardClass`: `A`, `B1`, `B2`, `C`
- `Gender`: `MALE`, `FEMALE`
- `InfectionStatus`: `NON_INFECTIOUS`, `RESPIRATORY`, `MRSA`
- `BedStatus`: `WHITE`, `GREEN`, `GREY`, `CLEANING_IN_PROGRESS`
- `AcuityTier`: `TIER_1_CRITICAL`, `TIER_2_ACUTE_URGENT`, `TIER_3_ACUTE_STABLE`, `TIER_4_SUBACUTE_DIVERSION`, `TIER_5_SHORT_STAY`
- `AdmissionStatus`: `ASSESSMENT_IN_PROGRESS`, `BED_REQUESTED`, `BED_ALLOCATED`, `IN_TRANSIT`, `ADMITTED`, `DIVERTED_SISTER_HOSPITAL`, `DIVERTED_HAH`, `DISCHARGED`
- `BroadcastStatus`: `OPEN`, `CLAIMED`, `AUTO_ESCALATED`, `COMPLETED`

#### B. Entities Schema

1. `Ward`:
   - `UUID id`
   - `int level` (floor number, e.g., 8)
   - `String wardCode` (e.g., "Ward 8A")
   - `@Enumerated(EnumType.STRING) WardClass wardClass` (`A`, `B1`, `B2`, `C`)
   - `String serviceCluster` (e.g., "CARDIOLOGY", "GENERAL_MEDICINE", "SURGERY")
   - `boolean isHoldingWard` (flag for dynamic holding ward designation)
   - `@Enumerated(EnumType.STRING) Gender lockedGender` (MALE, FEMALE, or null if all-White flex)
   - `@Enumerated(EnumType.STRING) InfectionStatus lockedInfectionStatus` (null if all-White flex)
   - `@OneToMany(mappedBy = "ward", cascade = CascadeType.ALL) List<Bed> beds`

2. **`Bed`**:
   - `UUID id`
   - `String bedNumber` (e.g., "8A-01")
   - `@ManyToOne @JoinColumn(name = "ward_id") Ward ward`
   - `@Enumerated(EnumType.STRING) BedStatus status` (`WHITE`, `GREEN`, `GREY`, `CLEANING_IN_PROGRESS`)
   - `boolean hasTelemetry`
   - `boolean isNegativePressure`
   - `boolean isBariatric`
   - `@OneToOne(mappedBy = "allocatedBed") AdmissionRequest activeAdmission`
   - `LocalDateTime cleaningStartedAt`

4. **`Patient`**:
   - `UUID id`
   - `String nricMasked` (e.g., "SXXXX123A")
   - `String fullName`
   - `@Enumerated(EnumType.STRING) Gender gender`
   - `int age`
   - `@Enumerated(EnumType.STRING) InfectionStatus infectionStatus`
   - `@Enumerated(EnumType.STRING) WardClass requestedWardClass`
   - `int fallRiskScore`
   - `String mobilityStatus`
   - `@OneToMany(mappedBy = "patient") List<AdmissionRequest> admissionRequests`

5. **`AdmissionRequest`**:
   - `UUID id`
   - `@ManyToOne @JoinColumn(name = "patient_id") Patient patient`
   - `@Enumerated(EnumType.STRING) AdmissionStatus status`
   - `@Enumerated(EnumType.STRING) AcuityTier primaryAcuityTier`
   - `@Enumerated(EnumType.STRING) AcuityTier effectiveBmuTier`
   - `String suspectedDiagnosis`
   - `String primaryDoctorName`
   - `String primaryClinicalDirectives`
   - `boolean requiresTelemetry`
   - `boolean requiresNegativePressure`
   - `boolean isDiscordant`
   - `LocalDateTime requestedAt`
   - `LocalDateTime allocatedAt`
   - `LocalDateTime admittedAt`
   - `LocalDateTime dischargedAt`
   - `@OneToOne @JoinColumn(name = "allocated_bed_id") Bed allocatedBed`
   - `@OneToMany(mappedBy = "admissionRequest", cascade = CascadeType.ALL) List<AssessmentBroadcast> broadcasts`
   - `String delayReasonTag`
   - `String publicTrackingToken`

6. **`AssessmentBroadcast`**:
   - `UUID id`
   - `@ManyToOne @JoinColumn(name = "admission_request_id") AdmissionRequest admissionRequest`
   - `String serviceCluster`
   - `@Enumerated(EnumType.STRING) BroadcastStatus status`
   - `String claimedDoctorName`
   - `LocalDateTime broadcastAt`
   - `LocalDateTime claimedAt`
   - `LocalDateTime completedAt`
   - `@Enumerated(EnumType.STRING) AcuityTier specialistAcuityTier`
   - `String specialistSpecialtyRecommendation`
   - `String specialistImpression`
   - `boolean specialistDiversionEndorsed`
