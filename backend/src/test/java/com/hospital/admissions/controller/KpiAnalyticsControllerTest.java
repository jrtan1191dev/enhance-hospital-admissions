package com.hospital.admissions.controller;

import tools.jackson.databind.json.JsonMapper;
import com.hospital.admissions.dto.HospitalKpiSummaryDto;
import com.hospital.admissions.service.KpiMetricsService;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class KpiAnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private KpiMetricsService kpiMetricsService;

    @InjectMocks
    private KpiAnalyticsController kpiAnalyticsController;

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(kpiAnalyticsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/analytics/kpis/summary returns 200 OK and expected Epic 1 metrics")
    void testGetKpiSummary_Success() throws Exception {
        HospitalKpiSummaryDto dto = HospitalKpiSummaryDto.builder()
                .periodStart("ALL_TIME")
                .periodEnd("ALL_TIME")
                .avgEdTurnaroundMinutes(14.5)
                .edTurnaroundP95Minutes(22.0)
                .specialistClaimLatencyAvgMinutes(6.2)
                .primarySpecialistConcordanceRatePct(85.5)
                .digitalBedRequestCount(42)
                .build();

        when(kpiMetricsService.getKpiSummary(any(), any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/analytics/kpis/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodStart").value("ALL_TIME"))
                .andExpect(jsonPath("$.avgEdTurnaroundMinutes").value(14.5))
                .andExpect(jsonPath("$.edTurnaroundP95Minutes").value(22.0))
                .andExpect(jsonPath("$.specialistClaimLatencyAvgMinutes").value(6.2))
                .andExpect(jsonPath("$.primarySpecialistConcordanceRatePct").value(85.5))
                .andExpect(jsonPath("$.digitalBedRequestCount").value(42));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/kpis/summary passes ISO-8601 query parameters correctly")
    void testGetKpiSummary_WithDateParams() throws Exception {
        HospitalKpiSummaryDto dto = HospitalKpiSummaryDto.builder()
                .periodStart("2026-09-01T00:00:00")
                .periodEnd("2026-09-10T23:59:59")
                .digitalBedRequestCount(15)
                .build();

        when(kpiMetricsService.getKpiSummary(any(), any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/analytics/kpis/summary")
                        .param("startDate", "2026-09-01T00:00:00")
                        .param("endDate", "2026-09-10T23:59:59")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodStart").value("2026-09-01T00:00:00"))
                .andExpect(jsonPath("$.digitalBedRequestCount").value(15));
    }
}
