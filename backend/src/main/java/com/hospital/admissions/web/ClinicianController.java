package com.hospital.admissions.web;

import com.hospital.admissions.domain.AdmissionRequest;
import com.hospital.admissions.domain.AssessmentBroadcast;
import com.hospital.admissions.domain.Patient;
import com.hospital.admissions.domain.SpecialtyCluster;
import com.hospital.admissions.dto.EdAssessmentSubmitRequest;
import com.hospital.admissions.dto.SpecialistConsultRequest;
import com.hospital.admissions.service.ClinicianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinicians")
@RequiredArgsConstructor
public class ClinicianController {

    private final ClinicianService clinicianService;

    @GetMapping("/ed/patients")
    public ResponseEntity<List<Patient>> getEdWaitingPatients() {
        return ResponseEntity.ok(clinicianService.getEdWaitingPatients());
    }

    @GetMapping("/ed/admissions")
    public ResponseEntity<List<AdmissionRequest>> getEdSubmittedAdmissions() {
        return ResponseEntity.ok(clinicianService.getEdSubmittedAdmissions());
    }

    @PostMapping("/ed/assessments/submit")
    public ResponseEntity<AdmissionRequest> submitEdAssessment(@Valid @RequestBody EdAssessmentSubmitRequest request) {
        return ResponseEntity.ok(clinicianService.submitEdAssessment(request));
    }

    @GetMapping("/specialist/broadcasts")
    public ResponseEntity<List<AssessmentBroadcast>> getSpecialistBroadcasts(
            @RequestParam(required = false) SpecialtyCluster cluster) {
        return ResponseEntity.ok(clinicianService.getBroadcasts(cluster));
    }

    @PostMapping("/specialist/broadcasts/{id}/claim")
    public ResponseEntity<AssessmentBroadcast> claimBroadcast(@PathVariable UUID id) {
        return ResponseEntity.ok(clinicianService.claimBroadcast(id));
    }

    @PostMapping("/specialist/broadcasts/{id}/consult")
    public ResponseEntity<AssessmentBroadcast> submitConsult(
            @PathVariable UUID id,
            @Valid @RequestBody SpecialistConsultRequest request) {
        return ResponseEntity.ok(clinicianService.submitConsult(id, request));
    }
}
