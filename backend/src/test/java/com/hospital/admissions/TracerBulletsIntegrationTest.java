package com.hospital.admissions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.admissions.domain.*;
import com.hospital.admissions.dto.*;
import com.hospital.admissions.repository.*;
import com.hospital.admissions.service.ClinicianService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prototype")
class TracerBulletsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AdmissionRequestRepository admissionRequestRepository;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private AssessmentBroadcastRepository broadcastRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private ClinicianService clinicianService;

    @Test
    @DisplayName("Tracer Bullet 1: ED Assessment -> Specialist Claim -> BMU Allocation")
    void testTracerBullet1_EdToBmuAllocation() throws Exception {
        // Step 1: Verify ED patients list
        mockMvc.perform(get("/api/v1/clinicians/ed/patients")
                        .header("X-User-Role", "ED_ATTENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        Patient p101 = patientRepository.findByQueueToken("TOKEN-P101").orElseThrow();

        // Step 2: Fetch BMU Queue (P101 was pre-seeded in BED_REQUESTED)
        MvcResult queueResult = mockMvc.perform(get("/api/v1/bmu/queue")
                        .header("X-User-Role", "BMU_COORDINATOR"))
                .andExpect(status().isOk())
                .andReturn();

        List<AdmissionRequest> queue = objectMapper.readValue(
                queueResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, AdmissionRequest.class)
        );
        assertThat(queue).isNotEmpty();
        AdmissionRequest p101Req = queue.stream()
                .filter(r -> r.getPatient().getQueueToken().equals("TOKEN-P101"))
                .findFirst().orElseThrow();

        // Step 3: Get BMU recommendations for P101 (Male, Cardio, B2, Telemetry, FallRisk 65)
        MvcResult recResult = mockMvc.perform(get("/api/v1/bmu/recommendations/" + p101Req.getId())
                        .header("X-User-Role", "BMU_COORDINATOR"))
                .andExpect(status().isOk())
                .andReturn();

        List<BedRecommendation> recommendations = objectMapper.readValue(
                recResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, BedRecommendation.class)
        );

        assertThat(recommendations).isNotEmpty();
        // Top recommendation should be Bed 8A-03 (Cardio B2 Male, telemetry, near nursing station)
        BedRecommendation topRec = recommendations.get(0);
        assertThat(topRec.getBedNumber()).isEqualTo("8A-03");
        assertThat(topRec.isRecommended()).isTrue();
        // Score: +40 Specialty, +30 Consolidation (Ward 8A has occupied beds 8A-01/02), +15 Proximity = 85
        assertThat(topRec.getScore()).isEqualTo(85);

        // Step 4: 1-Click Bed Allocation
        BedAllocationRequest allocReq = BedAllocationRequest.builder()
                .admissionRequestId(p101Req.getId())
                .bedId(topRec.getBedId())
                .build();

        mockMvc.perform(post("/api/v1/bmu/allocate")
                        .with(csrf())
                        .header("X-User-Role", "BMU_COORDINATOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allocReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BED_ALLOCATED"))
                .andExpect(jsonPath("$.assignedBed.bedNumber").value("8A-03"));

        // Verify Bed 8A-03 is now EMPTY_ASSIGNED (Green)
        Bed bed8A03 = bedRepository.findById(topRec.getBedId()).orElseThrow();
        assertThat(bed8A03.getStatus()).isEqualTo(BedStatus.EMPTY_ASSIGNED);
    }

    @Test
    @DisplayName("Tracer Bullet 2: Patient Milestone Tracker -> Nurse Checkin/Vacate -> Housekeeping Clean")
    void testTracerBullet2_PatientTrackerAndTurnoverLoop() throws Exception {
        // Step 1: Patient Milestone Tracker views status
        mockMvc.perform(get("/api/v1/patients/track/TOKEN-P101")
                        .header("X-User-Role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientName").value("Tan Ah Meng"))
                .andExpect(jsonPath("$.queueToken").value("TOKEN-P101"));

        Bed bed8A03 = bedRepository.findAll().stream()
                .filter(b -> b.getBedNumber().equals("8A-03"))
                .findFirst().orElseThrow();

        // Step 2: Inpatient Ward Nurse checks in patient -> Bed turns OCCUPIED_TAKEN (Grey)
        mockMvc.perform(post("/api/v1/patients/beds/" + bed8A03.getId() + "/checkin")
                        .with(csrf())
                        .header("X-User-Role", "WARD_NURSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OCCUPIED_TAKEN"));

        bed8A03 = bedRepository.findById(bed8A03.getId()).orElseThrow();
        assertThat(bed8A03.getStatus()).isEqualTo(BedStatus.OCCUPIED_TAKEN);

        // Step 3: Ward Nurse vacates patient upon discharge -> Bed turns EMPTY_PENDING_CLEANING (Mustard Yellow - vacated, empty, not yet cleaned)
        mockMvc.perform(post("/api/v1/patients/beds/" + bed8A03.getId() + "/vacate")
                        .with(csrf())
                        .header("X-User-Role", "WARD_NURSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EMPTY_PENDING_CLEANING"));

        bed8A03 = bedRepository.findById(bed8A03.getId()).orElseThrow();
        assertThat(bed8A03.getStatus()).isEqualTo(BedStatus.EMPTY_PENDING_CLEANING);

        // Step 4: Housekeeping completes 30m terminal cleaning -> Bed turns EMPTY_CLEANED (White - empty, cleaned)
        mockMvc.perform(post("/api/v1/patients/beds/" + bed8A03.getId() + "/clean")
                        .with(csrf())
                        .header("X-User-Role", "HOUSEKEEPING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EMPTY_CLEANED"));

        bed8A03 = bedRepository.findById(bed8A03.getId()).orElseThrow();
        assertThat(bed8A03.getStatus()).isEqualTo(BedStatus.EMPTY_CLEANED);
        assertThat(bed8A03.getCurrentPatient()).isNull();
        assertThat(bed8A03.getLastCleanedAt()).isNotNull();
    }

    @Test
    @DisplayName("Tracer Bullet 3: Submit ED Assessment creates AdmissionRequest and Broadcast")
    void testSubmitEdAssessment() throws Exception {
        Patient p102 = patientRepository.findByQueueToken("TOKEN-P102").orElseThrow();

        EdAssessmentSubmitRequest submitReq = EdAssessmentSubmitRequest.builder()
                .patientId(p102.getId())
                .suspectedDiagnosisService(SpecialtyCluster.GENERAL_MEDICINE)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .requestedWardClass(WardClass.B2)
                .needsTelemetry(false)
                .requiresSpecialistConsult(true)
                .build();

        mockMvc.perform(post("/api/v1/clinicians/ed/assessments/submit")
                        .with(csrf())
                        .header("X-User-Role", "ED_ATTENDING")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSESSMENT_PENDING"))
                .andExpect(jsonPath("$.suspectedDiagnosisService").value("GENERAL_MEDICINE"))
                .andExpect(jsonPath("$.primaryAcuityTier").value("TIER_3_ACUTE_STABLE"))
                .andExpect(jsonPath("$.requestedWardClass").value("B2"));

        List<AssessmentBroadcast> broadcasts = broadcastRepository.findAll();
        assertThat(broadcasts).anyMatch(b -> b.getTargetCluster() == SpecialtyCluster.GENERAL_MEDICINE
                && b.getAdmissionRequest().getPatient().getId().equals(p102.getId()));
    }

    @Test
    @DisplayName("Tracer Bullet 4: Consult-Gated Multi-Cluster Broadcast & Atomic Claiming with 409 Conflict")
    void testTracerBullet4_ConsultGatedMultiClusterBroadcastAndAtomicClaimConflict() throws Exception {
        // Step 1: Create test patient
        Patient patient = patientRepository.save(Patient.builder()
                .name("Consult Multi-Cluster Patient")
                .nricMasked("S****999Z")
                .age(64)
                .gender(Gender.FEMALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(25)
                .queueToken("TOKEN-CONSULT-" + java.util.UUID.randomUUID())
                .build());

        // Step 2: Submit consult-gated admission with 2 clusters (CARDIOLOGY and SURGERY)
        EdAssessmentSubmitRequest submitReq = EdAssessmentSubmitRequest.builder()
                .patientId(patient.getId())
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .targetClusters(java.util.Set.of(SpecialtyCluster.CARDIOLOGY, SpecialtyCluster.SURGERY))
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedWardClass(WardClass.B1)
                .needsTelemetry(true)
                .requiresSpecialistConsult(true)
                .build();

        MvcResult submitResult = mockMvc.perform(post("/api/v1/clinicians/ed/assessments/submit")
                        .with(csrf())
                        .header("X-User-Role", "ED_ATTENDING")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSESSMENT_PENDING"))
                .andReturn();

        AdmissionRequest admission = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(),
                AdmissionRequest.class);

        // Step 3: Verify hidden from BMU queue
        MvcResult queueResult = mockMvc.perform(get("/api/v1/bmu/queue")
                        .header("X-User-Role", "BMU_COORDINATOR"))
                .andExpect(status().isOk())
                .andReturn();

        List<AdmissionRequest> bmuQueue = objectMapper.readValue(
                queueResult.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, AdmissionRequest.class));

        assertThat(bmuQueue).noneMatch(r -> r.getId().equals(admission.getId()));

        // Step 4: Verify broadcasts persisted for both clusters
        List<AssessmentBroadcast> broadcasts = broadcastRepository.findAll().stream()
                .filter(b -> b.getAdmissionRequest().getId().equals(admission.getId()))
                .toList();

        assertThat(broadcasts).hasSize(2);
        assertThat(broadcasts).extracting(AssessmentBroadcast::getTargetCluster)
                .containsExactlyInAnyOrder(SpecialtyCluster.CARDIOLOGY, SpecialtyCluster.SURGERY);
        assertThat(broadcasts).allMatch(b -> b.getStatus() == BroadcastStatus.OPEN);

        AssessmentBroadcast cardioBroadcast = broadcasts.stream()
                .filter(b -> b.getTargetCluster() == SpecialtyCluster.CARDIOLOGY)
                .findFirst().orElseThrow();

        // Step 5: Specialist 1 claims the broadcast atomically -> 200 OK
        mockMvc.perform(post("/api/v1/clinicians/specialist/broadcasts/" + cardioBroadcast.getId() + "/claim")
                        .with(csrf())
                        .header("X-User-Role", "SPECIALIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLAIMED"))
                .andExpect(jsonPath("$.claimedBySpecialistId").isNotEmpty());

        // Step 6: Specialist 2 attempts to claim the already-claimed broadcast -> 409 Conflict
        mockMvc.perform(post("/api/v1/clinicians/specialist/broadcasts/" + cardioBroadcast.getId() + "/claim")
                        .with(csrf())
                        .header("X-User-Role", "SPECIALIST"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    @DisplayName("Tracer Bullet 5: Specialist Consult Evaluation, Structured Diversion & Consult Chaining")
    void testTracerBullet5_SpecialistConsultEvaluationAndChaining() throws Exception {
        // Step 1: Create patient and consult-gated admission
        Patient patient = patientRepository.save(Patient.builder()
                .name("Chain Evaluation Patient")
                .nricMasked("S****888X")
                .age(72)
                .gender(Gender.MALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(30)
                .queueToken("TOKEN-CHAIN-" + java.util.UUID.randomUUID())
                .build());

        EdAssessmentSubmitRequest submitReq = EdAssessmentSubmitRequest.builder()
                .patientId(patient.getId())
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .targetClusters(java.util.Set.of(SpecialtyCluster.CARDIOLOGY))
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedWardClass(WardClass.B2)
                .needsTelemetry(true)
                .requiresSpecialistConsult(true)
                .build();

        MvcResult submitResult = mockMvc.perform(post("/api/v1/clinicians/ed/assessments/submit")
                        .with(csrf())
                        .header("X-User-Role", "ED_ATTENDING")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk())
                .andReturn();

        AdmissionRequest admission = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(),
                AdmissionRequest.class);

        AssessmentBroadcast broadcast = broadcastRepository.findAll().stream()
                .filter(b -> b.getAdmissionRequest().getId().equals(admission.getId()))
                .findFirst().orElseThrow();

        // Step 2: Specialist claims broadcast
        mockMvc.perform(post("/api/v1/clinicians/specialist/broadcasts/" + broadcast.getId() + "/claim")
                        .with(csrf())
                        .header("X-User-Role", "SPECIALIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLAIMED"));

        // Step 3: Specialist chains secondary consult to SURGERY
        com.hospital.admissions.dto.ChainConsultRequest chainReq = com.hospital.admissions.dto.ChainConsultRequest.builder()
                .targetCluster(SpecialtyCluster.SURGERY)
                .rationale("Suspected mesenteric ischemia, surgical opinion requested")
                .build();

        MvcResult chainResult = mockMvc.perform(post("/api/v1/clinicians/specialist/broadcasts/" + broadcast.getId() + "/chain")
                        .with(csrf())
                        .header("X-User-Role", "SPECIALIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chainReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.targetCluster").value("SURGERY"))
                .andExpect(jsonPath("$.parentBroadcastId").value(broadcast.getId().toString()))
                .andReturn();

        AssessmentBroadcast chainedBroadcast = objectMapper.readValue(
                chainResult.getResponse().getContentAsString(),
                AssessmentBroadcast.class);

        assertThat(chainedBroadcast.getParentBroadcastId()).isEqualTo(broadcast.getId());

        // Step 4: Specialist submits clinical evaluation with structured diversion
        SpecialistConsultRequest consultReq = SpecialistConsultRequest.builder()
                .secondaryAcuityTier(AcuityTier.TIER_4_SUBACUTE_DIVERSION)
                .secondaryTelemetry(false)
                .consultNotes("Hemodynamically stable; recommend step-down to Community Hospital")
                .diversionPathway(DiversionPathway.COMMUNITY_HOSPITAL)
                .diversionRecommended(true)
                .build();

        mockMvc.perform(post("/api/v1/clinicians/specialist/broadcasts/" + broadcast.getId() + "/consult")
                        .with(csrf())
                        .header("X-User-Role", "SPECIALIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(consultReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Step 5: Verify updated AdmissionRequest dossier
        AdmissionRequest updatedAdmission = admissionRequestRepository.findById(admission.getId()).orElseThrow();
        assertThat(updatedAdmission.isDiversionRecommended()).isTrue();
        assertThat(updatedAdmission.getDiversionPathway()).isEqualTo(DiversionPathway.COMMUNITY_HOSPITAL);
        assertThat(updatedAdmission.getSecondaryAcuityTier()).isEqualTo(AcuityTier.TIER_4_SUBACUTE_DIVERSION);
    }

    @Test
    @DisplayName("Tracer Bullet 6: Acuity-Driven SLA Expiry and Cluster Auto-Escalation")
    void testTracerBullet6_AcuityDrivenSlaAndAutoEscalation() throws Exception {
        // Step 1: Submit ED assessment for P104 (Orthopaedics, TIER_3_ACUTE_STABLE) requiring consult
        Patient p104 = patientRepository.findByQueueToken("TOKEN-P104").orElseThrow();

        EdAssessmentSubmitRequest submitReq = EdAssessmentSubmitRequest.builder()
                .patientId(p104.getId())
                .suspectedDiagnosisService(SpecialtyCluster.ORTHOPAEDICS)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .requestedWardClass(WardClass.B2)
                .needsTelemetry(false)
                .requiresSpecialistConsult(true)
                .build();

        mockMvc.perform(post("/api/v1/clinicians/ed/assessments/submit")
                        .with(csrf())
                        .header("X-User-Role", "ED_ATTENDING")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk());

        AdmissionRequest admission = admissionRequestRepository.findAll().stream()
                .filter(a -> a.getPatient().getId().equals(p104.getId()))
                .findFirst().orElseThrow();

        AssessmentBroadcast broadcast = broadcastRepository.findByAdmissionRequest_Id(admission.getId()).stream()
                .filter(b -> b.getTargetCluster() == SpecialtyCluster.ORTHOPAEDICS)
                .findFirst().orElseThrow();

        assertThat(broadcast.getStatus()).isEqualTo(BroadcastStatus.OPEN);

        // Step 2: Trigger auto-escalation check at +20 mins -> Tier 3 has 30m SLA, so Ortho broadcast should NOT escalate yet
        LocalDateTime tPlus20 = LocalDateTime.now().plusMinutes(20);
        clinicianService.autoEscalateOverdueBroadcasts(tPlus20);

        AssessmentBroadcast stillOpen = broadcastRepository.findById(broadcast.getId()).orElseThrow();
        assertThat(stillOpen.getStatus()).isEqualTo(BroadcastStatus.OPEN);
        assertThat(stillOpen.getClaimedBySpecialistId()).isNull();

        // Step 3: Trigger auto-escalation check at +31 mins -> Exceeds 30m SLA -> should escalate to dr_lee_ortho
        LocalDateTime tPlus31 = LocalDateTime.now().plusMinutes(31);
        int escalatedCountLate = clinicianService.autoEscalateOverdueBroadcasts(tPlus31);
        assertThat(escalatedCountLate).isGreaterThanOrEqualTo(1);

        AssessmentBroadcast escalated = broadcastRepository.findById(broadcast.getId()).orElseThrow();
        assertThat(escalated.getStatus()).isEqualTo(BroadcastStatus.AUTO_ESCALATED);
        assertThat(escalated.getClaimedBySpecialistId()).isEqualTo("dr_lee_ortho");
        assertThat(escalated.getClaimedAt()).isNotNull();

        // Step 4: Verify specialist feed displays escalated broadcast
        mockMvc.perform(get("/api/v1/clinicians/specialist/broadcasts")
                        .param("cluster", "ORTHOPAEDICS")
                        .header("X-User-Role", "SPECIALIST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + broadcast.getId() + "')].status").value("AUTO_ESCALATED"))
                .andExpect(jsonPath("$[?(@.id == '" + broadcast.getId() + "')].claimedBySpecialistId").value("dr_lee_ortho"));

        // Step 5: Escalated specialist conducts consult evaluation and submits consult
        SpecialistConsultRequest consultReq = SpecialistConsultRequest.builder()
                .secondaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .secondaryTelemetry(false)
                .consultNotes("Escalated review completed: Conservative fracture management indicated.")
                .diversionPathway(DiversionPathway.NONE)
                .diversionRecommended(false)
                .build();

        mockMvc.perform(post("/api/v1/clinicians/specialist/broadcasts/" + broadcast.getId() + "/consult")
                        .with(csrf())
                        .header("X-User-Role", "SPECIALIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(consultReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Tracer Bullet 7: Multi-Broadcast Consensus Gate, Acuity Escalation & BMU Dispatch")
    void testTracerBullet7_ConsensusCompletionGateAndSafetyFirstBmuDispatch() throws Exception {
        // Step 1: Submit ED assessment for P103 with multiple target clusters (CARDIOLOGY + SURGERY)
        Patient p103 = patientRepository.findByQueueToken("TOKEN-P103").orElseThrow();

        EdAssessmentSubmitRequest submitReq = EdAssessmentSubmitRequest.builder()
                .patientId(p103.getId())
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .requestedWardClass(WardClass.B2)
                .needsTelemetry(false)
                .requiresSpecialistConsult(true)
                .targetClusters(java.util.Set.of(SpecialtyCluster.CARDIOLOGY, SpecialtyCluster.SURGERY))
                .build();

        mockMvc.perform(post("/api/v1/clinicians/ed/assessments/submit")
                        .with(csrf())
                        .header("X-User-Role", "ED_ATTENDING")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk());

        AdmissionRequest admission = admissionRequestRepository.findAll().stream()
                .filter(a -> a.getPatient().getId().equals(p103.getId()))
                .findFirst().orElseThrow();

        List<AssessmentBroadcast> broadcasts = broadcastRepository.findByAdmissionRequest_Id(admission.getId());
        assertThat(broadcasts).hasSize(2);

        AssessmentBroadcast cardioBroadcast = broadcasts.stream()
                .filter(b -> b.getTargetCluster() == SpecialtyCluster.CARDIOLOGY)
                .findFirst().orElseThrow();
        AssessmentBroadcast surgBroadcast = broadcasts.stream()
                .filter(b -> b.getTargetCluster() == SpecialtyCluster.SURGERY)
                .findFirst().orElseThrow();

        // Step 2: Cardiology specialist claims and submits consult (concordant, stable)
        mockMvc.perform(post("/api/v1/clinicians/specialist/broadcasts/" + cardioBroadcast.getId() + "/claim")
                        .with(csrf())
                        .header("X-User-Role", "SPECIALIST"))
                .andExpect(status().isOk());

        SpecialistConsultRequest cardioConsult = SpecialistConsultRequest.builder()
                .secondaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .secondaryTelemetry(false)
                .consultNotes("ECG and biomarkers normal.")
                .build();

        mockMvc.perform(post("/api/v1/clinicians/specialist/broadcasts/" + cardioBroadcast.getId() + "/consult")
                        .with(csrf())
                        .header("X-User-Role", "SPECIALIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardioConsult)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Consensus Completion Gate verification: surgBroadcast is still OPEN, so admission MUST remain ASSESSMENT_PENDING
        AdmissionRequest midAdmission = admissionRequestRepository.findById(admission.getId()).orElseThrow();
        assertThat(midAdmission.getStatus()).isEqualTo(AdmissionStatus.ASSESSMENT_PENDING);

        // Verify patient does NOT appear in BMU queue yet
        mockMvc.perform(get("/api/v1/bmu/queue")
                        .header("X-User-Role", "BMU_COORDINATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.patient.queueToken == 'TOKEN-P103')]").doesNotExist());

        // Step 3: Surgery specialist claims and submits consult (elevates acuity to TIER_1_CRITICAL + telemetry)
        mockMvc.perform(post("/api/v1/clinicians/specialist/broadcasts/" + surgBroadcast.getId() + "/claim")
                        .with(csrf())
                        .header("X-User-Role", "SPECIALIST"))
                .andExpect(status().isOk());

        SpecialistConsultRequest surgConsult = SpecialistConsultRequest.builder()
                .secondaryAcuityTier(AcuityTier.TIER_1_CRITICAL)
                .secondaryTelemetry(true)
                .consultNotes("Surgical emergency: Immediate resuscitation and ICU bed required.")
                .build();

        mockMvc.perform(post("/api/v1/clinicians/specialist/broadcasts/" + surgBroadcast.getId() + "/consult")
                        .with(csrf())
                        .header("X-User-Role", "SPECIALIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(surgConsult)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Consensus Completion Gate verification: All broadcasts are now COMPLETED -> status advances to BED_REQUESTED
        AdmissionRequest finalAdmission = admissionRequestRepository.findById(admission.getId()).orElseThrow();
        assertThat(finalAdmission.getStatus()).isEqualTo(AdmissionStatus.BED_REQUESTED);
        assertThat(finalAdmission.getEffectiveAcuityTier()).isEqualTo(AcuityTier.TIER_1_CRITICAL);
        assertThat(finalAdmission.getEffectiveTelemetry()).isTrue();
        assertThat(finalAdmission.getIsDiscordant()).isTrue();

        // Step 4: Verify patient appears in BMU queue with elevated priority
        mockMvc.perform(get("/api/v1/bmu/queue")
                        .header("X-User-Role", "BMU_COORDINATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patient.queueToken").value("TOKEN-P103"))
                .andExpect(jsonPath("$[0].effectiveAcuityTier").value("TIER_1_CRITICAL"))
                .andExpect(jsonPath("$[0].effectiveTelemetry").value(true))
                .andExpect(jsonPath("$[0].discordant").value(true));
    }

    @Test
    @DisplayName("Tracer Bullet 8: In-Place Consult Amendments & Non-Blocking BMU Clinical Reconciliation")
    void testTracerBullet8_InPlaceConsultAmendmentsAndNonBlockingReconciliation() throws Exception {
        // Step 1: Create dedicated patient and submit ED assessment requiring Cardiology consult
        Patient pAmend = patientRepository.save(Patient.builder()
                .name("Amendment Flow Patient")
                .nricMasked("S****888X")
                .age(58)
                .gender(Gender.MALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(15)
                .queueToken("TOKEN-AMEND-" + java.util.UUID.randomUUID())
                .build());

        EdAssessmentSubmitRequest submitReq = EdAssessmentSubmitRequest.builder()
                .patientId(pAmend.getId())
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .requestedWardClass(WardClass.B2)
                .needsTelemetry(false)
                .requiresSpecialistConsult(true)
                .targetClusters(java.util.Set.of(SpecialtyCluster.CARDIOLOGY))
                .build();

        mockMvc.perform(post("/api/v1/clinicians/ed/assessments/submit")
                        .with(csrf())
                        .header("X-User-Role", "ED_ATTENDING")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk());

        AdmissionRequest admission = admissionRequestRepository.findAll().stream()
                .filter(a -> a.getPatient().getId().equals(pAmend.getId()))
                .findFirst().orElseThrow();
        LocalDateTime originalRequestedAt = admission.getRequestedAt();

        AssessmentBroadcast broadcast = broadcastRepository.findByAdmissionRequest_Id(admission.getId()).get(0);

        // Claim and complete consult with higher acuity (TIER_2_ACUTE_URGENT) and telemetry
        mockMvc.perform(post("/api/v1/clinicians/specialist/broadcasts/" + broadcast.getId() + "/claim")
                        .with(csrf())
                        .header("X-User-Role", "SPECIALIST"))
                .andExpect(status().isOk());

        SpecialistConsultRequest consultReq = SpecialistConsultRequest.builder()
                .secondaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .secondaryTelemetry(true)
                .consultNotes("Specialist impression: Urgent telemetry monitoring required.")
                .build();

        mockMvc.perform(post("/api/v1/clinicians/specialist/broadcasts/" + broadcast.getId() + "/consult")
                        .with(csrf())
                        .header("X-User-Role", "SPECIALIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(consultReq)))
                .andExpect(status().isOk());

        AdmissionRequest queuedReq = admissionRequestRepository.findById(admission.getId()).orElseThrow();
        assertThat(queuedReq.getStatus()).isEqualTo(AdmissionStatus.BED_REQUESTED);
        assertThat(queuedReq.getEffectiveAcuityTier()).isEqualTo(AcuityTier.TIER_2_ACUTE_URGENT);
        assertThat(queuedReq.getEffectiveTelemetry()).isTrue();
        assertThat(queuedReq.getIsDiscordant()).isTrue();

        // Step 2: BMU coordinator triggers 1-click clinical reconciliation request
        mockMvc.perform(post("/api/v1/bmu/requests/" + admission.getId() + "/reconcile")
                        .with(csrf())
                        .header("X-User-Role", "BMU_COORDINATOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reconciliationRequested").value(true));

        AdmissionRequest reconciledReq = admissionRequestRepository.findById(admission.getId()).orElseThrow();
        assertThat(reconciledReq.getReconciliationRequested()).isTrue();

        // Step 3: Tentative bed allocation proceeds without being blocked by pending reconciliation
        Ward targetWard = bedRepository.findAll().get(0).getWard();
        Bed availableBed = bedRepository.save(Bed.builder()
                .bedNumber("TEST-AMEND-BED")
                .ward(targetWard)
                .status(BedStatus.EMPTY_CLEANED)
                .hasTelemetry(true)
                .isNearNursingStation(false)
                .build());

        com.hospital.admissions.dto.BedAllocationRequest allocReq = new com.hospital.admissions.dto.BedAllocationRequest();
        allocReq.setAdmissionRequestId(admission.getId());
        allocReq.setBedId(availableBed.getId());
        allocReq.setRank(1);
        allocReq.setScore(90.0);

        mockMvc.perform(post("/api/v1/bmu/allocate")
                        .with(csrf())
                        .header("X-User-Role", "BMU_COORDINATOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allocReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BED_ALLOCATED"))
                .andExpect(jsonPath("$.reconciliationRequested").value(true));

        // Step 4: Specialist amends consult in-place via PUT
        SpecialistConsultRequest amendReq = SpecialistConsultRequest.builder()
                .secondaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .secondaryTelemetry(false)
                .consultNotes("Re-evaluated post-medication: Patient stabilized, telemetry no longer critical. Aligned with ED.")
                .build();

        mockMvc.perform(put("/api/v1/clinicians/specialist/broadcasts/" + broadcast.getId() + "/consult")
                        .with(csrf())
                        .header("X-User-Role", "SPECIALIST")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(amendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secondaryAcuityTier").value("TIER_3_ACUTE_STABLE"))
                .andExpect(jsonPath("$.secondaryTelemetry").value(false));

        AdmissionRequest amendedAdmission = admissionRequestRepository.findById(admission.getId()).orElseThrow();
        // In-place effective acuity updated
        assertThat(amendedAdmission.getEffectiveAcuityTier()).isEqualTo(AcuityTier.TIER_3_ACUTE_STABLE);
        assertThat(amendedAdmission.getEffectiveTelemetry()).isFalse();
        // Discordance auto-cleared
        assertThat(amendedAdmission.getIsDiscordant()).isFalse();
        // Queue dwell time preserved
        assertThat(amendedAdmission.getRequestedAt()).isEqualTo(originalRequestedAt);
        // Clinical condition updated alert flagged
        assertThat(amendedAdmission.getClinicalConditionUpdated()).isTrue();
    }

    @Test
    @DisplayName("Tracer Bullet 9: BMU Admitting Specialty Placement & Dynamic Bed Reallocation")
    void testTracerBullet9_AdmittingSpecialtyPlacementAndDynamicReallocation() throws Exception {
        // Step 1: Create dedicated patient and submit ED assessment
        Patient pRealloc = patientRepository.save(Patient.builder()
                .name("Reallocation Flow Patient")
                .nricMasked("S****999Z")
                .age(62)
                .gender(Gender.MALE)
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(20)
                .queueToken("TOKEN-REALLOC-" + java.util.UUID.randomUUID())
                .build());

        EdAssessmentSubmitRequest submitReq = EdAssessmentSubmitRequest.builder()
                .patientId(pRealloc.getId())
                .suspectedDiagnosisService(SpecialtyCluster.GENERAL_MEDICINE)
                .primaryAcuityTier(AcuityTier.TIER_3_ACUTE_STABLE)
                .requestedWardClass(WardClass.B2)
                .needsTelemetry(true)
                .requiresSpecialistConsult(false)
                .build();

        mockMvc.perform(post("/api/v1/clinicians/ed/assessments/submit")
                        .with(csrf())
                        .header("X-User-Role", "ED_ATTENDING")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk());

        AdmissionRequest admission = admissionRequestRepository.findAll().stream()
                .filter(a -> a.getPatient().getId().equals(pRealloc.getId()))
                .findFirst().orElseThrow();
        assertThat(admission.getStatus()).isEqualTo(AdmissionStatus.BED_REQUESTED);

        // Step 2: BMU coordinator assigns authoritative admitting specialty cluster (CARDIOLOGY)
        com.hospital.admissions.dto.AdmittingClusterRequest clusterReq =
                com.hospital.admissions.dto.AdmittingClusterRequest.builder()
                        .admittingSpecialtyCluster(SpecialtyCluster.CARDIOLOGY)
                        .build();

        mockMvc.perform(post("/api/v1/bmu/requests/" + admission.getId() + "/admitting-cluster")
                        .with(csrf())
                        .header("X-User-Role", "BMU_COORDINATOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clusterReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admittingSpecialtyCluster").value("CARDIOLOGY"));

        AdmissionRequest updatedReq = admissionRequestRepository.findById(admission.getId()).orElseThrow();
        assertThat(updatedReq.getAdmittingSpecialtyCluster()).isEqualTo(SpecialtyCluster.CARDIOLOGY);

        // Step 3: Setup two available beds in B2 ward
        Ward targetWard = wardRepository.findAll().stream()
                .filter(w -> w.getWardClass() == WardClass.B2)
                .findFirst().orElseThrow();

        Bed bedA = bedRepository.save(Bed.builder()
                .bedNumber("TEST-REALLOC-A-" + java.util.UUID.randomUUID())
                .ward(targetWard)
                .status(BedStatus.EMPTY_CLEANED)
                .hasTelemetry(true)
                .isNearNursingStation(false)
                .build());

        Bed bedB = bedRepository.save(Bed.builder()
                .bedNumber("TEST-REALLOC-B-" + java.util.UUID.randomUUID())
                .ward(targetWard)
                .status(BedStatus.EMPTY_CLEANED)
                .hasTelemetry(true)
                .isNearNursingStation(true)
                .build());

        // Step 4: Allocate Bed A tentatively
        com.hospital.admissions.dto.BedAllocationRequest allocReqA = new com.hospital.admissions.dto.BedAllocationRequest();
        allocReqA.setAdmissionRequestId(admission.getId());
        allocReqA.setBedId(bedA.getId());
        allocReqA.setRank(1);
        allocReqA.setScore(85.0);

        mockMvc.perform(post("/api/v1/bmu/allocate")
                        .with(csrf())
                        .header("X-User-Role", "BMU_COORDINATOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allocReqA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BED_ALLOCATED"))
                .andExpect(jsonPath("$.assignedBed.id").value(bedA.getId().toString()));

        Bed bedAfterAllocA = bedRepository.findById(bedA.getId()).orElseThrow();
        assertThat(bedAfterAllocA.getStatus()).isEqualTo(BedStatus.EMPTY_ASSIGNED);
        assertThat(bedAfterAllocA.getCurrentPatient().getId()).isEqualTo(pRealloc.getId());

        // Step 5: Dynamic Reallocation to Bed B - old Bed A must be reverted to EMPTY_CLEANED (zero ghost beds)
        com.hospital.admissions.dto.BedAllocationRequest allocReqB = new com.hospital.admissions.dto.BedAllocationRequest();
        allocReqB.setAdmissionRequestId(admission.getId());
        allocReqB.setBedId(bedB.getId());
        allocReqB.setRank(2);
        allocReqB.setScore(95.0);
        allocReqB.setOverrideReason("Better proximity to nursing station for continuous telemetry monitoring");

        mockMvc.perform(post("/api/v1/bmu/allocate")
                        .with(csrf())
                        .header("X-User-Role", "BMU_COORDINATOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allocReqB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BED_ALLOCATED"))
                .andExpect(jsonPath("$.assignedBed.id").value(bedB.getId().toString()));

        // Verify Bed B is EMPTY_ASSIGNED to patient
        Bed bedAfterReallocB = bedRepository.findById(bedB.getId()).orElseThrow();
        assertThat(bedAfterReallocB.getStatus()).isEqualTo(BedStatus.EMPTY_ASSIGNED);
        assertThat(bedAfterReallocB.getCurrentPatient().getId()).isEqualTo(pRealloc.getId());

        // Verify Bed A is safely reverted back to EMPTY_CLEANED and currentPatient is null
        Bed bedAfterReallocA = bedRepository.findById(bedA.getId()).orElseThrow();
        assertThat(bedAfterReallocA.getStatus()).isEqualTo(BedStatus.EMPTY_CLEANED);
        assertThat(bedAfterReallocA.getCurrentPatient()).isNull();
    }

    @Test
    @DisplayName("Tracer Bullet 10: Acuity Dwell SLA Tracking, Closed-Loop Delay Tagging, and Auto-Archival")
    void testTracerBullet10_AcuityDwellSla_DelayTagging_AutoArchival() throws Exception {
        // Step 1: Create patient and admission request with Tier 2 Urgent and dwell time > 60m
        Patient patient = Patient.builder()
                .name("SLA Test Patient")
                .nricMasked("S****888X")
                .age(58)
                .gender(Gender.FEMALE)
                .queueToken("Q-SLA-10")
                .infectionStatus(InfectionStatus.NON_INFECTIOUS)
                .fallRiskScore(20)
                .needsTelemetry(false)
                .build();
        patient = patientRepository.save(patient);

        AdmissionRequest admission = AdmissionRequest.builder()
                .patient(patient)
                .suspectedDiagnosisService(SpecialtyCluster.GENERAL_MEDICINE)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedWardClass(WardClass.B2)
                .requestedAt(LocalDateTime.now().minusMinutes(75))
                .status(AdmissionStatus.BED_REQUESTED)
                .build();
        admission = admissionRequestRepository.save(admission);

        // Step 2: Attempt delay tagging by non-BMU persona (ED Attending) -> Rejected 403 Forbidden
        DelayTagRequest delayReq = DelayTagRequest.builder()
                .delayReasonCode(DelayReasonCode.HOUSEKEEPING_DELAY)
                .note("EVS terminal sanitization in progress")
                .build();

        mockMvc.perform(post("/api/v1/bmu/requests/" + admission.getId() + "/delay-tag")
                        .with(csrf())
                        .header("X-User-Role", "ED_ATTENDING")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(delayReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"));

        // Step 3: Authorized BMU Coordinator attaches delay reason tag -> 200 OK
        mockMvc.perform(post("/api/v1/bmu/requests/" + admission.getId() + "/delay-tag")
                        .with(csrf())
                        .header("X-User-Role", "BMU_COORDINATOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(delayReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delayReasonTag").value("HOUSEKEEPING_DELAY"))
                .andExpect(jsonPath("$.operationalDelayReason").value("EVS terminal sanitization in progress"));

        AdmissionRequest taggedAdmission = admissionRequestRepository.findById(admission.getId()).orElseThrow();
        assertThat(taggedAdmission.getDelayReasonTag()).isEqualTo("HOUSEKEEPING_DELAY");
        assertThat(taggedAdmission.getOperationalDelayReason()).isEqualTo("EVS terminal sanitization in progress");

        // Step 4: Allocate Bed -> Transitions to BED_ALLOCATED, archives active delay tag
        Ward ward = wardRepository.findAll().stream()
                .filter(w -> w.getWardClass() == WardClass.B2 && (w.getLockedGender() == null || w.getLockedGender() == Gender.FEMALE))
                .findFirst()
                .orElseThrow();
        Bed targetBed = bedRepository.findByWard_Id(ward.getId()).stream()
                .filter(b -> b.getStatus() == BedStatus.EMPTY_CLEANED)
                .findFirst()
                .orElseThrow();

        BedAllocationRequest allocReq = new BedAllocationRequest();
        allocReq.setAdmissionRequestId(admission.getId());
        allocReq.setBedId(targetBed.getId());
        allocReq.setRank(1);
        allocReq.setScore(90.0);

        mockMvc.perform(post("/api/v1/bmu/allocate")
                        .with(csrf())
                        .header("X-User-Role", "BMU_COORDINATOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allocReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BED_ALLOCATED"));

        AdmissionRequest allocatedAdmission = admissionRequestRepository.findById(admission.getId()).orElseThrow();
        assertThat(allocatedAdmission.getStatus()).isEqualTo(AdmissionStatus.BED_ALLOCATED);
        assertThat(allocatedAdmission.getDelayReasonTag()).isNull();
        assertThat(allocatedAdmission.getArchivedDelayReasonTag()).isEqualTo("HOUSEKEEPING_DELAY");
    }
}

