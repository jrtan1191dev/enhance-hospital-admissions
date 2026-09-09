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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicianService {

    public static final Map<SpecialtyCluster, String> DEFAULT_SPECIALISTS = Map.of(
            SpecialtyCluster.CARDIOLOGY, "dr_lim_cardio",
            SpecialtyCluster.GENERAL_MEDICINE, "dr_tan_genmed",
            SpecialtyCluster.SURGERY, "dr_kumar_surg",
            SpecialtyCluster.ORTHOPAEDICS, "dr_lee_ortho"
    );

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
            java.util.Set<SpecialtyCluster> clusters = req.getTargetClusters();
            if (clusters == null || clusters.isEmpty()) {
                clusters = java.util.Collections.singleton(req.getSuspectedDiagnosisService());
            }
            for (SpecialtyCluster cluster : clusters) {
                AssessmentBroadcast broadcast = AssessmentBroadcast.builder()
                        .admissionRequest(admissionRequest)
                        .targetCluster(cluster)
                        .status(BroadcastStatus.OPEN)
                        .build();
                broadcastRepository.save(broadcast);
            }
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

        if (broadcast.getStatus() != BroadcastStatus.OPEN) {
            throw new IllegalStateException("Broadcast has already been claimed or is no longer open: " + broadcastId);
        }

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
        broadcast.setSecondaryAcuityTier(req.getSecondaryAcuityTier());
        broadcast.setSecondaryTelemetry(req.getSecondaryTelemetry());
        broadcast.setDiversionPathway(req.getDiversionPathway());

        AdmissionRequest request = broadcast.getAdmissionRequest();
        request.setSecondaryAcuityTier(req.getSecondaryAcuityTier());
        if (req.getSecondaryTelemetry() != null) {
            request.setSecondaryTelemetry(req.getSecondaryTelemetry());
        }
        boolean isDiversion = req.isDiversionRecommended() ||
                (req.getDiversionPathway() != null && req.getDiversionPathway() != DiversionPathway.NONE);
        request.setDiversionRecommended(isDiversion);
        if (req.getDiversionPathway() != null) {
            request.setDiversionPathway(req.getDiversionPathway());
        }
        boolean isConcordant = request.getPrimaryAcuityTier() == req.getSecondaryAcuityTier();
        request.setIsDiscordant(!isConcordant);
        admissionRequestRepository.save(request);

        broadcast = broadcastRepository.save(broadcast);

        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("PrimaryAcuity", request.getPrimaryAcuityTier());
        details.put("SecondaryAcuity", req.getSecondaryAcuityTier());
        details.put("Concordant", isConcordant);
        details.put("DiversionEndorsed", isDiversion);
        if (req.getDiversionPathway() != null) {
            details.put("DiversionPathway", req.getDiversionPathway());
        }

        auditLogger.logAction(currentUser, "SUBMIT_SPECIALIST_CONSULT",
                "AssessmentBroadcast:" + broadcastId,
                AuditLogger.formatDetails(details));

        return broadcast;
    }

    @Transactional
    public AssessmentBroadcast chainConsult(UUID broadcastId, com.hospital.admissions.dto.ChainConsultRequest req) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        AssessmentBroadcast parentBroadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("Broadcast not found: " + broadcastId));

        if (parentBroadcast.getStatus() != BroadcastStatus.CLAIMED &&
                parentBroadcast.getStatus() != BroadcastStatus.AUTO_ESCALATED &&
                parentBroadcast.getStatus() != BroadcastStatus.COMPLETED) {
            throw new IllegalStateException("Cannot chain consult: Parent broadcast must be claimed or completed first (current status: " + parentBroadcast.getStatus() + ")");
        }

        AssessmentBroadcast chainedBroadcast = AssessmentBroadcast.builder()
                .admissionRequest(parentBroadcast.getAdmissionRequest())
                .targetCluster(req.getTargetCluster())
                .status(BroadcastStatus.OPEN)
                .parentBroadcastId(broadcastId)
                .consultNotes(req.getRationale())
                .build();

        chainedBroadcast = broadcastRepository.save(chainedBroadcast);

        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("ParentBroadcastId", broadcastId);
        details.put("TargetCluster", req.getTargetCluster());
        details.put("Specialist", currentUser);
        if (req.getRationale() != null) {
            details.put("Rationale", req.getRationale());
        }

        auditLogger.logAction(currentUser, "CHAIN_CONSULT",
                "AssessmentBroadcast:" + chainedBroadcast.getId(),
                AuditLogger.formatDetails(details));

        return chainedBroadcast;
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public int autoEscalateOverdueBroadcasts() {
        return autoEscalateOverdueBroadcasts(LocalDateTime.now());
    }

    @Transactional
    public int autoEscalateOverdueBroadcasts(LocalDateTime now) {
        List<AssessmentBroadcast> openBroadcasts = broadcastRepository.findByStatus(BroadcastStatus.OPEN);
        int count = 0;
        for (AssessmentBroadcast b : openBroadcasts) {
            LocalDateTime creationTime = b.getCreatedAt();
            if (creationTime == null && b.getAdmissionRequest() != null) {
                creationTime = b.getAdmissionRequest().getRequestedAt();
            }
            if (creationTime == null) {
                creationTime = now;
            }

            AcuityTier tier = b.getAdmissionRequest() != null ? b.getAdmissionRequest().getPrimaryAcuityTier() : null;
            long slaMins = (tier == AcuityTier.TIER_1_CRITICAL || tier == AcuityTier.TIER_2_ACUTE_URGENT) ? 15 : 30;

            if (java.time.Duration.between(creationTime, now).toMinutes() >= slaMins) {
                String defaultLead = DEFAULT_SPECIALISTS.getOrDefault(b.getTargetCluster(), "oncall_specialist");
                b.setStatus(BroadcastStatus.AUTO_ESCALATED);
                b.setClaimedBySpecialistId(defaultLead);
                b.setClaimedAt(now);
                broadcastRepository.save(b);
                count++;

                java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
                details.put("TargetCluster", b.getTargetCluster());
                details.put("EscalatedTo", defaultLead);
                details.put("AcuityTier", tier);
                details.put("ElapsedMins", java.time.Duration.between(creationTime, now).toMinutes());

                auditLogger.logAction("SYSTEM", "AUTO_ESCALATE_BROADCAST",
                        "AssessmentBroadcast:" + b.getId(),
                        AuditLogger.formatDetails(details));
            }
        }
        return count;
    }
}
