package com.hospital.admissions.controller;

import com.hospital.admissions.entity.Bed;
import com.hospital.admissions.entity.Patient;
import com.hospital.admissions.entity.PatientAuditInteraction;
import com.hospital.admissions.dto.PatientActionRequest;
import com.hospital.admissions.dto.PatientMilestoneResponse;
import com.hospital.admissions.repository.PatientRepository;
import com.hospital.admissions.service.PatientTrackerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for Patient and Family Journey Tracking operations.
 * <p>
 * Exposes endpoints for real-time queue milestone lookups, patient reassurance actions,
 * patient identity tokens, ward check-in, bedside vacating, and terminal cleaning triggers.
 */
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientTrackerController {

    private final PatientTrackerService patientTrackerService;
    private final PatientRepository patientRepository;

    /**
     * Retrieves current milestone, queue position, and estimated wait time for a patient via secure token.
     *
     * @param token the opaque alphanumeric tracking token issued to the patient.
     * @return {@link ResponseEntity} containing {@link PatientMilestoneResponse} journey details.
     */
    @GetMapping("/track/{token}")
    public ResponseEntity<PatientMilestoneResponse> trackPatient(@PathVariable String token) {
        return ResponseEntity.ok(patientTrackerService.trackPatient(token));
    }

    /**
     * Records a patient reassurance interaction or self-service status query for audit tracking.
     *
     * @param token   the patient's tracking token.
     * @param request the {@link PatientActionRequest} detailing the interaction type.
     * @return {@link ResponseEntity} containing the logged {@link PatientAuditInteraction}.
     */
    @PostMapping("/track/{token}/actions")
    public ResponseEntity<PatientAuditInteraction> recordPatientAction(
            @PathVariable String token,
            @RequestBody PatientActionRequest request) {
        return ResponseEntity.ok(patientTrackerService.recordPatientAction(token, request.getActionType()));
    }

    /**
     * Retrieves all registered patients for prototype persona selection and testing.
     *
     * @return {@link ResponseEntity} containing the list of all {@link Patient} records.
     */
    @GetMapping("/tokens")
    public ResponseEntity<List<Patient>> getAvailablePatientsForPicker() {
        return ResponseEntity.ok(patientRepository.findAll());
    }

    /**
     * Completes patient check-in into their assigned inpatient bed.
     *
     * @param bedId the unique identifier of the assigned {@link Bed}.
     * @return {@link ResponseEntity} containing the updated {@link Bed} with status OCCUPIED_TAKEN.
     */
    @PostMapping("/beds/{bedId}/checkin")
    public ResponseEntity<Bed> checkinPatient(@PathVariable UUID bedId) {
        return ResponseEntity.ok(patientTrackerService.checkinPatient(bedId));
    }

    /**
     * Marks a bed as vacated upon patient discharge, transitioning status to EMPTY_PENDING_CLEANING.
     *
     * @param bedId the unique identifier of the bed being vacated.
     * @return {@link ResponseEntity} containing the updated {@link Bed} entity.
     */
    @PostMapping("/beds/{bedId}/vacate")
    public ResponseEntity<Bed> vacatePatient(@PathVariable UUID bedId) {
        return ResponseEntity.ok(patientTrackerService.vacatePatient(bedId));
    }

    /**
     * Simulates background dispatch of periodic status updates to all waiting patients.
     *
     * @return {@link ResponseEntity} containing status message and count of dispatched notifications.
     */
    @PostMapping("/simulate-periodic-update")
    public ResponseEntity<Map<String, Object>> simulatePeriodicUpdate() {
        int count = patientTrackerService.dispatchPeriodicUpdates();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "dispatchedCount", count,
                "message", "Simulated periodic status updates dispatched to " + count + " waiting patient(s)"
        ));
    }

    /**
     * Marks a vacated bed as cleaned and ready for the next patient assignment.
     *
     * @param bedId the unique identifier of the cleaned bed.
     * @return {@link ResponseEntity} containing the updated {@link Bed} with status EMPTY_CLEANED.
     */
    @PostMapping("/beds/{bedId}/clean")
    public ResponseEntity<Bed> cleanBed(@PathVariable UUID bedId) {
        return ResponseEntity.ok(patientTrackerService.cleanBed(bedId));
    }
}
