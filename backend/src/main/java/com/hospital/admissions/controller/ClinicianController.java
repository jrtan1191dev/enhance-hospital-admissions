package com.hospital.admissions.controller;

import com.hospital.admissions.entity.AdmissionRequest;
import com.hospital.admissions.entity.AssessmentBroadcast;
import com.hospital.admissions.entity.Patient;
import com.hospital.admissions.entity.SpecialtyCluster;
import com.hospital.admissions.dto.EdAssessmentSubmitRequest;
import com.hospital.admissions.dto.SpecialistConsultRequest;
import com.hospital.admissions.service.ClinicianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Emergency Department (ED) and Inpatient Specialist clinical workflows.
 * <p>
 * Exposes endpoints for reviewing ED waiting patients, submitting clinical assessments,
 * managing peer specialist consult broadcasts, claiming consults, submitting clinical opinions,
 * and chaining cross-specialty consultations.
 */
@RestController
@RequestMapping("/api/v1/clinicians")
@RequiredArgsConstructor
public class ClinicianController {

    private final ClinicianService clinicianService;

    /**
     * Retrieves the list of patients currently waiting in the Emergency Department without an active admission request.
     *
     * @return {@link ResponseEntity} containing a list of waiting {@link Patient} entities.
     */
    @GetMapping("/ed/patients")
    public ResponseEntity<List<Patient>> getEdWaitingPatients() {
        return ResponseEntity.ok(clinicianService.getEdWaitingPatients());
    }

    /**
     * Retrieves all admission requests submitted by ED clinicians, ordered by submission timestamp descending.
     *
     * @return {@link ResponseEntity} containing a list of submitted {@link AdmissionRequest} instances.
     */
    @GetMapping("/ed/admissions")
    public ResponseEntity<List<AdmissionRequest>> getEdSubmittedAdmissions() {
        return ResponseEntity.ok(clinicianService.getEdSubmittedAdmissions());
    }

    /**
     * Submits an ED clinical intake assessment and generates an admission request or specialist broadcast.
     *
     * @param request the {@link EdAssessmentSubmitRequest} containing clinical observations, acuity tier, and consult needs.
     * @return {@link ResponseEntity} containing the created or updated {@link AdmissionRequest}.
     */
    @PostMapping("/ed/assessments/submit")
    public ResponseEntity<AdmissionRequest> submitEdAssessment(@Valid @RequestBody EdAssessmentSubmitRequest request) {
        return ResponseEntity.ok(clinicianService.submitEdAssessment(request));
    }

    /**
     * Retrieves active assessment broadcasts filtered optionally by medical specialty cluster.
     *
     * @param cluster optional {@link SpecialtyCluster} filter (e.g., CARDIOLOGY, GENERAL_MEDICINE).
     * @return {@link ResponseEntity} containing a list of matching {@link AssessmentBroadcast} objects.
     */
    @GetMapping("/specialist/broadcasts")
    public ResponseEntity<List<AssessmentBroadcast>> getSpecialistBroadcasts(
            @RequestParam(required = false) SpecialtyCluster cluster) {
        return ResponseEntity.ok(clinicianService.getBroadcasts(cluster));
    }

    /**
     * Claims an open specialist broadcast consultation for the currently authenticated clinician.
     *
     * @param id the unique identifier of the {@link AssessmentBroadcast}.
     * @return {@link ResponseEntity} containing the claimed {@link AssessmentBroadcast}.
     */
    @PostMapping("/specialist/broadcasts/{id}/claim")
    public ResponseEntity<AssessmentBroadcast> claimBroadcast(@PathVariable UUID id) {
        return ResponseEntity.ok(clinicianService.claimBroadcast(id));
    }

    /**
     * Submits a specialist consult opinion, recommending admission, diversion, or acuity tier updates.
     *
     * @param id      the unique identifier of the broadcast consultation.
     * @param request the {@link SpecialistConsultRequest} containing recommendations and clinical findings.
     * @return {@link ResponseEntity} containing the completed {@link AssessmentBroadcast}.
     */
    @PostMapping("/specialist/broadcasts/{id}/consult")
    public ResponseEntity<AssessmentBroadcast> submitConsult(
            @PathVariable UUID id,
            @Valid @RequestBody SpecialistConsultRequest request) {
        return ResponseEntity.ok(clinicianService.submitConsult(id, request));
    }

    /**
     * Amends a previously submitted specialist consult recommendation prior to patient bed placement.
     *
     * @param id      the unique identifier of the broadcast consultation.
     * @param request the updated {@link SpecialistConsultRequest}.
     * @return {@link ResponseEntity} containing the amended {@link AssessmentBroadcast}.
     */
    @PutMapping("/specialist/broadcasts/{id}/consult")
    public ResponseEntity<AssessmentBroadcast> amendConsult(
            @PathVariable UUID id,
            @Valid @RequestBody SpecialistConsultRequest request) {
        return ResponseEntity.ok(clinicianService.amendConsult(id, request));
    }

    /**
     * Chains a secondary specialist consult to an existing consultation broadcast for multi-disciplinary review.
     *
     * @param id      the unique identifier of the broadcast consultation.
     * @param request the {@link com.hospital.admissions.dto.ChainConsultRequest} specifying the secondary specialty cluster.
     * @return {@link ResponseEntity} containing the chained {@link AssessmentBroadcast}.
     */
    @PostMapping("/specialist/broadcasts/{id}/chain")
    public ResponseEntity<AssessmentBroadcast> chainConsult(
            @PathVariable UUID id,
            @Valid @RequestBody com.hospital.admissions.dto.ChainConsultRequest request) {
        return ResponseEntity.ok(clinicianService.chainConsult(id, request));
    }
}
