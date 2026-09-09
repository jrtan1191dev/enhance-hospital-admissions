package com.hospital.admissions.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.admissions.domain.AdmissionRequest;
import com.hospital.admissions.domain.BmuAlgorithmConfig;
import com.hospital.admissions.dto.BedAllocationRequest;
import com.hospital.admissions.dto.BedRecommendation;
import com.hospital.admissions.dto.BmuConfigUpdateRequest;
import com.hospital.admissions.domain.DelayReasonCode;
import com.hospital.admissions.dto.DelayTagRequest;
import com.hospital.admissions.dto.DiversionReferralRequest;
import com.hospital.admissions.dto.SisterHospitalReferralResponse;
import com.hospital.admissions.dto.WardDto;
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
        @DisplayName("POST /api/v1/bmu/allocations/override returns 200 and allocated request")
        void testOverrideAllocation() throws Exception {
                UUID reqId = UUID.randomUUID();
                UUID bedId = UUID.randomUUID();
                BedAllocationRequest allocReq = BedAllocationRequest.builder()
                                .admissionRequestId(reqId)
                                .bedId(bedId)
                                .rank(2)
                                .score(70.0)
                                .overrideReason("GOVERNMENT_SUBSIDY_CLASS_UPGRADE")
                                .build();

                AdmissionRequest allocated = AdmissionRequest.builder().id(reqId)
                                .overrideReasonCode("GOVERNMENT_SUBSIDY_CLASS_UPGRADE").build();
                when(bmuService.allocateBed(reqId, bedId, 2, 70.0, "GOVERNMENT_SUBSIDY_CLASS_UPGRADE"))
                                .thenReturn(allocated);

                mockMvc.perform(post("/api/v1/bmu/allocations/override")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(allocReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.overrideReasonCode").value("GOVERNMENT_SUBSIDY_CLASS_UPGRADE"));
        }

        @Test
        @DisplayName("POST /api/v1/bmu/allocate returns 422 Unprocessable Entity when safety invariant violated")
        void testAllocateBed_SafetyInvariant_Returns422() throws Exception {
                UUID reqId = UUID.randomUUID();
                UUID bedId = UUID.randomUUID();
                BedAllocationRequest allocReq = BedAllocationRequest.builder()
                                .admissionRequestId(reqId)
                                .bedId(bedId)
                                .build();

                when(bmuService.allocateBed(reqId, bedId))
                                .thenThrow(new com.hospital.admissions.exception.SafetyInvariantViolationException(
                                                "Biological gender cohorting violation in multi-bed ward"));

                mockMvc.perform(post("/api/v1/bmu/allocate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(allocReq)))
                                .andExpect(status().isUnprocessableEntity())
                                .andExpect(jsonPath("$.status").value(422))
                                .andExpect(jsonPath("$.title").value("Safety Invariant Violation"));
        }

        @Test
        @DisplayName("POST /api/v1/bmu/allocate returns 400 Bad Request when operational override lacks reason code")
        void testAllocateBed_OperationalConstraintWithoutReason_Returns400() throws Exception {
                UUID reqId = UUID.randomUUID();
                UUID bedId = UUID.randomUUID();
                BedAllocationRequest allocReq = BedAllocationRequest.builder()
                                .admissionRequestId(reqId)
                                .bedId(bedId)
                                .build();

                when(bmuService.allocateBed(reqId, bedId))
                                .thenThrow(new IllegalArgumentException(
                                                "Operational constraint override requires a valid structured reason code"));

                mockMvc.perform(post("/api/v1/bmu/allocate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(allocReq)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.title").value("Bad Request"));
        }

        @Test
        @DisplayName("POST /api/v1/bmu/requests/{requestId}/allocate returns 200 and allocated request")
        void testAllocateBedForRequest() throws Exception {
                UUID reqId = UUID.randomUUID();
                UUID bedId = UUID.randomUUID();
                BedAllocationRequest allocReq = BedAllocationRequest.builder()
                                .bedId(bedId)
                                .build();

                AdmissionRequest allocated = AdmissionRequest.builder().id(reqId).build();
                when(bmuService.allocateBed(reqId, bedId, null, null, null)).thenReturn(allocated);

                mockMvc.perform(post("/api/v1/bmu/requests/" + reqId + "/allocate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(allocReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(reqId.toString()));
        }

        @Test
        @DisplayName("GET /api/v1/bmu/inventory returns 200 and ward inventory")
        void testGetInventory() throws Exception {
                WardDto wardDto = new WardDto(null, "8A", 8, null, null, null, null, List.of());
                when(bmuService.getInventory()).thenReturn(List.of(wardDto));

                mockMvc.perform(get("/api/v1/bmu/inventory"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].wardCode").value("8A"));
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

        @Test
        @DisplayName("POST /api/v1/bmu/requests/{id}/reconcile returns 200 and admission request with reconciliationRequested")
        void testRequestReconciliation() throws Exception {
                UUID reqId = UUID.randomUUID();
                AdmissionRequest req = AdmissionRequest.builder()
                                .id(reqId)
                                .reconciliationRequested(true)
                                .build();

                when(bmuService.requestReconciliation(eq(reqId))).thenReturn(req);

                mockMvc.perform(post("/api/v1/bmu/requests/" + reqId + "/reconcile"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reconciliationRequested").value(true));
        }

        @Test
        @DisplayName("POST /api/v1/bmu/requests/{id}/admitting-cluster returns 200 and updated cluster")
        void testAssignAdmittingCluster() throws Exception {
                UUID reqId = UUID.randomUUID();
                com.hospital.admissions.dto.AdmittingClusterRequest clusterReq = com.hospital.admissions.dto.AdmittingClusterRequest
                                .builder()
                                .admittingSpecialtyCluster(com.hospital.admissions.domain.SpecialtyCluster.CARDIOLOGY)
                                .build();

                AdmissionRequest updated = AdmissionRequest.builder()
                                .id(reqId)
                                .admittingSpecialtyCluster(com.hospital.admissions.domain.SpecialtyCluster.CARDIOLOGY)
                                .build();

                when(bmuService.assignAdmittingCluster(eq(reqId),
                                eq(com.hospital.admissions.domain.SpecialtyCluster.CARDIOLOGY)))
                                .thenReturn(updated);

                mockMvc.perform(post("/api/v1/bmu/requests/" + reqId + "/admitting-cluster")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(clusterReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.admittingSpecialtyCluster").value("CARDIOLOGY"));
        }

        @Test
        @DisplayName("POST /api/v1/bmu/deallocate returns 200 and deallocated request")
        void testDeallocateBed() throws Exception {
                UUID reqId = UUID.randomUUID();
                com.hospital.admissions.dto.BedDeallocationRequest deallocReq = new com.hospital.admissions.dto.BedDeallocationRequest(
                                reqId);

                AdmissionRequest deallocated = AdmissionRequest.builder().id(reqId)
                                .status(com.hospital.admissions.domain.AdmissionStatus.BED_REQUESTED).build();
                when(bmuService.deallocateBed(reqId)).thenReturn(deallocated);

                mockMvc.perform(post("/api/v1/bmu/deallocate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(deallocReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("BED_REQUESTED"));
        }

        @Test
        @DisplayName("POST /api/v1/bmu/beds/{bedId}/arrive returns 200 and OCCUPIED_TAKEN bed")
        void testConfirmArrival() throws Exception {
                UUID bedId = UUID.randomUUID();
                com.hospital.admissions.domain.Bed bed = com.hospital.admissions.domain.Bed.builder()
                                .id(bedId)
                                .status(com.hospital.admissions.domain.BedStatus.OCCUPIED_TAKEN)
                                .build();
                when(bmuService.confirmArrival(bedId)).thenReturn(bed);

                mockMvc.perform(post("/api/v1/bmu/beds/" + bedId + "/arrive"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("OCCUPIED_TAKEN"));
        }

        @Test
        @DisplayName("POST /api/v1/bmu/beds/{bedId}/vacate returns 200 and EMPTY_PENDING_CLEANING bed")
        void testVacateBed() throws Exception {
                UUID bedId = UUID.randomUUID();
                com.hospital.admissions.domain.Bed bed = com.hospital.admissions.domain.Bed.builder()
                                .id(bedId)
                                .status(com.hospital.admissions.domain.BedStatus.EMPTY_PENDING_CLEANING)
                                .build();
                when(bmuService.vacateBed(bedId)).thenReturn(bed);

                mockMvc.perform(post("/api/v1/bmu/beds/" + bedId + "/vacate"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("EMPTY_PENDING_CLEANING"));
        }

        @Test
        @DisplayName("POST /api/v1/bmu/beds/{bedId}/clean returns 200 and EMPTY_CLEANED bed")
        void testCleanBed() throws Exception {
                UUID bedId = UUID.randomUUID();
                com.hospital.admissions.domain.Bed bed = com.hospital.admissions.domain.Bed.builder()
                                .id(bedId)
                                .status(com.hospital.admissions.domain.BedStatus.EMPTY_CLEANED)
                                .build();
                when(bmuService.signOffCleaning(bedId)).thenReturn(bed);

                mockMvc.perform(post("/api/v1/bmu/beds/" + bedId + "/clean"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("EMPTY_CLEANED"));
        }

        @Test
        @DisplayName("GET /api/v1/bmu/batch-suggestions returns 200 and list of suggestions")
        void testGetBatchSuggestions() throws Exception {
                UUID wardId = UUID.randomUUID();
                com.hospital.admissions.dto.BatchSuggestion suggestion = com.hospital.admissions.dto.BatchSuggestion
                                .builder()
                                .suggestionId("SUGG-1")
                                .targetWardId(wardId)
                                .targetWardName("Ward 9C")
                                .commonWardClass(com.hospital.admissions.domain.WardClass.B2)
                                .commonGender(com.hospital.admissions.domain.Gender.MALE)
                                .build();

                when(bmuService.getBatchSuggestions()).thenReturn(List.of(suggestion));

                mockMvc.perform(get("/api/v1/bmu/batch-suggestions"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].suggestionId").value("SUGG-1"))
                                .andExpect(jsonPath("$[0].targetWardName").value("Ward 9C"));
        }

        @Test
        @DisplayName("POST /api/v1/bmu/batch-holding-wards/approve returns 200 and allocated requests")
        void testApproveBatchHoldingWard() throws Exception {
                UUID wardId = UUID.randomUUID();
                UUID reqId1 = UUID.randomUUID();
                UUID reqId2 = UUID.randomUUID();
                UUID reqId3 = UUID.randomUUID();

                com.hospital.admissions.dto.BatchApprovalRequest approval = com.hospital.admissions.dto.BatchApprovalRequest
                                .builder()
                                .targetWardId(wardId)
                                .admissionRequestIds(List.of(reqId1, reqId2, reqId3))
                                .build();

                AdmissionRequest r1 = AdmissionRequest.builder().id(reqId1)
                                .status(com.hospital.admissions.domain.AdmissionStatus.BED_ALLOCATED).build();
                AdmissionRequest r2 = AdmissionRequest.builder().id(reqId2)
                                .status(com.hospital.admissions.domain.AdmissionStatus.BED_ALLOCATED).build();
                AdmissionRequest r3 = AdmissionRequest.builder().id(reqId3)
                                .status(com.hospital.admissions.domain.AdmissionStatus.BED_ALLOCATED).build();

                when(bmuService.approveBatchHoldingWard(any(com.hospital.admissions.dto.BatchApprovalRequest.class)))
                                .thenReturn(List.of(r1, r2, r3));

                mockMvc.perform(post("/api/v1/bmu/batch-holding-wards/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(approval)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(3))
                                .andExpect(jsonPath("$[0].status").value("BED_ALLOCATED"));
        }

        @Test
        @DisplayName("GET /api/v1/bmu/cohort-swap-suggestions returns 200 and list of suggestions")
        void testGetCohortSwapSuggestions() throws Exception {
                UUID reqId = UUID.randomUUID();
                com.hospital.admissions.dto.CohortSwapSuggestion suggestion = com.hospital.admissions.dto.CohortSwapSuggestion
                                .builder()
                                .suggestionId("SWAP-1")
                                .admissionRequestId(reqId)
                                .patientName("John Doe")
                                .currentBedNumber("9A-01")
                                .targetBedNumber("8A-03")
                                .unlockedCapacityCount(4)
                                .build();

                when(bmuService.getCohortSwapSuggestions()).thenReturn(List.of(suggestion));

                mockMvc.perform(get("/api/v1/bmu/cohort-swap-suggestions"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].suggestionId").value("SWAP-1"))
                                .andExpect(jsonPath("$[0].currentBedNumber").value("9A-01"))
                                .andExpect(jsonPath("$[0].targetBedNumber").value("8A-03"));
        }

        @Test
        @DisplayName("POST /api/v1/bmu/cohort-swap/approve returns 200 and reallocated request")
        void testApproveCohortSwap() throws Exception {
                UUID reqId = UUID.randomUUID();
                UUID targetBedId = UUID.randomUUID();
                com.hospital.admissions.dto.CohortSwapApprovalRequest request = com.hospital.admissions.dto.CohortSwapApprovalRequest
                                .builder()
                                .admissionRequestId(reqId)
                                .targetBedId(targetBedId)
                                .build();

                AdmissionRequest result = AdmissionRequest.builder().id(reqId)
                                .status(com.hospital.admissions.domain.AdmissionStatus.BED_ALLOCATED).build();
                when(bmuService.approveCohortSwap(any(com.hospital.admissions.dto.CohortSwapApprovalRequest.class)))
                                .thenReturn(result);

                mockMvc.perform(post("/api/v1/bmu/cohort-swap/approve")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(reqId.toString()));
        }

        @Test
        @DisplayName("POST /api/v1/bmu/diversion/{id}/recall returns 200 and recalled request")
        void testRecallDiversion() throws Exception {
                UUID reqId = UUID.randomUUID();
                AdmissionRequest result = AdmissionRequest.builder().id(reqId)
                                .status(com.hospital.admissions.domain.AdmissionStatus.BED_REQUESTED).build();
                when(bmuService.recallDiversionToAcuteQueue(reqId)).thenReturn(result);

                mockMvc.perform(post("/api/v1/bmu/diversion/" + reqId + "/recall"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("BED_REQUESTED"));
        }

        @Test
        @DisplayName("POST /api/v1/bmu/diversion/{id}/extend-sla returns 200 and extended request")
        void testExtendDiversionSla() throws Exception {
                UUID reqId = UUID.randomUUID();
                AdmissionRequest result = AdmissionRequest.builder().id(reqId).referralSlaMinutes(45).build();
                when(bmuService.extendDiversionSla(reqId)).thenReturn(result);

                mockMvc.perform(post("/api/v1/bmu/diversion/" + reqId + "/extend-sla"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.referralSlaMinutes").value(45));
        }

        @Test
        @DisplayName("POST /api/v1/bmu/diversion/{id}/follow-up returns 200 and updated request")
        void testLogTelephoneFollowUp() throws Exception {
                UUID reqId = UUID.randomUUID();
                com.hospital.admissions.dto.FollowUpNoteRequest noteReq = new com.hospital.admissions.dto.FollowUpNoteRequest(
                                "Spoke with partner hospital");
                AdmissionRequest result = AdmissionRequest.builder().id(reqId)
                                .operationalDelayReason("Spoke with partner hospital").build();
                when(bmuService.logTelephoneFollowUp(eq(reqId), eq("Spoke with partner hospital"))).thenReturn(result);

                mockMvc.perform(post("/api/v1/bmu/diversion/" + reqId + "/follow-up")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(noteReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.operationalDelayReason").value("Spoke with partner hospital"));
        }

        @Test
        @DisplayName("POST /api/v1/bmu/requests/{id}/delay-tag returns 200 when role is BMU_COORDINATOR")
        void testAttachDelayTag_Success() throws Exception {
                UUID reqId = UUID.randomUUID();
                DelayTagRequest delayReq = DelayTagRequest.builder()
                                .delayReasonCode(DelayReasonCode.HOUSEKEEPING_DELAY)
                                .note("EVS terminal cleaning underway")
                                .build();
                AdmissionRequest result = AdmissionRequest.builder()
                                .id(reqId)
                                .delayReasonTag("HOUSEKEEPING_DELAY")
                                .operationalDelayReason("EVS terminal cleaning underway")
                                .build();

                when(bmuService.attachDelayTag(eq(reqId), any(DelayTagRequest.class))).thenReturn(result);

                mockMvc.perform(post("/api/v1/bmu/requests/" + reqId + "/delay-tag")
                                .header("X-User-Role", "BMU_COORDINATOR")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(delayReq)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.delayReasonTag").value("HOUSEKEEPING_DELAY"));
        }

        @Test
        @DisplayName("POST /api/v1/bmu/requests/{id}/delay-tag returns 403 Forbidden when role is non-BMU")
        void testAttachDelayTag_Forbidden_WhenNonBmu() throws Exception {
                UUID reqId = UUID.randomUUID();
                DelayTagRequest delayReq = DelayTagRequest.builder()
                                .delayReasonCode(DelayReasonCode.BED_SHORTAGE)
                                .note("ED trying to tag delay")
                                .build();

                when(bmuService.attachDelayTag(eq(reqId), any(DelayTagRequest.class)))
                                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                                                "Only BMU coordinators possess operational authority to tag delay reasons."));

                mockMvc.perform(post("/api/v1/bmu/requests/" + reqId + "/delay-tag")
                                .header("X-User-Role", "ED_ATTENDING")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(delayReq)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.status").value(403))
                                .andExpect(jsonPath("$.title").value("Forbidden"));
        }
}
