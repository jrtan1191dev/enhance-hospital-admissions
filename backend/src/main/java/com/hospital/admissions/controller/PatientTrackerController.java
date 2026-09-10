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

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientTrackerController {

    private final PatientTrackerService patientTrackerService;
    private final PatientRepository patientRepository;

    @GetMapping("/track/{token}")
    public ResponseEntity<PatientMilestoneResponse> trackPatient(@PathVariable String token) {
        return ResponseEntity.ok(patientTrackerService.trackPatient(token));
    }

    @PostMapping("/track/{token}/actions")
    public ResponseEntity<PatientAuditInteraction> recordPatientAction(
            @PathVariable String token,
            @RequestBody PatientActionRequest request) {
        return ResponseEntity.ok(patientTrackerService.recordPatientAction(token, request.getActionType()));
    }

    @GetMapping("/tokens")
    public ResponseEntity<List<Patient>> getAvailablePatientsForPicker() {
        return ResponseEntity.ok(patientRepository.findAll());
    }

    @PostMapping("/beds/{bedId}/checkin")
    public ResponseEntity<Bed> checkinPatient(@PathVariable UUID bedId) {
        return ResponseEntity.ok(patientTrackerService.checkinPatient(bedId));
    }

    @PostMapping("/beds/{bedId}/vacate")
    public ResponseEntity<Bed> vacatePatient(@PathVariable UUID bedId) {
        return ResponseEntity.ok(patientTrackerService.vacatePatient(bedId));
    }

    @PostMapping("/simulate-periodic-update")
    public ResponseEntity<Map<String, Object>> simulatePeriodicUpdate() {
        int count = patientTrackerService.dispatchPeriodicUpdates();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "dispatchedCount", count,
                "message", "Simulated periodic status updates dispatched to " + count + " waiting patient(s)"
        ));
    }

    @PostMapping("/beds/{bedId}/clean")
    public ResponseEntity<Bed> cleanBed(@PathVariable UUID bedId) {
        return ResponseEntity.ok(patientTrackerService.cleanBed(bedId));
    }
}
