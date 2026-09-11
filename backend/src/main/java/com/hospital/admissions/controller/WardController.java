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
     *
     * @param patientId the unique identifier of the admitted patient.
     * @param request   the {@link EddUpdateRequest} containing target date and confidence level.
     * @return {@link ResponseEntity} containing the updated {@link AdmissionRequest}.
     */
    @PostMapping("/patients/{patientId}/edd")
    public ResponseEntity<AdmissionRequest> updateEdd(
            @PathVariable UUID patientId,
            @Valid @RequestBody EddUpdateRequest request) {
        return ResponseEntity.ok(wardService.updateEdd(patientId, request));
    }

    /**
     * Retrieves active discharge runways across all wards with dynamic stage calculations.
     *
     * @return {@link ResponseEntity} containing a list of {@link DischargeRunwayDto} milestones.
     */
    @GetMapping("/runway")
    public ResponseEntity<List<DischargeRunwayDto>> getRunway() {
        return ResponseEntity.ok(wardService.getRunway());
    }

    /**
     * Retrieves 24 to 72-hour aggregate discharge capacity forecasts.
     *
     * @return {@link ResponseEntity} containing {@link BmuCapacityForecastDto} forecast metrics.
     */
    @GetMapping("/capacity-forecast")
    public ResponseEntity<BmuCapacityForecastDto> getCapacityForecast() {
        return ResponseEntity.ok(wardService.getCapacityForecast());
    }

    /**
     * Clinician 1-click morning discharge sign-off action.
     *
     * @param patientId the unique identifier of the patient to authorize for discharge.
     * @return {@link ResponseEntity} containing the updated {@link AdmissionRequest}.
     */
    @PostMapping("/patients/{patientId}/discharge-signoff")
    public ResponseEntity<AdmissionRequest> dischargeSignoff(@PathVariable UUID patientId) {
        return ResponseEntity.ok(wardService.dischargeSignoff(patientId));
    }

    /**
     * Bedside medication delivery confirmation action.
     *
     * @param patientId the unique identifier of the patient receiving discharge medications.
     * @return {@link ResponseEntity} containing the updated {@link AdmissionRequest}.
     */
    @PostMapping("/patients/{patientId}/deliver-medication")
    public ResponseEntity<AdmissionRequest> deliverMedication(@PathVariable UUID patientId) {
        return ResponseEntity.ok(wardService.deliverMedication(patientId));
    }

    /**
     * Retrieves active vacated beds under 30-minute terminal sanitization SLA.
     *
     * @return {@link ResponseEntity} containing a list of {@link TurnoverTaskDto} sanitization tasks.
     */
    @GetMapping("/turnover-tasks")
    public ResponseEntity<List<TurnoverTaskDto>> getTurnoverTasks() {
        return ResponseEntity.ok(wardService.getTurnoverTasks());
    }
}
