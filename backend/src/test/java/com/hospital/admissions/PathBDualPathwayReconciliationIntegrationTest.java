package com.hospital.admissions;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import tools.jackson.databind.ObjectMapper;
import com.hospital.admissions.controller.KpiAnalyticsController;
import com.hospital.admissions.dto.*;
import com.hospital.admissions.entity.*;
import com.hospital.admissions.repository.*;
import com.hospital.admissions.security.AuditLogger;
import com.hospital.admissions.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("prototype")
class PathBDualPathwayReconciliationIntegrationTest {

    @Autowired
    private ClinicianService clinicianService;

    @Autowired
    private BmuService bmuService;

    @Autowired
    private PatientTrackerService trackerService;

    @Autowired
    private WardService wardService;

    @Autowired
    private KpiAnalyticsController kpiAnalyticsController;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AdmissionRequestRepository admissionRequestRepository;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private AssessmentBroadcastRepository broadcastRepository;

    @Autowired
    private PatientAuditInteractionRepository patientAuditInteractionRepository;

    @Autowired
    private AuditLogger auditLogger;

    private File tempLogFile;
    private FileAppender<ILoggingEvent> fileAppender;
    private Logger auditLogbackLogger;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("dr_test_admin", null, List.of())
        );

        // Attach FileAppender to stream AuditLogger output to temp file
        tempLogFile = File.createTempFile("audit-recon-", ".log");
        auditLogbackLogger = (Logger) LoggerFactory.getLogger(AuditLogger.class);

        fileAppender = new FileAppender<>();
        fileAppender.setContext(auditLogbackLogger.getLoggerContext());
        fileAppender.setFile(tempLogFile.getAbsolutePath());

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(auditLogbackLogger.getLoggerContext());
        encoder.setPattern("%msg%n");
        encoder.start();

        fileAppender.setEncoder(encoder);
        fileAppender.start();
        auditLogbackLogger.addAppender(fileAppender);

        // Clear existing transactional records
        patientAuditInteractionRepository.deleteAll();
        broadcastRepository.deleteAll();
        admissionRequestRepository.deleteAll();

        // Reset bed cleaning timestamps so pre-seeded beds don't interfere with KPI 22 test
        List<Bed> beds = bedRepository.findAll();
        for (Bed b : beds) {
            b.setCleaningStartedAt(null);
            b.setLastCleanedAt(null);
        }
        bedRepository.saveAll(beds);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (fileAppender != null && auditLogbackLogger != null) {
            fileAppender.stop();
            auditLogbackLogger.detachAppender(fileAppender);
        }
        if (tempLogFile != null && tempLogFile.exists()) {
            tempLogFile.delete();
        }
    }

    @Test
    @DisplayName("End-to-end clinical workflow scenario emits all standard audit tags and achieves exact parity between Path A and Path B")
    void testDualPathwayReconciliation_ExactParity() throws Exception {
        // --- 1. Emergency Department Submissions (Epic 1) ---
        Patient p1 = patientRepository.save(Patient.builder()
                .name("Mr. Tan")
                .nricMasked("S****111A")
                .age(62)
                .gender(Gender.MALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(2)
                .needsTelemetry(true)
                .queueToken("TOK-TAN-01")
                .build());

        Patient p2 = patientRepository.save(Patient.builder()
                .name("Mdm. Lee")
                .nricMasked("S****222B")
                .age(55)
                .gender(Gender.FEMALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(1)
                .needsTelemetry(false)
                .queueToken("TOK-LEE-02")
                .build());

        EdAssessmentSubmitRequest edReq1 = EdAssessmentSubmitRequest.builder()
                .patientId(p1.getId())
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedWardClass(WardClass.B2)
                .needsTelemetry(true)
                .requiresSpecialistConsult(true)
                .recommendedAccepted(true)
                .elapsedMins(12.0)
                .build();
        AdmissionRequest req1 = clinicianService.submitEdAssessment(edReq1);

        EdAssessmentSubmitRequest edReq2 = EdAssessmentSubmitRequest.builder()
                .patientId(p2.getId())
                .suspectedDiagnosisService(SpecialtyCluster.GENERAL_MEDICINE)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .requestedWardClass(WardClass.B2)
                .needsTelemetry(false)
                .requiresSpecialistConsult(false)
                .recommendedAccepted(true)
                .elapsedMins(16.0)
                .build();
        AdmissionRequest req2 = clinicianService.submitEdAssessment(edReq2);

        // Specialist claim and consult for req1
        List<AssessmentBroadcast> broadcasts = broadcastRepository.findByAdmissionRequest_Id(req1.getId());
        assertThat(broadcasts).isNotEmpty();
        AssessmentBroadcast b1 = broadcasts.get(0);
        b1.setCreatedAt(LocalDateTime.now().minusMinutes(8));
        broadcastRepository.save(b1);

        clinicianService.claimBroadcast(b1.getId());
        clinicianService.submitConsult(b1.getId(), SpecialistConsultRequest.builder()
                .secondaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .secondaryTelemetry(true)
                .diversionRecommended(false)
                .consultNotes("Specialist concurs with acute cardiology placement")
                .build());

        // --- 2. BMU Capacity & Diversions (Epic 2) ---
        List<Ward> wards = wardRepository.findAll();
        Ward ward = wards.get(0);
        List<Bed> beds = bedRepository.findByWard_Id(ward.getId());
        Bed bed1 = beds.get(0);
        bed1.setStatus(BedStatus.EMPTY_CLEANED);
        bedRepository.save(bed1);

        // Standard 1-click allocation for req1
        bmuService.allocateBed(req1.getId(), bed1.getId());

        // Diversion referral for req2 to Community Hospital
        bmuService.referToSisterHospital(req2.getId(), "Outram Community Hospital");
        // Mark referral completed within 20 mins (< 30m SLA)
        req2 = admissionRequestRepository.findById(req2.getId()).orElseThrow();
        req2.setReferralCompletedAt(req2.getReferralDispatchedAt().plusMinutes(20));
        admissionRequestRepository.save(req2);

        // --- 3. Patient Journey & Caregiver Counseling (Epic 3) ---
        // Patient 1 accesses tracker
        trackerService.trackPatient("TOK-TAN-01");

        // Simulate 2h periodic update and delay tag for req1
        req1 = admissionRequestRepository.findById(req1.getId()).orElseThrow();
        req1.setRequestedAt(LocalDateTime.now().minusHours(3));
        req1.setLastPeriodicUpdateSentAt(LocalDateTime.now().minusHours(1));
        req1.setDelayReasonTag("HOUSEKEEPING_DELAY");
        admissionRequestRepository.save(req1);

        auditLogger.logAction("SYSTEM_SCHEDULER", "DISPATCH_PERIODIC_UPDATE",
                "AdmissionRequest:" + req1.getId(),
                "Channel=SMS_PUSH, Token=TOK-TAN-01, Milestone=MILESTONE_1_ADMISSION_CONFIRMED, DwellMins=180, DeliveryStatus=SUCCESS");

        auditLogger.logAction("dr_test_admin", "TAG_DELAY_REASON",
                "AdmissionRequest:" + req1.getId(),
                "DelayCode=HOUSEKEEPING_DELAY, DwellMins=180, AcuityTier=TIER_2_ACUTE_URGENT");

        // Patient 2 contacts MSW counseling hotline
        trackerService.recordPatientAction("TOK-LEE-02", "MSW_CALL");

        // --- 4. Inpatient Discharge Runway & Rapid Turnover (Epic 4) ---
        // Record 48h advance runway EDD
        wardService.updateEdd(p1.getId(), EddUpdateRequest.builder()
                .edd(LocalDate.now().plusDays(2))
                .eddConfidence(EddConfidence.HIGH)
                .rationale("Patient condition stabilized post-cardiac monitoring")
                .build());

        wardService.dischargeSignoff(p1.getId());
        wardService.deliverMedication(p1.getId());

        // Vacate patient before noon (10:30 AM)
        req1 = admissionRequestRepository.findById(req1.getId()).orElseThrow();
        req1.setDischargedAt(LocalDateTime.now().withHour(10).withMinute(30));
        req1.setEddRecordedAt(req1.getDischargedAt().minusHours(49));
        req1.setStatus(AdmissionStatus.DISCHARGED);
        admissionRequestRepository.save(req1);

        auditLogger.logAction("dr_test_admin", "VACATE_PATIENT",
                "Bed:" + bed1.getId(),
                "BedNumber=" + bed1.getBedNumber() + ", VacateTimestamp=" + LocalDateTime.now() + ", VacateHour=10, DischargedBeforeNoon=true");

        // Housekeeping clean bed within 25 mins (< 30m SLA)
        bed1 = bedRepository.findById(bed1.getId()).orElseThrow();
        bed1.setStatus(BedStatus.EMPTY_PENDING_CLEANING);
        bed1.setCleaningStartedAt(LocalDateTime.now().minusMinutes(25));
        bed1.setLastCleanedAt(LocalDateTime.now());
        bedRepository.save(bed1);

        auditLogger.logAction("dr_test_admin", "CLEAN_BED",
                "Bed:" + bed1.getId(),
                "BedNumber=" + bed1.getBedNumber() + ", HousekeeperId=dr_test_admin, ElapsedCleaningMins=25, Within30mSla=true");

        // Flush and detach file appender so log file is complete
        fileAppender.stop();
        auditLogbackLogger.detachAppender(fileAppender);

        // --- 5. Retrieve Path A (Relational SQL/Service) KPI Summary ---
        HospitalKpiSummaryDto pathA = kpiAnalyticsController.getKpiSummary(null, null).getBody();
        assertThat(pathA).isNotNull();

        // --- 6. Execute Path B Shell Script (CLI Log Extraction) ---
        File projectRoot = new File("..").getCanonicalFile();
        File scriptFile = new File(projectRoot, "scripts/kpi-extract-all.sh");
        assertThat(scriptFile).exists();

        Process process = new ProcessBuilder(scriptFile.getAbsolutePath(), tempLogFile.getAbsolutePath())
                .directory(projectRoot)
                .redirectErrorStream(true)
                .start();

        String scriptOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        assertThat(exitCode).isEqualTo(0);

        HospitalKpiSummaryDto pathB = objectMapper.readValue(scriptOutput, HospitalKpiSummaryDto.class);
        assertThat(pathB).isNotNull();

        // --- 7. Assert Exact Mathematical Consistency and Parity ---
        // Epic 1
        assertThat(pathB.getDigitalBedRequestCount()).isEqualTo(pathA.getDigitalBedRequestCount());
        assertThat(pathB.getAvgEdTurnaroundMinutes()).isEqualTo(pathA.getAvgEdTurnaroundMinutes());
        assertThat(pathB.getEdTurnaroundP95Minutes()).isEqualTo(pathA.getEdTurnaroundP95Minutes());
        assertThat(pathB.getSpecialistClaimLatencyAvgMinutes()).isEqualTo(pathA.getSpecialistClaimLatencyAvgMinutes());
        assertThat(pathB.getPrimarySpecialistConcordanceRatePct()).isEqualTo(pathA.getPrimarySpecialistConcordanceRatePct());

        // Epic 2
        assertThat(pathB.getBmuSuggestionAcceptanceRatePct()).isEqualTo(pathA.getBmuSuggestionAcceptanceRatePct());
        assertThat(pathB.getBmuManualOverrideCount()).isEqualTo(pathA.getBmuManualOverrideCount());
        assertThat(pathB.getTotalDiversionCount()).isEqualTo(pathA.getTotalDiversionCount());
        assertThat(pathB.getDiversionRatePct()).isEqualTo(pathA.getDiversionRatePct());
        assertThat(pathB.getSisterHospitalSlaCompliancePct()).isEqualTo(pathA.getSisterHospitalSlaCompliancePct());

        // Epic 3
        assertThat(pathB.getPatientTrackerAccessRatePct()).isEqualTo(pathA.getPatientTrackerAccessRatePct());
        assertThat(pathB.getTwoHourPeriodicUpdateDeliveryPct()).isEqualTo(pathA.getTwoHourPeriodicUpdateDeliveryPct());
        assertThat(pathB.getProlongedWaitCommunicationRatePct()).isEqualTo(pathA.getProlongedWaitCommunicationRatePct());
        assertThat(pathB.getCaregiverCounselingConnectRatePct()).isEqualTo(pathA.getCaregiverCounselingConnectRatePct());

        // Epic 4
        assertThat(pathB.getDischargeBeforeNoonRatePct()).isEqualTo(pathA.getDischargeBeforeNoonRatePct());
        assertThat(pathB.getAdvanceRunwayEstablishmentRatePct()).isEqualTo(pathA.getAdvanceRunwayEstablishmentRatePct());
        assertThat(pathB.getBedsideMedicationDeliveryAdoptionPct()).isEqualTo(pathA.getBedsideMedicationDeliveryAdoptionPct());
        assertThat(pathB.getHousekeepingTurnoverAvgMinutes()).isEqualTo(pathA.getHousekeepingTurnoverAvgMinutes());
        assertThat(pathB.getHousekeeping30mSlaCompliancePct()).isEqualTo(pathA.getHousekeeping30mSlaCompliancePct());
    }
}
