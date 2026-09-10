package com.hospital.admissions.web;

import tools.jackson.databind.json.JsonMapper;
import com.hospital.admissions.domain.AdmissionRequest;
import com.hospital.admissions.domain.DischargeRunwayStage;
import com.hospital.admissions.domain.EddConfidence;
import com.hospital.admissions.domain.MedicationDeliveryStatus;
import com.hospital.admissions.dto.BmuCapacityForecastDto;
import com.hospital.admissions.dto.DischargeRunwayDto;
import com.hospital.admissions.dto.EddUpdateRequest;
import com.hospital.admissions.dto.TurnoverTaskDto;
import com.hospital.admissions.service.WardService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
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
class WardControllerTest {

    private MockMvc mockMvc;
    private JsonMapper objectMapper;

    @Mock
    private WardService wardService;

    @InjectMocks
    private WardController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = JsonMapper.builder().build();
    }

    @Test
    @DisplayName("POST /api/v1/ward/patients/{patientId}/edd sets EDD and returns AdmissionRequest")
    void testSetEdd() throws Exception {
        UUID patientId = UUID.randomUUID();
        LocalDate edd = LocalDate.now().plusDays(2);
        EddUpdateRequest req = EddUpdateRequest.builder()
                .edd(edd)
                .eddConfidence(EddConfidence.HIGH)
                .rationale("Stable recovery")
                .build();

        AdmissionRequest admissionRequest = AdmissionRequest.builder()
                .id(UUID.randomUUID())
                .edd(edd)
                .eddConfidence(EddConfidence.HIGH)
                .eddRationale("Stable recovery")
                .build();

        when(wardService.updateEdd(eq(patientId), any(EddUpdateRequest.class))).thenReturn(admissionRequest);

        mockMvc.perform(post("/api/v1/ward/patients/" + patientId + "/edd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.edd").value(edd.toString()))
                .andExpect(jsonPath("$.eddConfidence").value("HIGH"))
                .andExpect(jsonPath("$.eddRationale").value("Stable recovery"));
    }

    @Test
    @DisplayName("POST /api/v1/ward/patients/{patientId}/edd with invalid confidence returns 400")
    void testSetEdd_InvalidValidation() throws Exception {
        UUID patientId = UUID.randomUUID();
        String invalidJson = "{\"edd\": \"2026-09-12\", \"eddConfidence\": null}";

        mockMvc.perform(post("/api/v1/ward/patients/" + patientId + "/edd")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/ward/runway returns active runway list")
    void testGetRunway() throws Exception {
        UUID patientId = UUID.randomUUID();
        DischargeRunwayDto dto = DischargeRunwayDto.builder()
                .patientId(patientId)
                .patientName("Uncle Seng")
                .bedNumber("8A-01")
                .wardCode("Ward 8A")
                .runwayStage(DischargeRunwayStage.RUNWAY_D2)
                .confidence(EddConfidence.HIGH)
                .medicationStatus(MedicationDeliveryStatus.NOT_DISPATCHED)
                .build();

        when(wardService.getRunway()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/ward/runway"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientName").value("Uncle Seng"))
                .andExpect(jsonPath("$[0].bedNumber").value("8A-01"))
                .andExpect(jsonPath("$[0].runwayStage").value("RUNWAY_D2"));
    }

    @Test
    @DisplayName("GET /api/v1/ward/capacity-forecast returns BMU forecast projections")
    void testGetCapacityForecast() throws Exception {
        BmuCapacityForecastDto forecast = BmuCapacityForecastDto.builder()
                .totalNext24Hours(3)
                .totalNext48Hours(2)
                .totalNext72Hours(1)
                .build();

        when(wardService.getCapacityForecast()).thenReturn(forecast);

        mockMvc.perform(get("/api/v1/ward/capacity-forecast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNext24Hours").value(3))
                .andExpect(jsonPath("$.totalNext48Hours").value(2))
                .andExpect(jsonPath("$.totalNext72Hours").value(1));
    }
}
