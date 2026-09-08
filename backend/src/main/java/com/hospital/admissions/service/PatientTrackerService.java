package com.hospital.admissions.service;

import com.hospital.admissions.domain.*;
import com.hospital.admissions.dto.PatientMilestoneResponse;
import com.hospital.admissions.repository.AdmissionRequestRepository;
import com.hospital.admissions.repository.BedRepository;
import com.hospital.admissions.repository.PatientRepository;
import com.hospital.admissions.security.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientTrackerService {

    private final PatientRepository patientRepository;
    private final AdmissionRequestRepository admissionRequestRepository;
    private final BedRepository bedRepository;
    private final AuditLogger auditLogger;

    public PatientMilestoneResponse trackPatient(String queueToken) {
        Patient patient = patientRepository.findByQueueToken(queueToken)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found for token: " + queueToken));

        Optional<AdmissionRequest> optRequest = admissionRequestRepository.findByPatient_QueueToken(queueToken);

        AdmissionStatus status = optRequest.map(AdmissionRequest::getStatus).orElse(AdmissionStatus.ASSESSMENT_PENDING);
        int queuePosition = 1;
        int estimatedWaitMinutes = 15;

        if (status == AdmissionStatus.BED_REQUESTED) {
            List<AdmissionRequest> pendingRequests = admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED).stream()
                    .sorted(Comparator.comparing((AdmissionRequest r) -> r.getPrimaryAcuityTier().ordinal())
                            .thenComparing(AdmissionRequest::getRequestedAt))
                    .collect(Collectors.toList());

            UUID currentReqId = optRequest.get().getId();
            for (int i = 0; i < pendingRequests.size(); i++) {
                if (pendingRequests.get(i).getId().equals(currentReqId)) {
                    queuePosition = i + 1;
                    break;
                }
            }
            estimatedWaitMinutes = queuePosition * 25;
        } else if (status == AdmissionStatus.BED_ALLOCATED) {
            queuePosition = 0;
            estimatedWaitMinutes = 5;
        } else if (status == AdmissionStatus.ADMITTED_INPATIENT || status == AdmissionStatus.DISCHARGED) {
            queuePosition = 0;
            estimatedWaitMinutes = 0;
        }

        String assignedBedNumber = null;
        String assignedWardName = null;
        Integer assignedLevel = null;

        if (optRequest.isPresent() && optRequest.get().getAssignedBed() != null) {
            Bed bed = optRequest.get().getAssignedBed();
            assignedBedNumber = bed.getBedNumber();
            assignedWardName = bed.getWard().getName();
            assignedLevel = bed.getWard().getLevel();
        }

        String coPay = "Estimated Co-Pay: $35 - $60 / day (MediShield Life & Subsidized Class "
                + (optRequest.map(r -> r.getRequestedWardClass().name()).orElse("B2")) + " applied)";
        String guidance = "Please remain seated in the ED observation area. Our portering team will escort you once your bed is prepared.";

        if (optRequest.isPresent()) {
            AdmissionRequest req = optRequest.get();
            if (req.getFirstTrackerAccessedAt() == null) {
                req.setFirstTrackerAccessedAt(LocalDateTime.now());
                admissionRequestRepository.save(req);

                java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
                details.put("PatientId", patient.getId());
                details.put("AdmissionStatus", status);
                details.put("MilestoneStep", status == AdmissionStatus.BED_REQUESTED ? "QUEUE_WAITING" : status.name());
                details.put("QueuePos", queuePosition);
                details.put("EstWaitMins", estimatedWaitMinutes);

                auditLogger.logAction("PUBLIC_TOKEN", "TRACK_PATIENT_ACCESS",
                        "PatientToken:" + queueToken,
                        AuditLogger.formatDetails(details));
            }
        }

        return PatientMilestoneResponse.builder()
                .patientId(patient.getId())
                .patientName(patient.getName())
                .queueToken(queueToken)
                .admissionStatus(status)
                .queuePosition(queuePosition)
                .estimatedWaitMinutes(estimatedWaitMinutes)
                .assignedBedNumber(assignedBedNumber)
                .assignedWardName(assignedWardName)
                .assignedLevel(assignedLevel)
                .coPayEstimate(coPay)
                .careGuidance(guidance)
                .build();
    }

    @Transactional
    public Bed checkinPatient(UUID bedId) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new IllegalArgumentException("Bed not found: " + bedId));

        bed.setStatus(BedStatus.OCCUPIED_TAKEN);
        Bed savedBed = bedRepository.save(bed);

        if (bed.getCurrentPatient() != null) {
            admissionRequestRepository.findByPatient_Id(bed.getCurrentPatient().getId()).ifPresent(req -> {
                req.setStatus(AdmissionStatus.ADMITTED_INPATIENT);
                req.setAdmittedAt(LocalDateTime.now());
                admissionRequestRepository.save(req);
            });
        }

        auditLogger.logAction(currentUser, "CHECKIN_PATIENT",
                "Bed:" + bedId,
                "BedNumber=" + bed.getBedNumber() + " transitioned to OCCUPIED_TAKEN");

        return savedBed;
    }

    @Transactional
    public Bed vacatePatient(UUID bedId) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new IllegalArgumentException("Bed not found: " + bedId));

        LocalDateTime now = LocalDateTime.now();
        int vacateHour = now.getHour();
        boolean dischargedBeforeNoon = vacateHour < 12;

        bed.setStatus(BedStatus.EMPTY_PENDING_CLEANING);
        bed.setCleaningStartedAt(now);
        Bed savedBed = bedRepository.save(bed);

        if (bed.getCurrentPatient() != null) {
            admissionRequestRepository.findByPatient_Id(bed.getCurrentPatient().getId()).ifPresent(req -> {
                req.setStatus(AdmissionStatus.DISCHARGED);
                req.setDischargedAt(now);
                admissionRequestRepository.save(req);
            });
        }

        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("BedNumber", bed.getBedNumber());
        details.put("VacateTimestamp", now);
        details.put("VacateHour", vacateHour);
        details.put("DischargedBeforeNoon", dischargedBeforeNoon);

        auditLogger.logAction(currentUser, "VACATE_PATIENT",
                "Bed:" + bedId,
                AuditLogger.formatDetails(details));

        return savedBed;
    }

    @Transactional
    public Bed cleanBed(UUID bedId) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new IllegalArgumentException("Bed not found: " + bedId));

        LocalDateTime now = LocalDateTime.now();
        long elapsedCleaningMins = (bed.getCleaningStartedAt() != null) ?
                Math.max(1, java.time.Duration.between(bed.getCleaningStartedAt(), now).toMinutes()) : 24L;
        boolean within30mSla = elapsedCleaningMins <= 30;

        bed.setStatus(BedStatus.EMPTY_CLEANED);
        bed.setLastCleanedAt(now);
        bed.setCurrentPatient(null);
        Bed savedBed = bedRepository.save(bed);

        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("BedNumber", bed.getBedNumber());
        details.put("HousekeeperId", currentUser);
        details.put("ElapsedCleaningMins", elapsedCleaningMins);
        details.put("Within30mSla", within30mSla);

        auditLogger.logAction(currentUser, "CLEAN_BED",
                "Bed:" + bedId,
                AuditLogger.formatDetails(details));

        return savedBed;
    }
}
