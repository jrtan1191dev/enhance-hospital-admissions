package com.hospital.admissions.service;

import com.hospital.admissions.domain.*;
import com.hospital.admissions.dto.EdAssessmentSubmitRequest;
import com.hospital.admissions.dto.SpecialistConsultRequest;
import com.hospital.admissions.repository.AdmissionRequestRepository;
import com.hospital.admissions.repository.AssessmentBroadcastRepository;
import com.hospital.admissions.repository.PatientRepository;
import com.hospital.admissions.security.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicianService {

    private final PatientRepository patientRepository;
    private final AdmissionRequestRepository admissionRequestRepository;
    private final AssessmentBroadcastRepository broadcastRepository;
    private final AuditLogger auditLogger;

    public List<Patient> getEdWaitingPatients() {
        return patientRepository.findPatientsWithoutActiveAdmission();
    }

    public List<AdmissionRequest> getEdSubmittedAdmissions() {
        return admissionRequestRepository.findAllByOrderByRequestedAtDesc();
    }

    @Transactional
    public AdmissionRequest submitEdAssessment(EdAssessmentSubmitRequest req) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        Patient patient = patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + req.getPatientId()));

        boolean primaryTelemetry = req.isPrimaryTelemetry();
        patient.setNeedsTelemetry(primaryTelemetry);
        patientRepository.save(patient);

        boolean requiresConsult = req.isRequiresSpecialistConsult();
        AdmissionStatus initialStatus = requiresConsult ? AdmissionStatus.ASSESSMENT_PENDING : AdmissionStatus.BED_REQUESTED;

        AdmissionRequest admissionRequest = AdmissionRequest.builder()
                .patient(patient)
                .suspectedDiagnosisService(req.getSuspectedDiagnosisService())
                .primaryAcuityTier(req.getPrimaryAcuityTier())
                .effectiveAcuityTier(req.getPrimaryAcuityTier())
                .primaryTelemetry(primaryTelemetry)
                .effectiveTelemetry(primaryTelemetry)
                .requestedWardClass(req.getRequestedWardClass())
                .status(initialStatus)
                .requiresSpecialistConsult(requiresConsult)
                .requestedAt(LocalDateTime.now())
                .build();

        boolean hasOverrides = req.getOverrides() != null && !req.getOverrides().isEmpty();
        boolean recommendedAccepted = !hasOverrides && (req.getRecommendedAccepted() != null ? req.getRecommendedAccepted() : true);
        double elapsedMins = req.getElapsedMins() != null ? req.getElapsedMins() :
                (patient.getCreatedAt() != null ? Math.max(1.0, java.time.Duration.between(patient.getCreatedAt(), LocalDateTime.now()).toMinutes()) : 12.5);

        admissionRequest.setIsRecommendationAccepted(recommendedAccepted);
        admissionRequest = admissionRequestRepository.save(admissionRequest);

        if (requiresConsult) {
            AssessmentBroadcast broadcast = AssessmentBroadcast.builder()
                    .admissionRequest(admissionRequest)
                    .targetCluster(req.getSuspectedDiagnosisService())
                    .status(BroadcastStatus.OPEN)
                    .build();
            broadcastRepository.save(broadcast);
        }

        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("PrimaryAcuity", req.getPrimaryAcuityTier());
        details.put("EffectiveAcuity", req.getPrimaryAcuityTier());
        details.put("WardClass", req.getRequestedWardClass());
        details.put("Cluster", req.getSuspectedDiagnosisService());
        details.put("RequiresConsult", requiresConsult);
        details.put("RecommendedAccepted", recommendedAccepted);
        details.put("ElapsedMins", String.format(java.util.Locale.US, "%.1f", elapsedMins));

        auditLogger.logAction(currentUser, "SUBMIT_ED_ASSESSMENT",
                "AdmissionRequest:" + admissionRequest.getId(),
                AuditLogger.formatDetails(details));

        if (hasOverrides) {
            for (com.hospital.admissions.dto.ClinicalBaselineOverride ov : req.getOverrides()) {
                java.util.Map<String, Object> ovDetails = new java.util.LinkedHashMap<>();
                ovDetails.put("Field", ov.getField());
                ovDetails.put("Original", ov.getOriginalValue());
                ovDetails.put("Submitted", ov.getSubmittedValue());
                if (ov.getOverrideReason() != null) {
                    ovDetails.put("Reason", ov.getOverrideReason());
                }
                auditLogger.logAction(currentUser, "OVERRIDE_CLINICAL_BASELINE",
                        "AdmissionRequest:" + admissionRequest.getId(),
                        AuditLogger.formatDetails(ovDetails));
            }
        }

        return admissionRequest;
    }

    public List<AssessmentBroadcast> getBroadcasts(SpecialtyCluster cluster) {
        if (cluster != null) {
            return broadcastRepository.findByTargetCluster(cluster);
        }
        return broadcastRepository.findAll();
    }

    @Transactional
    public AssessmentBroadcast claimBroadcast(UUID broadcastId) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        AssessmentBroadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("Broadcast not found: " + broadcastId));

        broadcast.setStatus(BroadcastStatus.CLAIMED);
        broadcast.setClaimedBySpecialistId(currentUser);
        broadcast.setClaimedAt(LocalDateTime.now());

        broadcast = broadcastRepository.save(broadcast);

        double elapsedClaimMins = broadcast.getCreatedAt() != null ?
                Math.max(0.5, java.time.Duration.between(broadcast.getCreatedAt(), LocalDateTime.now()).toMinutes()) : 4.5;

        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("TargetCluster", broadcast.getTargetCluster());
        details.put("Specialist", currentUser);
        details.put("ElapsedClaimMins", String.format(java.util.Locale.US, "%.1f", elapsedClaimMins));

        auditLogger.logAction(currentUser, "CLAIM_BROADCAST",
                "AssessmentBroadcast:" + broadcastId,
                AuditLogger.formatDetails(details));

        return broadcast;
    }

    @Transactional
    public AssessmentBroadcast submitConsult(UUID broadcastId, SpecialistConsultRequest req) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        AssessmentBroadcast broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("Broadcast not found: " + broadcastId));

        broadcast.setStatus(BroadcastStatus.COMPLETED);
        broadcast.setConsultNotes(req.getConsultNotes());

        AdmissionRequest request = broadcast.getAdmissionRequest();
        request.setSecondaryAcuityTier(req.getSecondaryAcuityTier());
        request.setDiversionRecommended(req.isDiversionRecommended());
        boolean isConcordant = request.getPrimaryAcuityTier() == req.getSecondaryAcuityTier();
        request.setIsDiscordant(!isConcordant);
        admissionRequestRepository.save(request);

        broadcast = broadcastRepository.save(broadcast);

        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("PrimaryAcuity", request.getPrimaryAcuityTier());
        details.put("SecondaryAcuity", req.getSecondaryAcuityTier());
        details.put("Concordant", isConcordant);
        details.put("DiversionEndorsed", req.isDiversionRecommended());

        auditLogger.logAction(currentUser, "SUBMIT_SPECIALIST_CONSULT",
                "AssessmentBroadcast:" + broadcastId,
                AuditLogger.formatDetails(details));

        return broadcast;
    }
}
