package com.hospital.admissions.controller;

import tools.jackson.databind.json.JsonMapper;
import com.hospital.admissions.entity.*;
import com.hospital.admissions.dto.EdAssessmentSubmitRequest;
import com.hospital.admissions.dto.SpecialistConsultRequest;
import com.hospital.admissions.service.ClinicianService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClinicianControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ClinicianService clinicianService;

    @InjectMocks
    private ClinicianController clinicianController;

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(clinicianController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/clinicians/ed/patients returns 200 and list")
    void testGetEdWaitingPatients() throws Exception {
        Patient patient = Patient.builder().id(UUID.randomUUID()).name("Tan Ah Meng").build();
        when(clinicianService.getEdWaitingPatients()).thenReturn(List.of(patient));

        mockMvc.perform(get("/api/v1/clinicians/ed/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Tan Ah Meng"));
    }

    @Test
    @DisplayName("GET /api/v1/clinicians/ed/admissions returns 200 and list of submitted admissions")
    void testGetEdSubmittedAdmissions() throws Exception {
        AdmissionRequest req = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .status(AdmissionStatus.BED_REQUESTED)
                .effectiveAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .build();
        when(clinicianService.getEdSubmittedAdmissions()).thenReturn(List.of(req));

        mockMvc.perform(get("/api/v1/clinicians/ed/admissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("BED_REQUESTED"))
                .andExpect(jsonPath("$[0].effectiveAcuityTier").value("TIER_2_ACUTE_URGENT"));
    }

    @Test
    @DisplayName("POST /api/v1/clinicians/ed/assessments/submit returns 200 and admission request")
    void testSubmitEdAssessment() throws Exception {
        UUID patientId = UUID.randomUUID();
        EdAssessmentSubmitRequest req = EdAssessmentSubmitRequest.builder()
                .patientId(patientId)
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .primaryAcuityTier(AcuityTier.TIER_2_ACUTE_URGENT)
                .requestedWardClass(WardClass.B2)
                .needsTelemetry(true)
                .build();

        AdmissionRequest admissionRequest = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .status(AdmissionStatus.BED_REQUESTED)
                .suspectedDiagnosisService(SpecialtyCluster.CARDIOLOGY)
                .build();

        when(clinicianService.submitEdAssessment(any(EdAssessmentSubmitRequest.class))).thenReturn(admissionRequest);

        mockMvc.perform(post("/api/v1/clinicians/ed/assessments/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BED_REQUESTED"));
    }

    @Test
    @DisplayName("POST /api/v1/clinicians/ed/assessments/submit with invalid request returns 400")
    void testSubmitEdAssessment_InvalidRequest() throws Exception {
        // Missing mandatory @NotNull fields
        EdAssessmentSubmitRequest req = EdAssessmentSubmitRequest.builder().build();

        mockMvc.perform(post("/api/v1/clinicians/ed/assessments/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/clinicians/specialist/broadcasts returns 200 and list")
    void testGetSpecialistBroadcasts() throws Exception {
        AssessmentBroadcast broadcast = AssessmentBroadcast.builder()
                .id(UUID.randomUUID())
                .targetCluster(SpecialtyCluster.CARDIOLOGY)
                .build();

        when(clinicianService.getBroadcasts(SpecialtyCluster.CARDIOLOGY)).thenReturn(List.of(broadcast));

        mockMvc.perform(get("/api/v1/clinicians/specialist/broadcasts")
                        .param("cluster", "CARDIOLOGY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].targetCluster").value("CARDIOLOGY"));
    }

    @Test
    @DisplayName("POST /api/v1/clinicians/specialist/broadcasts/{id}/claim returns 200")
    void testClaimBroadcast() throws Exception {
        UUID broadcastId = UUID.randomUUID();
        AssessmentBroadcast broadcast = AssessmentBroadcast.builder()
                .id(broadcastId)
                .status(BroadcastStatus.CLAIMED)
                .build();

        when(clinicianService.claimBroadcast(broadcastId)).thenReturn(broadcast);

        mockMvc.perform(post("/api/v1/clinicians/specialist/broadcasts/" + broadcastId + "/claim"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLAIMED"));
    }

    @Test
    @DisplayName("POST /api/v1/clinicians/specialist/broadcasts/{id}/claim conflict returns 409 Conflict ProblemDetail")
    void testClaimBroadcast_ConflictReturns409() throws Exception {
        UUID broadcastId = UUID.randomUUID();
        when(clinicianService.claimBroadcast(broadcastId))
                .thenThrow(new IllegalStateException("Broadcast has already been claimed or is no longer open: " + broadcastId));

        mockMvc.perform(post("/api/v1/clinicians/specialist/broadcasts/" + broadcastId + "/claim"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Broadcast has already been claimed or is no longer open: " + broadcastId));
    }

    @Test
    @DisplayName("POST /api/v1/clinicians/specialist/broadcasts/{id}/consult returns 200")
    void testSubmitConsult() throws Exception {
        UUID broadcastId = UUID.randomUUID();
        SpecialistConsultRequest req = SpecialistConsultRequest.builder()
                .consultNotes("Clear for diversion")
                .secondaryAcuityTier(AcuityTier.TIER_4_SUBACUTE_DIVERSION)
                .diversionRecommended(true)
                .build();

        AssessmentBroadcast broadcast = AssessmentBroadcast.builder()
                .id(broadcastId)
                .status(BroadcastStatus.COMPLETED)
                .consultNotes("Clear for diversion")
                .build();

        when(clinicianService.submitConsult(eq(broadcastId), any(SpecialistConsultRequest.class)))
                .thenReturn(broadcast);

        mockMvc.perform(post("/api/v1/clinicians/specialist/broadcasts/" + broadcastId + "/consult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /api/v1/clinicians/specialist/broadcasts/{id}/chain returns 200 and chained broadcast")
    void testChainConsult() throws Exception {
        UUID broadcastId = UUID.randomUUID();
        com.hospital.admissions.dto.ChainConsultRequest req = com.hospital.admissions.dto.ChainConsultRequest.builder()
                .targetCluster(SpecialtyCluster.ORTHOPAEDICS)
                .rationale("Suspected pelvic fracture")
                .build();

        AssessmentBroadcast chained = AssessmentBroadcast.builder()
                .id(UUID.randomUUID())
                .targetCluster(SpecialtyCluster.ORTHOPAEDICS)
                .status(BroadcastStatus.OPEN)
                .parentBroadcastId(broadcastId)
                .build();

        when(clinicianService.chainConsult(eq(broadcastId), any(com.hospital.admissions.dto.ChainConsultRequest.class)))
                .thenReturn(chained);

        mockMvc.perform(post("/api/v1/clinicians/specialist/broadcasts/" + broadcastId + "/chain")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.targetCluster").value("ORTHOPAEDICS"))
                .andExpect(jsonPath("$.parentBroadcastId").value(broadcastId.toString()));
    }

    @Test
    @DisplayName("PUT /api/v1/clinicians/specialist/broadcasts/{id}/consult returns 200 and amended broadcast")
    void testAmendConsult() throws Exception {
        UUID broadcastId = UUID.randomUUID();
        SpecialistConsultRequest req = SpecialistConsultRequest.builder()
                .secondaryAcuityTier(AcuityTier.TIER_1_CRITICAL)
                .secondaryTelemetry(true)
                .consultNotes("Amended consult: Acute peritonitis diagnosed.")
                .build();

        AssessmentBroadcast broadcast = AssessmentBroadcast.builder()
                .id(broadcastId)
                .secondaryAcuityTier(AcuityTier.TIER_1_CRITICAL)
                .secondaryTelemetry(true)
                .consultNotes("Amended consult: Acute peritonitis diagnosed.")
                .status(BroadcastStatus.COMPLETED)
                .build();

        when(clinicianService.amendConsult(eq(broadcastId), any(SpecialistConsultRequest.class)))
                .thenReturn(broadcast);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/clinicians/specialist/broadcasts/" + broadcastId + "/consult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secondaryAcuityTier").value("TIER_1_CRITICAL"))
                .andExpect(jsonPath("$.secondaryTelemetry").value(true));
    }
}
