package com.hospital.admissions.web;

import com.hospital.admissions.domain.Bed;
import com.hospital.admissions.domain.Patient;
import com.hospital.admissions.dto.PatientMilestoneResponse;
import com.hospital.admissions.repository.PatientRepository;
import com.hospital.admissions.service.PatientTrackerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @PostMapping("/beds/{bedId}/clean")
    public ResponseEntity<Bed> cleanBed(@PathVariable UUID bedId) {
        return ResponseEntity.ok(patientTrackerService.cleanBed(bedId));
    }
}
