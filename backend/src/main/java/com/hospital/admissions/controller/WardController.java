package com.hospital.admissions.controller;

import com.hospital.admissions.entity.AdmissionRequest;
import com.hospital.admissions.dto.BmuCapacityForecastDto;
import com.hospital.admissions.dto.DischargeRunwayDto;
import com.hospital.admissions.dto.EddUpdateRequest;
import com.hospital.admissions.dto.TurnoverTaskDto;
import com.hospital.admissions.service.WardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for inpatient ward operations, discharge runways,
 * morning authorizations, bedside medication delivery, and turnover tasks.
 */
@RestController
@RequestMapping("/api/v1/ward")
@RequiredArgsConstructor
public class WardController {

    private final WardService wardService;

    /**
     * Sets or updates Estimated Date of Discharge (EDD) for an admitted inpatient.
     */
    @PostMapping("/patients/{patientId}/edd")
    public ResponseEntity<AdmissionRequest> updateEdd(
            @PathVariable UUID patientId,
            @Valid @RequestBody EddUpdateRequest request) {
        return ResponseEntity.ok(wardService.updateEdd(patientId, request));
    }

    /**
     * Retrieves active discharge runways across all wards with dynamic stage calculations.
     */
    @GetMapping("/runway")
    public ResponseEntity<List<DischargeRunwayDto>> getRunway() {
        return ResponseEntity.ok(wardService.getRunway());
    }

    /**
     * Retrieves 24 to 72-hour aggregate discharge capacity forecasts.
     */
    @GetMapping("/capacity-forecast")
    public ResponseEntity<BmuCapacityForecastDto> getCapacityForecast() {
        return ResponseEntity.ok(wardService.getCapacityForecast());
    }

    /**
     * Clinician 1-click morning discharge sign-off action.
     */
    @PostMapping("/patients/{patientId}/discharge-signoff")
    public ResponseEntity<AdmissionRequest> dischargeSignoff(@PathVariable UUID patientId) {
        return ResponseEntity.ok(wardService.dischargeSignoff(patientId));
    }

    /**
     * Bedside medication delivery confirmation action.
     */
    @PostMapping("/patients/{patientId}/deliver-medication")
    public ResponseEntity<AdmissionRequest> deliverMedication(@PathVariable UUID patientId) {
        return ResponseEntity.ok(wardService.deliverMedication(patientId));
    }

    /**
     * Retrieves active vacated beds under 30-minute terminal sanitization SLA.
     */
    @GetMapping("/turnover-tasks")
    public ResponseEntity<List<TurnoverTaskDto>> getTurnoverTasks() {
        return ResponseEntity.ok(wardService.getTurnoverTasks());
    }
}
