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

            // Ward 8A: Level 8 / Cardiology / Class B2 / Locked Male
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

            // Ward 8B: Level 8 / Cardiology / Class B2 / Flex Unlocked
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

            // Ward 9A: Level 9 / General Medicine / Class C / Locked Female
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

            // Seed active Inpatient Admission Requests for occupied beds & 1 past discharge
            admissionRequestRepository.save(AdmissionRequest.builder()
                    .patient(inp1)
                    .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                    .admittingSpecialtyCluster(SpecialtyCluster.CARDIOLOGY)
                    .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                    .requestedWardClass(WardClass.B2)
                    .status(AdmissionStatus.ADMITTED_INPATIENT)
                    .assignedBed(bed8A01)
                    .requestedAt(LocalDateTime.now().minusDays(3))
                    .allocatedAt(LocalDateTime.now().minusDays(3).plusMinutes(20))
                    .admittedAt(LocalDateTime.now().minusDays(3).plusMinutes(45))
                    .waitingInEd(false)
                    .build());

            admissionRequestRepository.save(AdmissionRequest.builder()
                    .patient(inp2)
                    .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                    .admittingSpecialtyCluster(SpecialtyCluster.CARDIOLOGY)
                    .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                    .requestedWardClass(WardClass.B2)
                    .status(AdmissionStatus.ADMITTED_INPATIENT)
                    .assignedBed(bed8A02)
                    .requestedAt(LocalDateTime.now().minusDays(2))
                    .allocatedAt(LocalDateTime.now().minusDays(2).plusMinutes(30))
                    .admittedAt(LocalDateTime.now().minusDays(2).plusMinutes(55))
                    .waitingInEd(false)
                    .build());

            admissionRequestRepository.save(AdmissionRequest.builder()
                    .patient(inp3)
                    .suspectedDiagnosisService(SpecialtyCluster.GENERAL_MEDICINE)
                    .admittingSpecialtyCluster(SpecialtyCluster.GENERAL_MEDICINE)
                    .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                    .requestedWardClass(WardClass.C)
                    .status(AdmissionStatus.ADMITTED_INPATIENT)
                    .assignedBed(bed9A01)
                    .requestedAt(LocalDateTime.now().minusDays(1))
                    .allocatedAt(LocalDateTime.now().minusDays(1).plusMinutes(15))
                    .admittedAt(LocalDateTime.now().minusDays(1).plusMinutes(35))
                    .waitingInEd(false)
                    .build());

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
                    .requestedWardClass(WardClass.C)
                    .status(AdmissionStatus.DISCHARGED)
                    .requestedAt(LocalDateTime.now().minusDays(2))
                    .allocatedAt(LocalDateTime.now().minusDays(2).plusMinutes(10))
                    .admittedAt(LocalDateTime.now().minusDays(2).plusMinutes(30))
                    .dischargedAt(LocalDateTime.now().minusDays(1).withHour(10).withMinute(30))
                    .waitingInEd(false)
                    .build());

            log.info("[PROTOTYPE SEEDER] Seeded Wards 8A, 8B, 9A, 10 beds, and inpatient admission requests successfully.");
        }

        // 3. Seed ED Patients P101 - P104
        if (patientRepository.findByQueueToken("TOKEN-P101").isEmpty()) {
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

            Patient p103 = patientRepository.save(Patient.builder()
                    .name("Kowsalya")
                    .nricMasked("S****789C")
                    .age(72)
                    .gender(Gender.FEMALE)
                    .infectionStatus(InfectionStatus.MRSA)
                    .fallRiskScore(70)
                    .needsTelemetry(true)
                    .queueToken("TOKEN-P103")
                    .build());

            Patient p104 = patientRepository.save(Patient.builder()
                    .name("Mdm Lee")
                    .nricMasked("S****234D")
                    .age(81)
                    .gender(Gender.FEMALE)
                    .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                    .fallRiskScore(50)
                    .needsTelemetry(false)
                    .queueToken("TOKEN-P104")
                    .build());

            // Pre-seed P101 in BED_REQUESTED state for Tracer Bullet 1 immediate testing
            admissionRequestRepository.save(AdmissionRequest.builder()
                    .patient(p101)
                    .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                    .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                    .requestedWardClass(WardClass.B2)
                    .status(AdmissionStatus.BED_REQUESTED)
                    .requestedAt(LocalDateTime.now().minusMinutes(45))
                    .build());

            log.info("[PROTOTYPE SEEDER] Seeded waiting patients P101-P104 successfully.");
        }
    }
}
