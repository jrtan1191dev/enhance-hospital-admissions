package com.hospital.admissions.controller;

import com.hospital.admissions.dto.HospitalKpiSummaryDto;
import com.hospital.admissions.service.KpiMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST API controller exposing hospital-wide operational Key Performance Indicators (KPIs).
 */
@RestController
@RequestMapping("/api/v1/analytics")
@CrossOrigin
@RequiredArgsConstructor
public class KpiAnalyticsController {

    private final KpiMetricsService kpiMetricsService;

    /**
     * Retrieve executive summary of operational KPIs.
     *
     * @param startDate optional ISO-8601 start boundary
     * @param endDate   optional ISO-8601 end boundary
     * @return 200 OK with HospitalKpiSummaryDto
     */
    @GetMapping("/kpis/summary")
    public ResponseEntity<HospitalKpiSummaryDto> getKpiSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        HospitalKpiSummaryDto summary = kpiMetricsService.getKpiSummary(startDate, endDate);
        return ResponseEntity.ok(summary);
    }
}
