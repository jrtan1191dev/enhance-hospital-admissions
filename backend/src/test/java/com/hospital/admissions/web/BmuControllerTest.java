package com.hospital.admissions.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.admissions.domain.AdmissionRequest;
import com.hospital.admissions.domain.BmuAlgorithmConfig;
import com.hospital.admissions.domain.Ward;
import com.hospital.admissions.dto.BedAllocationRequest;
import com.hospital.admissions.dto.BedRecommendation;
import com.hospital.admissions.dto.BmuConfigUpdateRequest;
import com.hospital.admissions.dto.DiversionReferralRequest;
import com.hospital.admissions.dto.SisterHospitalReferralResponse;
import com.hospital.admissions.service.BmuService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BmuControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BmuService bmuService;

    @InjectMocks
    private BmuController bmuController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(bmuController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/bmu/queue returns 200 and list")
    void testGetPrioritizedQueue() throws Exception {
        AdmissionRequest req = AdmissionRequest.builder().id(UUID.randomUUID()).build();
        when(bmuService.getPrioritizedQueue()).thenReturn(List.of(req));

        mockMvc.perform(get("/api/v1/bmu/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/bmu/recommendations/{requestId} returns 200 and recommendations")
    void testGetRecommendations() throws Exception {
        UUID reqId = UUID.randomUUID();
        BedRecommendation rec = BedRecommendation.builder().bedNumber("8A-01").score(85).build();
        when(bmuService.getRecommendations(reqId)).thenReturn(List.of(rec));

        mockMvc.perform(get("/api/v1/bmu/recommendations/" + reqId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bedNumber").value("8A-01"));
    }

    @Test
    @DisplayName("POST /api/v1/bmu/allocate returns 200 and allocated request")
    void testAllocateBed() throws Exception {
        UUID reqId = UUID.randomUUID();
        UUID bedId = UUID.randomUUID();
        BedAllocationRequest allocReq = BedAllocationRequest.builder()
                .admissionRequestId(reqId)
                .bedId(bedId)
                .build();

        AdmissionRequest allocated = AdmissionRequest.builder().id(reqId).build();
        when(bmuService.allocateBed(reqId, bedId)).thenReturn(allocated);

        mockMvc.perform(post("/api/v1/bmu/allocate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(allocReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reqId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/bmu/inventory returns 200 and ward inventory")
    void testGetInventory() throws Exception {
        Ward ward = Ward.builder().name("Ward 8A").build();
        when(bmuService.getInventory()).thenReturn(List.of(ward));

        mockMvc.perform(get("/api/v1/bmu/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ward 8A"));
    }

    @Test
    @DisplayName("GET /api/v1/bmu/config returns 200 and config")
    void testGetConfig() throws Exception {
        BmuAlgorithmConfig config = BmuAlgorithmConfig.builder().weightSpecialtyCluster(40).build();
        when(bmuService.getOrCreateConfig()).thenReturn(config);

        mockMvc.perform(get("/api/v1/bmu/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weightSpecialtyCluster").value(40));
    }

    @Test
    @DisplayName("PUT /api/v1/bmu/config returns 200 and updated config")
    void testUpdateConfig() throws Exception {
        BmuConfigUpdateRequest updateReq = BmuConfigUpdateRequest.builder()
                .weightSpecialtyCluster(50)
                .weightConsolidation(25)
                .weightFallRiskStation(20)
                .batchHoldingWardThreshold(4)
                .build();

        BmuAlgorithmConfig updated = BmuAlgorithmConfig.builder().weightSpecialtyCluster(50).build();
        when(bmuService.updateConfig(any(BmuConfigUpdateRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/bmu/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weightSpecialtyCluster").value(50));
    }

    @Test
    @DisplayName("POST /api/v1/bmu/diversion/refer returns 200 and referral response")
    void testReferToSisterHospital() throws Exception {
        UUID reqId = UUID.randomUUID();
        DiversionReferralRequest refReq = DiversionReferralRequest.builder()
                .admissionRequestId(reqId)
                .facility("OCH")
                .build();

        SisterHospitalReferralResponse response = SisterHospitalReferralResponse.builder()
                .referralId("REF-OCH-001")
                .status("ACCEPTED_30MIN_SLA")
                .build();

        when(bmuService.referToSisterHospital(eq(reqId), eq("OCH"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/bmu/diversion/refer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referralId").value("REF-OCH-001"));
    }
}
