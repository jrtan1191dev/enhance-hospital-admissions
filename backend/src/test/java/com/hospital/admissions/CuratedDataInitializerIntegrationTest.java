package com.hospital.admissions;

import com.hospital.admissions.dto.BatchSuggestion;
import com.hospital.admissions.dto.CohortSwapSuggestion;
import com.hospital.admissions.dto.DischargeRunwayDto;
import com.hospital.admissions.dto.TurnoverTaskDto;
import com.hospital.admissions.entity.DischargeRunwayStage;
import com.hospital.admissions.entity.MedicationDeliveryStatus;
import com.hospital.admissions.entity.TurnoverSlaStatus;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prototype")
class CuratedDataInitializerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Curated Data: Batch Holding Ward Suggestion generated for Ward 8B on startup")
    void testBatchHoldingWardSuggestionCurated() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/bmu/batch-suggestions")
                        .header("X-User-Role", "BMU_COORDINATOR"))
                .andExpect(status().isOk())
                .andReturn();

        List<BatchSuggestion> suggestions = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, BatchSuggestion.class)
        );

        assertThat(suggestions).isNotEmpty();
        BatchSuggestion ward8b = suggestions.stream()
                .filter(s -> "Ward 8B".equals(s.getTargetWardName()))
                .findFirst()
                .orElse(null);

        assertThat(ward8b).isNotNull();
        assertThat(ward8b.getPatientNames()).contains("Tan Ah Meng", "Mr Goh Beng Kiat", "Mr Teo Hock Seng");
        assertThat(ward8b.getUnlockedCapacityCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("Curated Data: Dynamic Cohort-Swap Suggestion generated for Ward 9B -> Ward 8A")
    void testDynamicCohortSwapSuggestionCurated() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/bmu/cohort-swap-suggestions")
                        .header("X-User-Role", "BMU_COORDINATOR"))
                .andExpect(status().isOk())
                .andReturn();

        List<CohortSwapSuggestion> suggestions = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, CohortSwapSuggestion.class)
        );

        assertThat(suggestions).isNotEmpty();
        CohortSwapSuggestion swap = suggestions.stream()
                .filter(s -> "Ward 9B".equals(s.getCurrentWardName()) && "Ward 8A".equals(s.getTargetWardName()))
                .findFirst()
                .orElse(null);

        assertThat(swap).isNotNull();
        assertThat(swap.getPatientName()).isEqualTo("Mr David Koh");
        assertThat(swap.getCurrentBedNumber()).isEqualTo("9B-01");
        assertThat(swap.getTargetBedNumber()).isEqualTo("8A-03");
        assertThat(swap.getUnlockedCapacityCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Curated Data: EVS Housekeeping Turnover Queue contains all 3 SLA status states")
    void testEvsHousekeepingTurnoverSlaTiersCurated() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/ward/turnover-tasks")
                        .header("X-User-Role", "HOUSEKEEPING"))
                .andExpect(status().isOk())
                .andReturn();

        List<TurnoverTaskDto> tasks = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, TurnoverTaskDto.class)
        );

        assertThat(tasks).isNotEmpty();
        boolean hasOnTrack = tasks.stream().anyMatch(t -> t.getSlaStatus() == TurnoverSlaStatus.ON_TRACK);
        boolean hasApproaching = tasks.stream().anyMatch(t -> t.getSlaStatus() == TurnoverSlaStatus.APPROACHING_SLA);
        boolean hasBreached = tasks.stream().anyMatch(t -> t.getSlaStatus() == TurnoverSlaStatus.BREACHED);

        assertThat(hasOnTrack).as("Should have ON_TRACK turnover task (8A-04)").isTrue();
        assertThat(hasApproaching).as("Should have APPROACHING_SLA turnover task (10A-03)").isTrue();
        assertThat(hasBreached).as("Should have BREACHED turnover task (9A-03)").isTrue();
    }

    @Test
    @DisplayName("Curated Data: Inpatient Discharge Runway displays full progression of stages")
    void testInpatientDischargeRunwayStagesCurated() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/ward/runway")
                        .header("X-User-Role", "WARD_NURSE"))
                .andExpect(status().isOk())
                .andReturn();

        List<DischargeRunwayDto> runway = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, DischargeRunwayDto.class)
        );

        assertThat(runway).isNotEmpty();
        boolean hasD2 = runway.stream().anyMatch(r -> r.getRunwayStage() == DischargeRunwayStage.RUNWAY_D2);
        boolean hasD3 = runway.stream().anyMatch(r -> r.getRunwayStage() == DischargeRunwayStage.RUNWAY_D3);
        boolean hasMorningSignoff = runway.stream().anyMatch(r -> r.getRunwayStage() == DischargeRunwayStage.READY_FOR_MORNING_SIGNOFF);
        boolean hasMedicationsPending = runway.stream().anyMatch(r -> r.getRunwayStage() == DischargeRunwayStage.MEDICATIONS_PENDING && r.getMedicationStatus() == MedicationDeliveryStatus.PACKING_IN_PROGRESS);
        boolean hasReadyToVacate = runway.stream().anyMatch(r -> r.getRunwayStage() == DischargeRunwayStage.READY_TO_VACATE && r.getMedicationStatus() == MedicationDeliveryStatus.DELIVERED_BEDSIDE);

        assertThat(hasD2).as("Should have RUNWAY_D2 (inp1)").isTrue();
        assertThat(hasD3).as("Should have RUNWAY_D3 (inp2)").isTrue();
        assertThat(hasMorningSignoff).as("Should have READY_FOR_MORNING_SIGNOFF (inp3)").isTrue();
        assertThat(hasMedicationsPending).as("Should have MEDICATIONS_PENDING with PACKING_IN_PROGRESS (inp4)").isTrue();
        assertThat(hasReadyToVacate).as("Should have READY_TO_VACATE with DELIVERED_BEDSIDE (inp5)").isTrue();
    }

    @Test
    @DisplayName("Curated Data: Public Patient Tracker demonstrates Sister Hospital referral and UV sanitization delay")
    void testPublicTrackerCuratedScenarios() throws Exception {
        // TOKEN-P116: Sister Hospital Referral
        mockMvc.perform(get("/api/v1/patients/track/TOKEN-P116"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientName").value("Mr Ahmad Ibrahim"))
                .andExpect(jsonPath("$.diversionRecommended").value(true))
                .andExpect(jsonPath("$.diversionPathway").value("COMMUNITY_HOSPITAL"));

        // TOKEN-P118: Specialized UV isolation delay tag
        mockMvc.perform(get("/api/v1/patients/track/TOKEN-P118"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientName").value("Mdm Wong Siew Kuan"))
                .andExpect(jsonPath("$.delayReason").isNotEmpty())
                .andExpect(jsonPath("$.estimatedWaitMinutes").isNumber());
    }
}
