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
}
