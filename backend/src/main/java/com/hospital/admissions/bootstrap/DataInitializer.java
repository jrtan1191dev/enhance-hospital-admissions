package com.hospital.admissions.bootstrap;

import com.hospital.admissions.domain.*;
import com.hospital.admissions.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Profile("prototype")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final WardRepository wardRepository;
    private final BedRepository bedRepository;
    private final PatientRepository patientRepository;
    private final AdmissionRequestRepository admissionRequestRepository;
    private final BmuAlgorithmConfigRepository configRepository;
    private final AssessmentBroadcastRepository broadcastRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("[PROTOTYPE SEEDER] Initializing synthetic hospital dataset for prototype evaluation...");

        // 1. Seed Algorithm Config
        if (configRepository.count() == 0) {
            configRepository.save(BmuAlgorithmConfig.builder()
                    .weightSpecialtyCluster(40)
                    .weightConsolidation(30)
                    .weightFallRiskStation(15)
                    .batchHoldingWardThreshold(3)
                    .build());
        }

        // 2. Seed Inpatient Beds & Wards (Level -> Ward -> Bed)
        if (wardRepository.count() == 0) {
            seedWardsAndInpatients();
        }

        // 3. Seed ED Patients P101 - P109
        if (patientRepository.findByQueueToken("TOKEN-P101").isEmpty()) {
            seedEdPatients();
        }
    }

    // -------------------------------------------------------------------------
    // Ward + Inpatient seeding
    // -------------------------------------------------------------------------

    private void seedWardsAndInpatients() {
        // Existing Inpatients for Occupied Beds
        Patient inp1 = patientRepository.save(Patient.builder()
                .name("Uncle Seng (Inpatient)")
                .nricMasked("S****881E")
                .age(75)
                .gender(Gender.MALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(30)
                .needsTelemetry(true)
                .queueToken("TOKEN-INP-881")
                .build());

        Patient inp2 = patientRepository.save(Patient.builder()
                .name("Mr Muthu (Inpatient)")
                .nricMasked("S****992F")
                .age(62)
                .gender(Gender.MALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(25)
                .needsTelemetry(false)
                .queueToken("TOKEN-INP-992")
                .build());

        Patient inp3 = patientRepository.save(Patient.builder()
                .name("Auntie Mei (Inpatient)")
                .nricMasked("S****773G")
                .age(70)
                .gender(Gender.FEMALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(40)
                .needsTelemetry(false)
                .queueToken("TOKEN-INP-773")
                .build());

        // Inpatient in Surgery ward (occupied, high fall-risk, near station)
        Patient inp4 = patientRepository.save(Patient.builder()
                .name("Mr Rajan (Inpatient, Surgery)")
                .nricMasked("S****441J")
                .age(58)
                .gender(Gender.MALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(50)
                .needsTelemetry(false)
                .queueToken("TOKEN-INP-441")
                .build());

        // Inpatient in Orthopaedics ward (female, Class B1)
        Patient inp5 = patientRepository.save(Patient.builder()
                .name("Mdm Lim (Inpatient, Ortho)")
                .nricMasked("S****558K")
                .age(67)
                .gender(Gender.FEMALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(60)
                .needsTelemetry(false)
                .queueToken("TOKEN-INP-558")
                .build());

        // ---- Ward 8A: Level 8 / Cardiology / Class B2 / Locked Male ----
        Ward ward8A = wardRepository.save(Ward.builder()
                .level(8)
                .name("Ward 8A")
                .wardClass(WardClass.B2)
                .lockedGender(Gender.MALE)
                .serviceCluster(SpecialtyCluster.CARDIOLOGY)
                .capacity(4)
                .build());

        Bed bed8A01 = bedRepository.save(Bed.builder()
                .ward(ward8A)
                .bedNumber("8A-01")
                .status(BedStatus.OCCUPIED_TAKEN)
                .isNearNursingStation(true)
                .hasTelemetry(true)
                .currentPatient(inp1)
                .build());

        Bed bed8A02 = bedRepository.save(Bed.builder()
                .ward(ward8A)
                .bedNumber("8A-02")
                .status(BedStatus.OCCUPIED_TAKEN)
                .isNearNursingStation(false)
                .hasTelemetry(true)
                .currentPatient(inp2)
                .build());

        bedRepository.save(Bed.builder()
                .ward(ward8A)
                .bedNumber("8A-03")
                .status(BedStatus.EMPTY_CLEANED)
                .isNearNursingStation(true)
                .hasTelemetry(true)
                .cleaningStartedAt(LocalDateTime.now().minusHours(2).minusMinutes(22))
                .lastCleanedAt(LocalDateTime.now().minusHours(2))
                .build());

        bedRepository.save(Bed.builder()
                .ward(ward8A)
                .bedNumber("8A-04")
                .status(BedStatus.EMPTY_PENDING_CLEANING)
                .isNearNursingStation(false)
                .hasTelemetry(true)
                .cleaningStartedAt(LocalDateTime.now().minusMinutes(12))
                .build());

        // ---- Ward 8B: Level 8 / Cardiology / Class B2 / Flex Unlocked (all EMPTY_CLEANED for batch suggestions) ----
        Ward ward8B = wardRepository.save(Ward.builder()
                .level(8)
                .name("Ward 8B")
                .wardClass(WardClass.B2)
                .lockedGender(null)
                .serviceCluster(SpecialtyCluster.CARDIOLOGY)
                .capacity(4)
                .build());

        for (int i = 1; i <= 4; i++) {
            bedRepository.save(Bed.builder()
                    .ward(ward8B)
                    .bedNumber("8B-0" + i)
                    .status(BedStatus.EMPTY_CLEANED)
                    .isNearNursingStation(i <= 2)
                    .hasTelemetry(false)
                    .cleaningStartedAt(i == 1 ? LocalDateTime.now().minusHours(3).minusMinutes(25) : null)
                    .lastCleanedAt(LocalDateTime.now().minusHours(3))
                    .build());
        }

        // ---- Ward 9A: Level 9 / General Medicine / Class C / Locked Female ----
        Ward ward9A = wardRepository.save(Ward.builder()
                .level(9)
                .name("Ward 9A")
                .wardClass(WardClass.C)
                .lockedGender(Gender.FEMALE)
                .serviceCluster(SpecialtyCluster.GENERAL_MEDICINE)
                .capacity(2)
                .build());

        Bed bed9A01 = bedRepository.save(Bed.builder()
                .ward(ward9A)
                .bedNumber("9A-01")
                .status(BedStatus.OCCUPIED_TAKEN)
                .isNearNursingStation(true)
                .hasTelemetry(false)
                .currentPatient(inp3)
                .build());

        bedRepository.save(Bed.builder()
                .ward(ward9A)
                .bedNumber("9A-02")
                .status(BedStatus.EMPTY_CLEANED)
                .isNearNursingStation(true)
                .hasTelemetry(false)
                .cleaningStartedAt(LocalDateTime.now().minusHours(4).minusMinutes(20))
                .lastCleanedAt(LocalDateTime.now().minusHours(4))
                .build());

        // ---- Ward 10A: Level 10 / Surgery / Class B2 / Locked Male ----
        Ward ward10A = wardRepository.save(Ward.builder()
                .level(10)
                .name("Ward 10A")
                .wardClass(WardClass.B2)
                .lockedGender(Gender.MALE)
                .serviceCluster(SpecialtyCluster.SURGERY)
                .capacity(3)
                .build());

        Bed bed10A01 = bedRepository.save(Bed.builder()
                .ward(ward10A)
                .bedNumber("10A-01")
                .status(BedStatus.OCCUPIED_TAKEN)
                .isNearNursingStation(true)
                .hasTelemetry(false)
                .currentPatient(inp4)
                .build());

        bedRepository.save(Bed.builder()
                .ward(ward10A)
                .bedNumber("10A-02")
                .status(BedStatus.EMPTY_CLEANED)
                .isNearNursingStation(false)
                .hasTelemetry(false)
                .cleaningStartedAt(LocalDateTime.now().minusHours(1).minusMinutes(10))
                .lastCleanedAt(LocalDateTime.now().minusHours(1))
                .build());

        bedRepository.save(Bed.builder()
                .ward(ward10A)
                .bedNumber("10A-03")
                .status(BedStatus.EMPTY_PENDING_CLEANING)
                .isNearNursingStation(true)
                .hasTelemetry(false)
                .cleaningStartedAt(LocalDateTime.now().minusMinutes(8))
                .build());

        // ---- Ward 10B: Level 10 / Orthopaedics / Class B1 / Locked Female ----
        Ward ward10B = wardRepository.save(Ward.builder()
                .level(10)
                .name("Ward 10B")
                .wardClass(WardClass.B1)
                .lockedGender(Gender.FEMALE)
                .serviceCluster(SpecialtyCluster.ORTHOPAEDICS)
                .capacity(3)
                .build());

        Bed bed10B01 = bedRepository.save(Bed.builder()
                .ward(ward10B)
                .bedNumber("10B-01")
                .status(BedStatus.OCCUPIED_TAKEN)
                .isNearNursingStation(true)
                .hasTelemetry(false)
                .currentPatient(inp5)
                .build());

        bedRepository.save(Bed.builder()
                .ward(ward10B)
                .bedNumber("10B-02")
                .status(BedStatus.EMPTY_CLEANED)
                .isNearNursingStation(true)
                .hasTelemetry(false)
                .cleaningStartedAt(LocalDateTime.now().minusHours(2))
                .lastCleanedAt(LocalDateTime.now().minusHours(1).minusMinutes(30))
                .build());

        bedRepository.save(Bed.builder()
                .ward(ward10B)
                .bedNumber("10B-03")
                .status(BedStatus.EMPTY_CLEANED)
                .isNearNursingStation(false)
                .hasTelemetry(false)
                .cleaningStartedAt(LocalDateTime.now().minusHours(3))
                .lastCleanedAt(LocalDateTime.now().minusHours(2).minusMinutes(30))
                .build());

        // ---- Ward 11A: Level 11 / General Medicine / Class A / Negative Pressure (MRSA/RESPIRATORY isolation) ----
        Ward ward11A = wardRepository.save(Ward.builder()
                .level(11)
                .name("Ward 11A")
                .wardClass(WardClass.A)
                .lockedGender(null)
                .serviceCluster(SpecialtyCluster.GENERAL_MEDICINE)
                .capacity(2)
                .isNegativePressure(true)
                .build());

        bedRepository.save(Bed.builder()
                .ward(ward11A)
                .bedNumber("11A-01")
                .status(BedStatus.EMPTY_CLEANED)
                .isNearNursingStation(true)
                .hasTelemetry(true)
                .cleaningStartedAt(LocalDateTime.now().minusHours(1))
                .lastCleanedAt(LocalDateTime.now().minusMinutes(30))
                .build());

        bedRepository.save(Bed.builder()
                .ward(ward11A)
                .bedNumber("11A-02")
                .status(BedStatus.EMPTY_CLEANED)
                .isNearNursingStation(false)
                .hasTelemetry(false)
                .cleaningStartedAt(LocalDateTime.now().minusHours(2))
                .lastCleanedAt(LocalDateTime.now().minusHours(1).minusMinutes(15))
                .build());

        // Seed active Inpatient Admission Requests for occupied beds & 1 past discharge
        java.time.LocalDate today = java.time.LocalDate.now();

        admissionRequestRepository.save(AdmissionRequest.builder()
                .patient(inp1)
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .admittingSpecialtyCluster(SpecialtyCluster.CARDIOLOGY)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .effectiveAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedWardClass(WardClass.B2)
                .status(AdmissionStatus.ADMITTED_INPATIENT)
                .assignedBed(bed8A01)
                .requestedAt(LocalDateTime.now().minusDays(3))
                .allocatedAt(LocalDateTime.now().minusDays(3).plusMinutes(20))
                .admittedAt(LocalDateTime.now().minusDays(3).plusMinutes(45))
                .waitingInEd(false)
                .edd(today.plusDays(2))
                .eddConfidence(EddConfidence.HIGH)
                .eddRationale("Post-PCI cardiac stabilization complete; ambulating well")
                .build());

        admissionRequestRepository.save(AdmissionRequest.builder()
                .patient(inp2)
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .admittingSpecialtyCluster(SpecialtyCluster.CARDIOLOGY)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .effectiveAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .requestedWardClass(WardClass.B2)
                .status(AdmissionStatus.ADMITTED_INPATIENT)
                .assignedBed(bed8A02)
                .requestedAt(LocalDateTime.now().minusDays(2))
                .allocatedAt(LocalDateTime.now().minusDays(2).plusMinutes(30))
                .admittedAt(LocalDateTime.now().minusDays(2).plusMinutes(55))
                .waitingInEd(false)
                .edd(today.plusDays(3))
                .eddConfidence(EddConfidence.MEDIUM)
                .eddRationale("Monitoring oral diuresis response and kidney panel")
                .build());

        admissionRequestRepository.save(AdmissionRequest.builder()
                .patient(inp3)
                .suspectedDiagnosisService(SpecialtyCluster.GENERAL_MEDICINE)
                .admittingSpecialtyCluster(SpecialtyCluster.GENERAL_MEDICINE)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .effectiveAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .requestedWardClass(WardClass.C)
                .status(AdmissionStatus.ADMITTED_INPATIENT)
                .assignedBed(bed9A01)
                .requestedAt(LocalDateTime.now().minusDays(1))
                .allocatedAt(LocalDateTime.now().minusDays(1).plusMinutes(15))
                .admittedAt(LocalDateTime.now().minusDays(1).plusMinutes(35))
                .waitingInEd(false)
                .edd(today)
                .eddConfidence(EddConfidence.HIGH)
                .eddRationale("Afebrile x48h, oral antibiotics tolerated, ready for morning sign-off")
                .build());

        admissionRequestRepository.save(AdmissionRequest.builder()
                .patient(inp4)
                .suspectedDiagnosisService(SpecialtyCluster.SURGERY)
                .admittingSpecialtyCluster(SpecialtyCluster.SURGERY)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .effectiveAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .requestedWardClass(WardClass.B2)
                .status(AdmissionStatus.ADMITTED_INPATIENT)
                .assignedBed(bed10A01)
                .requestedAt(LocalDateTime.now().minusDays(2).minusHours(5))
                .allocatedAt(LocalDateTime.now().minusDays(2).minusHours(5).plusMinutes(25))
                .admittedAt(LocalDateTime.now().minusDays(2).minusHours(5).plusMinutes(50))
                .waitingInEd(false)
                .edd(today.plusDays(1))
                .eddConfidence(EddConfidence.HIGH)
                .eddRationale("Wound dressing dry, drain removed, discharge planned tomorrow")
                .build());

        admissionRequestRepository.save(AdmissionRequest.builder()
                .patient(inp5)
                .suspectedDiagnosisService(SpecialtyCluster.ORTHOPAEDICS)
                .admittingSpecialtyCluster(SpecialtyCluster.ORTHOPAEDICS)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .effectiveAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .requestedWardClass(WardClass.B1)
                .status(AdmissionStatus.ADMITTED_INPATIENT)
                .assignedBed(bed10B01)
                .requestedAt(LocalDateTime.now().minusDays(1).minusHours(6))
                .allocatedAt(LocalDateTime.now().minusDays(1).minusHours(6).plusMinutes(18))
                .admittedAt(LocalDateTime.now().minusDays(1).minusHours(6).plusMinutes(40))
                .waitingInEd(false)
                .edd(today.plusDays(2))
                .eddConfidence(EddConfidence.HIGH)
                .eddRationale("Physiotherapy transfer cleared, step-down walk approved")
                .build());

        // Past discharge
        Patient pastInp = patientRepository.save(Patient.builder()
                .name("Madam Choo (Discharged)")
                .nricMasked("S****334H")
                .age(65)
                .gender(Gender.FEMALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(20)
                .needsTelemetry(false)
                .queueToken("TOKEN-DIS-334")
                .build());

        admissionRequestRepository.save(AdmissionRequest.builder()
                .patient(pastInp)
                .suspectedDiagnosisService(SpecialtyCluster.GENERAL_MEDICINE)
                .admittingSpecialtyCluster(SpecialtyCluster.GENERAL_MEDICINE)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .effectiveAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .requestedWardClass(WardClass.C)
                .status(AdmissionStatus.DISCHARGED)
                .requestedAt(LocalDateTime.now().minusDays(2))
                .allocatedAt(LocalDateTime.now().minusDays(2).plusMinutes(10))
                .admittedAt(LocalDateTime.now().minusDays(2).plusMinutes(30))
                .dischargedAt(LocalDateTime.now().minusDays(1).withHour(10).withMinute(30))
                .waitingInEd(false)
                .build());

        log.info("[PROTOTYPE SEEDER] Seeded Wards 8A/8B/9A/10A/10B/11A, {} beds, and inpatient admission requests.", bedRepository.count());
    }

    // -------------------------------------------------------------------------
    // ED Patient seeding
    // -------------------------------------------------------------------------

    private void seedEdPatients() {
        // ------------------------------------------------------------------
        // P101 — Male, CARDIOLOGY, Tier 2, BED_REQUESTED (direct admission)
        //        High fall-risk (65), needs telemetry → tracer bullet
        // ------------------------------------------------------------------
        Patient p101 = patientRepository.save(Patient.builder()
                .name("Tan Ah Meng")
                .nricMasked("S****123A")
                .age(68)
                .gender(Gender.MALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(65)
                .needsTelemetry(true)
                .queueToken("TOKEN-P101")
                .build());

        admissionRequestRepository.save(AdmissionRequest.builder()
                .patient(p101)
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .effectiveAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .primaryTelemetry(true)
                .effectiveTelemetry(true)
                .requestedWardClass(WardClass.B2)
                .status(AdmissionStatus.BED_REQUESTED)
                .isRecommendationAccepted(true)
                .requestedAt(LocalDateTime.now().minusMinutes(45))
                .build());

        // ------------------------------------------------------------------
        // P102 — Female, RESPIRATORY, ASSESSMENT_PENDING with 2 OPEN broadcasts
        //        Consult-gated: Cardiology + General Medicine
        //        Scenario: specialist broadcast pool visible in UI
        // ------------------------------------------------------------------
        Patient p102 = patientRepository.save(Patient.builder()
                .name("Siti Rahmah")
                .nricMasked("S****456B")
                .age(54)
                .gender(Gender.FEMALE)
                .infectionStatus(InfectionStatus.RESPIRATORY)
                .fallRiskScore(20)
                .needsTelemetry(false)
                .queueToken("TOKEN-P102")
                .build());

        AdmissionRequest req102 = admissionRequestRepository.save(AdmissionRequest.builder()
                .patient(p102)
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .effectiveAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .primaryTelemetry(false)
                .effectiveTelemetry(false)
                .requestedWardClass(WardClass.C)
                .status(AdmissionStatus.ASSESSMENT_PENDING)
                .requiresSpecialistConsult(true)
                .requestedAt(LocalDateTime.now().minusMinutes(28))
                .build());

        broadcastRepository.save(AssessmentBroadcast.builder()
                .admissionRequest(req102)
                .targetCluster(SpecialtyCluster.CARDIOLOGY)
                .status(BroadcastStatus.OPEN)
                .diversionPathway(DiversionPathway.NONE)
                .build());

        broadcastRepository.save(AssessmentBroadcast.builder()
                .admissionRequest(req102)
                .targetCluster(SpecialtyCluster.GENERAL_MEDICINE)
                .status(BroadcastStatus.OPEN)
                .diversionPathway(DiversionPathway.NONE)
                .build());

        // ------------------------------------------------------------------
        // P103 — Female, MRSA, high fall-risk — clean ED patient (no admission request)
        //        Reserved for Tracer Bullet 7 (consensus gate test)
        // ------------------------------------------------------------------
        patientRepository.save(Patient.builder()
                .name("Kowsalya")
                .nricMasked("S****789C")
                .age(72)
                .gender(Gender.FEMALE)
                .infectionStatus(InfectionStatus.MRSA)
                .fallRiskScore(70)
                .needsTelemetry(true)
                .queueToken("TOKEN-P103")
                .build());

        // ------------------------------------------------------------------
        // P104 — Female, non-infectious — clean ED patient (no admission request)
        //        Reserved for Tracer Bullet 6 (SLA auto-escalation test)
        // ------------------------------------------------------------------
        patientRepository.save(Patient.builder()
                .name("Mdm Lee")
                .nricMasked("S****234D")
                .age(81)
                .gender(Gender.FEMALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(50)
                .needsTelemetry(false)
                .queueToken("TOKEN-P104")
                .build());

        // ------------------------------------------------------------------
        // P110 — Female, MRSA, BED_ALLOCATED (bed EMPTY_ASSIGNED, patient in transit)
        //        Concordant consult completed; effectiveAcuityTier = Tier 2
        //        Scenario: bed-in-transit green state, check-in pending, negative-pressure ward
        // ------------------------------------------------------------------
        Patient p110 = patientRepository.save(Patient.builder()
                .name("Kavitha (In Transit)")
                .nricMasked("S****811S")
                .age(68)
                .gender(Gender.FEMALE)
                .infectionStatus(InfectionStatus.MRSA)
                .fallRiskScore(70)
                .needsTelemetry(true)
                .queueToken("TOKEN-P110")
                .build());

        Bed bed11A01 = bedRepository.findAll().stream()
                .filter(b -> b.getBedNumber().equals("11A-01"))
                .findFirst()
                .orElseThrow();
        bed11A01.setStatus(BedStatus.EMPTY_ASSIGNED);
        bed11A01.setCurrentPatient(p110);
        bedRepository.save(bed11A01);

        Ward ward11A = bed11A01.getWard();
        ward11A.setLockedGender(Gender.FEMALE);
        ward11A.setLockedInfectionStatus(InfectionStatus.MRSA);
        wardRepository.save(ward11A);

        AdmissionRequest req110 = admissionRequestRepository.save(AdmissionRequest.builder()
                .patient(p110)
                .suspectedDiagnosisService(SpecialtyCluster.GENERAL_MEDICINE)
                .admittingSpecialtyCluster(SpecialtyCluster.GENERAL_MEDICINE)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .secondaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .effectiveAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .primaryTelemetry(true)
                .secondaryTelemetry(true)
                .effectiveTelemetry(true)
                .requestedWardClass(WardClass.A)
                .status(AdmissionStatus.BED_ALLOCATED)
                .requiresSpecialistConsult(true)
                .isDiscordant(false)
                .isRecommendationAccepted(true)
                .assignedBed(bed11A01)
                .requestedAt(LocalDateTime.now().minusMinutes(60))
                .allocatedAt(LocalDateTime.now().minusMinutes(15))
                .waitingInEd(true)
                .build());

        broadcastRepository.save(AssessmentBroadcast.builder()
                .admissionRequest(req110)
                .targetCluster(SpecialtyCluster.GENERAL_MEDICINE)
                .status(BroadcastStatus.COMPLETED)
                .claimedBySpecialistId("dr_chen_genmedicine")
                .claimedAt(LocalDateTime.now().minusMinutes(40))
                .secondaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .secondaryTelemetry(true)
                .consultNotes("Confirmed MRSA. Negative pressure isolation mandatory. Telemetry required. Concordant with ED Tier 2 assessment.")
                .diversionPathway(DiversionPathway.NONE)
                .build());

        // ------------------------------------------------------------------
        // P111 — Female, Tier 1 Critical, BED_REQUESTED with HOUSEKEEPING delay tag
        //        Scenario: delay reason tag visible in patient tracker & BMU queue
        // ------------------------------------------------------------------
        Patient p111 = patientRepository.save(Patient.builder()
                .name("Mdm Rajamani (Delayed)")
                .nricMasked("S****522T")
                .age(79)
                .gender(Gender.FEMALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(55)
                .needsTelemetry(false)
                .queueToken("TOKEN-P111")
                .build());

        admissionRequestRepository.save(AdmissionRequest.builder()
                .patient(p111)
                .suspectedDiagnosisService(SpecialtyCluster.GENERAL_MEDICINE)
                .primaryAcuityTier(AcuityTier.TIER_1_CRITICAL)
                .effectiveAcuityTier(AcuityTier.TIER_1_CRITICAL)
                .primaryTelemetry(false)
                .effectiveTelemetry(false)
                .requestedWardClass(WardClass.C)
                .status(AdmissionStatus.BED_REQUESTED)
                .isRecommendationAccepted(true)
                .delayReasonTag(DelayReasonCode.HOUSEKEEPING_DELAY.name())
                .operationalDelayReason("Ward 9A bed 9A-02 final sanitization delayed by EVS staffing gap. Estimated 20 mins.")
                .requestedAt(LocalDateTime.now().minusMinutes(75))
                .build());

        // ------------------------------------------------------------------
        // P105 — Male, CARDIOLOGY, ASSESSMENT_PENDING — DISCORDANT assessment
        //        ED assessed Tier 3; specialist returned Tier 2 → effectiveTier elevated
        //        One broadcast COMPLETED (discordant), one still OPEN (chained to SURGERY)
        //        Scenario: discordance badge + effective acuity elevation visible in BMU
        // ------------------------------------------------------------------
        Patient p105 = patientRepository.save(Patient.builder()
                .name("Mr Fernandez")
                .nricMasked("S****677M")
                .age(55)
                .gender(Gender.MALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(15)
                .needsTelemetry(false)
                .queueToken("TOKEN-P105")
                .build());

        AdmissionRequest req105 = admissionRequestRepository.save(AdmissionRequest.builder()
                .patient(p105)
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .secondaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)  // specialist escalated
                .effectiveAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)  // auto-elevated
                .primaryTelemetry(false)
                .secondaryTelemetry(true)          // specialist flagged telemetry
                .effectiveTelemetry(true)          // safety-first override
                .requestedWardClass(WardClass.B2)
                .status(AdmissionStatus.ASSESSMENT_PENDING)
                .requiresSpecialistConsult(true)
                .isDiscordant(true)
                .requestedAt(LocalDateTime.now().minusMinutes(50))
                .build());

        // Completed discordant broadcast (Cardiology specialist escalated tier)
        broadcastRepository.save(AssessmentBroadcast.builder()
                .admissionRequest(req105)
                .targetCluster(SpecialtyCluster.CARDIOLOGY)
                .status(BroadcastStatus.COMPLETED)
                .claimedBySpecialistId("dr_patel_cardiology")
                .claimedAt(LocalDateTime.now().minusMinutes(35))
                .secondaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .secondaryTelemetry(true)
                .consultNotes("Elevated troponin trend and dynamic ST changes. Upgrading to Tier 2. Continuous telemetry mandatory. Recommending chained surgery review for concurrent abdominal pain.")
                .diversionPathway(DiversionPathway.NONE)
                .build());

        // Chained open broadcast to Surgery (consensus gate still pending)
        broadcastRepository.save(AssessmentBroadcast.builder()
                .admissionRequest(req105)
                .targetCluster(SpecialtyCluster.SURGERY)
                .status(BroadcastStatus.OPEN)
                .diversionPathway(DiversionPathway.NONE)
                .build());

        // ------------------------------------------------------------------
        // P106 — Female, GENERAL_MEDICINE, Tier 4, DIVERTED_HAH (MIC@Home)
        //        Scenario: diversion virtual bed, HAH pathway exercised
        // ------------------------------------------------------------------
        Patient p106 = patientRepository.save(Patient.builder()
                .name("Mrs Tan Boon Hwa")
                .nricMasked("S****912N")
                .age(63)
                .gender(Gender.FEMALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(10)
                .needsTelemetry(false)
                .queueToken("TOKEN-P106")
                .build());

        admissionRequestRepository.save(AdmissionRequest.builder()
                .patient(p106)
                .suspectedDiagnosisService(SpecialtyCluster.GENERAL_MEDICINE)
                .admittingSpecialtyCluster(SpecialtyCluster.GENERAL_MEDICINE)
                .primaryAcuityTier(AcuityTier.TIER_4_SUBACUTE_DIVERSION)
                .effectiveAcuityTier(AcuityTier.TIER_4_SUBACUTE_DIVERSION)
                .primaryTelemetry(false)
                .effectiveTelemetry(false)
                .requestedWardClass(WardClass.B2)
                .status(AdmissionStatus.DIVERTED_HAH)
                .diversionRecommended(true)
                .diversionPathway(DiversionPathway.HOSPITAL_AT_HOME_MIC)
                .virtualBedNumber("MIC-V042")
                .isRecommendationAccepted(true)
                .requestedAt(LocalDateTime.now().minusHours(2))
                .allocatedAt(LocalDateTime.now().minusHours(1).minusMinutes(40))
                .waitingInEd(false)
                .build());

        // ------------------------------------------------------------------
        // P107 — Male, SURGERY, Tier 3, BED_REQUESTED with Surgery broadcast COMPLETED
        //        Consensus gate cleared → awaiting BMU admitting-cluster assignment
        //        Scenario: consult-gated → all broadcasts done → BMU assignment pending
        // ------------------------------------------------------------------
        Patient p107 = patientRepository.save(Patient.builder()
                .name("Mr Goh Beng Kiat")
                .nricMasked("S****305P")
                .age(44)
                .gender(Gender.MALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(10)
                .needsTelemetry(false)
                .queueToken("TOKEN-P107")
                .build());

        AdmissionRequest req107 = admissionRequestRepository.save(AdmissionRequest.builder()
                .patient(p107)
                .suspectedDiagnosisService(SpecialtyCluster.SURGERY)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .effectiveAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .primaryTelemetry(false)
                .effectiveTelemetry(false)
                .requestedWardClass(WardClass.B2)
                .status(AdmissionStatus.BED_REQUESTED)
                .requiresSpecialistConsult(true)
                .isDiscordant(false)
                .isRecommendationAccepted(true)
                .requestedAt(LocalDateTime.now().minusMinutes(35))
                .build());

        broadcastRepository.save(AssessmentBroadcast.builder()
                .admissionRequest(req107)
                .targetCluster(SpecialtyCluster.SURGERY)
                .status(BroadcastStatus.COMPLETED)
                .claimedBySpecialistId("dr_wong_surgery")
                .claimedAt(LocalDateTime.now().minusMinutes(25))
                .secondaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .secondaryTelemetry(false)
                .consultNotes("Right lower quadrant pain consistent with appendicitis. Surgical admission confirmed. Non-urgent for theatre.")
                .diversionPathway(DiversionPathway.NONE)
                .build());

        // ------------------------------------------------------------------
        // P108 & P109 — Pure ED awaiting-assessment patients (no admission request yet)
        //               Scenario: visible on ED clinician intake board
        // ------------------------------------------------------------------
        patientRepository.save(Patient.builder()
                .name("Encik Azman")
                .nricMasked("S****601Q")
                .age(49)
                .gender(Gender.MALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(5)
                .needsTelemetry(false)
                .queueToken("TOKEN-P108")
                .build());

        patientRepository.save(Patient.builder()
                .name("Ms Priya")
                .nricMasked("S****738R")
                .age(34)
                .gender(Gender.FEMALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(0)
                .needsTelemetry(false)
                .queueToken("TOKEN-P109")
                .build());

        log.info("[PROTOTYPE SEEDER] Seeded ED patients P101-P111: BED_REQUESTED (P101/P107/P111), ASSESSMENT_PENDING+broadcasts (P102/P105), BED_ALLOCATED in-transit (P110), DIVERTED_HAH (P106), discordant consult (P105), delay tag (P111), clean test-reserved (P103/P104), awaiting-assessment (P108/P109).");
    }
}
