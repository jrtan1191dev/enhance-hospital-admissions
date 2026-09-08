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
        return patientRepository.findAll();
    }

    @Transactional
    public AdmissionRequest submitEdAssessment(EdAssessmentSubmitRequest req) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        Patient patient = patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + req.getPatientId()));

        patient.setNeedsTelemetry(req.isNeedsTelemetry());
        patientRepository.save(patient);

        AdmissionRequest admissionRequest = AdmissionRequest.builder()
                .patient(patient)
                .suspectedDiagnosisService(req.getSuspectedDiagnosisService())
                .primaryAcuityTier(req.getPrimaryAcuityTier())
                .requestedWardClass(req.getRequestedWardClass())
                .status(AdmissionStatus.BED_REQUESTED)
                .requestedAt(LocalDateTime.now())
                .build();

        admissionRequest = admissionRequestRepository.save(admissionRequest);

        AssessmentBroadcast broadcast = AssessmentBroadcast.builder()
                .admissionRequest(admissionRequest)
                .targetCluster(req.getSuspectedDiagnosisService())
                .status(BroadcastStatus.OPEN)
                .build();

        broadcastRepository.save(broadcast);

        auditLogger.logAction(currentUser, "SUBMIT_ED_ASSESSMENT",
                "AdmissionRequest:" + admissionRequest.getId(),
                "PrimaryAcuity=" + req.getPrimaryAcuityTier() + ", WardClass=" + req.getRequestedWardClass() + ", Cluster=" + req.getSuspectedDiagnosisService());

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

        auditLogger.logAction(currentUser, "CLAIM_BROADCAST",
                "AssessmentBroadcast:" + broadcastId,
                "Specialist=" + currentUser);

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
        admissionRequestRepository.save(request);

        broadcast = broadcastRepository.save(broadcast);

        auditLogger.logAction(currentUser, "SUBMIT_SPECIALIST_CONSULT",
                "AssessmentBroadcast:" + broadcastId,
                "SecondaryAcuity=" + req.getSecondaryAcuityTier() + ", Diversion=" + req.isDiversionRecommended());

        return broadcast;
    }
}
