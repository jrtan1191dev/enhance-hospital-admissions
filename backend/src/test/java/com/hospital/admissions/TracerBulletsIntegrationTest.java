package com.hospital.admissions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.admissions.domain.*;
import com.hospital.admissions.dto.*;
import com.hospital.admissions.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
                .build();

        mockMvc.perform(post("/api/v1/clinicians/ed/assessments/submit")
                        .with(csrf())
                        .header("X-User-Role", "ED_ATTENDING")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BED_REQUESTED"))
                .andExpect(jsonPath("$.suspectedDiagnosisService").value("GENERAL_MEDICINE"))
                .andExpect(jsonPath("$.primaryAcuityTier").value("TIER_3_ACUTE_STABLE"))
                .andExpect(jsonPath("$.requestedWardClass").value("B2"));

        List<AssessmentBroadcast> broadcasts = broadcastRepository.findAll();
        assertThat(broadcasts).anyMatch(b -> b.getTargetCluster() == SpecialtyCluster.GENERAL_MEDICINE
                && b.getAdmissionRequest().getPatient().getId().equals(p102.getId()));
    }
}
