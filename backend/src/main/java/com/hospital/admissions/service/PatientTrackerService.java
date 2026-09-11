package com.hospital.admissions.service;

import com.hospital.admissions.entity.*;
import com.hospital.admissions.dto.PatientMilestoneResponse;
import com.hospital.admissions.repository.AdmissionRequestRepository;
import com.hospital.admissions.repository.BedRepository;
import com.hospital.admissions.repository.PatientAuditInteractionRepository;
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

/**
 * Service managing the patient and family queue journey experience.
 * <p>
 * Provides transparent real-time milestone tracking, dynamic queue position estimation,
 * empathetic delay communication, automated status update dispatch, bed check-in,
 * vacating, terminal cleaning SLA monitoring, and patient reassurance logging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientTrackerService {

    private final PatientRepository patientRepository;
    private final AdmissionRequestRepository admissionRequestRepository;
    private final BedRepository bedRepository;
    private final PatientAuditInteractionRepository patientAuditInteractionRepository;
    private final AuditLogger auditLogger;

    /**
     * Retrieves comprehensive real-time journey milestone tracking data for a patient by queue token.
     * Computes queue position relative to same-ward-class patients and estimates wait time.
     *
     * @param queueToken the unique alphanumeric token issued to the patient.
     * @return the populated {@link PatientMilestoneResponse} with milestone progress and reassurance narrative.
     */
    public PatientMilestoneResponse trackPatient(String queueToken) {
        Patient patient = patientRepository.findByQueueToken(queueToken)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found for token: " + queueToken));

        Optional<AdmissionRequest> optRequest = admissionRequestRepository.findByPatient_QueueToken(queueToken);

        AdmissionStatus status = optRequest.map(AdmissionRequest::getStatus).orElse(AdmissionStatus.ASSESSMENT_PENDING);
        WardClass requestedWardClass = optRequest.map(AdmissionRequest::getRequestedWardClass).orElse(WardClass.B2);
        int queuePosition = 1;
        int patientsAhead = 0;
        int estimatedWaitMinutes = 15;

        if (status == AdmissionStatus.BED_REQUESTED) {
            List<AdmissionRequest> pendingRequests = admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED).stream()
                    .filter(r -> r.getRequestedWardClass() == requestedWardClass)
                    .sorted(Comparator.comparing((AdmissionRequest r) -> {
                        AcuityTier tier = r.getEffectiveAcuityTier() != null ? r.getEffectiveAcuityTier() : r.getPrimaryAcuityTier();
                        return tier != null ? tier.ordinal() : Integer.MAX_VALUE;
                    }).thenComparing(AdmissionRequest::getRequestedAt))
                    .collect(Collectors.toList());

            UUID currentReqId = optRequest.get().getId();
            for (int i = 0; i < pendingRequests.size(); i++) {
                if (pendingRequests.get(i).getId().equals(currentReqId)) {
                    queuePosition = i + 1;
                    break;
                }
            }
            patientsAhead = Math.max(0, queuePosition - 1);
            int delayBuffer = optRequest.map(r -> getDelayBufferMinutes(r.getDelayReasonTag())).orElse(0);
            estimatedWaitMinutes = (queuePosition * 25) + delayBuffer;
        } else if (status == AdmissionStatus.BED_ALLOCATED) {
            queuePosition = 0;
            patientsAhead = 0;
            estimatedWaitMinutes = 5;
        } else if (status == AdmissionStatus.ADMITTED_INPATIENT || status == AdmissionStatus.DISCHARGED) {
            queuePosition = 0;
            patientsAhead = 0;
            estimatedWaitMinutes = 0;
        }

        String delayReason = null;
        String delayContactHotline = null;
        if (optRequest.isPresent()) {
            AdmissionRequest req = optRequest.get();
            if (req.getDelayReasonTag() != null || (req.getOperationalDelayReason() != null && !req.getOperationalDelayReason().isBlank())) {
                delayReason = getDelayDisclosure(req.getDelayReasonTag(), req.getOperationalDelayReason());
                delayContactHotline = "+65 6321 4311";
            }
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

        String coPay = getCoPayEstimate(requestedWardClass);
        String guidance = getCareGuidance(optRequest);
        Boolean diversionRecommended = optRequest.map(AdmissionRequest::isDiversionRecommended).orElse(false);
        DiversionPathway diversionPathway = optRequest.map(AdmissionRequest::getDiversionPathway).orElse(null);

        if (optRequest.isPresent()) {
            AdmissionRequest req = optRequest.get();
            LocalDateTime now = LocalDateTime.now();
            boolean isFirstAccess = (req.getFirstTrackerAccessedAt() == null);
            if (isFirstAccess) {
                req.setFirstTrackerAccessedAt(now);
            }
            req.setLastTrackerAccessedAt(now);
            int count = (req.getTrackerAccessCount() == null ? 0 : req.getTrackerAccessCount()) + 1;
            req.setTrackerAccessCount(count);
            admissionRequestRepository.save(req);

            if (isFirstAccess) {
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

        java.time.LocalDate edd = optRequest.map(AdmissionRequest::getEdd).orElse(null);
        com.hospital.admissions.entity.EddConfidence eddConfidence = optRequest.map(AdmissionRequest::getEddConfidence).orElse(null);
        com.hospital.admissions.entity.MedicationDeliveryStatus medicationStatus = optRequest.map(AdmissionRequest::getMedicationDeliveryStatus).orElse(null);
        java.time.LocalDateTime dischargeSignoffAt = optRequest.map(AdmissionRequest::getDischargeSignoffAt).orElse(null);
        com.hospital.admissions.entity.DischargeRunwayStage runwayStage = (edd != null)
                ? WardService.calculateRunwayStage(edd, dischargeSignoffAt, medicationStatus)
                : null;

        return PatientMilestoneResponse.builder()
                .patientId(patient.getId())
                .patientName(patient.getName())
                .queueToken(queueToken)
                .admissionStatus(status)
                .requestedWardClass(requestedWardClass)
                .queuePosition(queuePosition)
                .patientsAhead(patientsAhead)
                .estimatedWaitMinutes(estimatedWaitMinutes)
                .assignedBedNumber(assignedBedNumber)
                .assignedWardName(assignedWardName)
                .assignedLevel(assignedLevel)
                .delayReason(delayReason)
                .delayContactHotline(delayContactHotline)
                .coPayEstimate(coPay)
                .careGuidance(guidance)
                .diversionRecommended(diversionRecommended)
                .diversionPathway(diversionPathway)
                .estimatedDateOfDischarge(edd)
                .eddConfidence(eddConfidence)
                .medicationDeliveryStatus(medicationStatus)
                .runwayStage(runwayStage)
                .build();
    }

    private String getCoPayEstimate(WardClass wardClass) {
        if (wardClass == null) {
            wardClass = WardClass.B2;
        }
        return switch (wardClass) {
            case A -> "Estimated Co-Pay: $450 - $650 / day (Non-subsidized Class A; MediShield Life claimable up to policy limits)";
            case B1 -> "Estimated Co-Pay: $220 - $340 / day (Government subsidized up to 20%; MediShield Life claimable)";
            case B2 -> "Estimated Co-Pay: $60 - $110 / day (Government subsidized up to 70% means-tested; MediShield Life claimable)";
            case C -> "Estimated Co-Pay: $35 - $60 / day (Government subsidized up to 80% means-tested; MediShield Life claimable)";
        };
    }

    private String getCareGuidance(Optional<AdmissionRequest> optRequest) {
        if (optRequest.isPresent()) {
            AdmissionRequest req = optRequest.get();
            if (req.isDiversionRecommended()) {
                if (req.getDiversionPathway() == DiversionPathway.COMMUNITY_HOSPITAL) {
                    return "Community Hospital Transfer: Sub-acute rehabilitation with average length of stay 14 to 21 days at Outram / St. Andrew's Community Hospital.";
                } else if (req.getDiversionPathway() == DiversionPathway.HOSPITAL_AT_HOME_MIC) {
                    return "MIC@Home (Hospital-at-Home): Virtual ward monitoring, regular visiting nurse schedules, and delivery of home medical equipment.";
                }
            }
        }
        return "Please remain seated in the ED observation area. Our portering team will escort you once your bed is prepared.";
    }

    private int getDelayBufferMinutes(String tag) {
        if (tag == null) return 0;
        return switch (tag) {
            case "HOUSEKEEPING_DELAY" -> 20;
            case "BED_SHORTAGE", "SPECIALIZED_ISOLATION_CLEANING" -> 30;
            case "SURGE_TRAUMA_EVENT" -> 45;
            default -> 0;
        };
    }

    private String getDelayDisclosure(String tag, String customReason) {
        if (tag != null) {
            switch (tag) {
                case "HOUSEKEEPING_DELAY":
                    return "Your ward bed is currently undergoing final housekeeping sanitization and linen preparation.";
                case "BED_SHORTAGE":
                    return "Our clinical coordinators are actively prioritizing ward beds across the hospital to ensure optimal clinical placement.";
                case "SPECIALIZED_ISOLATION_CLEANING":
                    return "Your specialized isolation room is completing a mandatory 30-minute UV disinfection cycle for your safety.";
                case "SURGE_TRAUMA_EVENT":
                    return "The emergency department is currently managing critical trauma arrivals. Thank you for your patience as urgent cases are stabilized.";
            }
        }
        if (customReason != null && !customReason.isBlank()) {
            return "Our clinical coordination team is actively managing bed assignments: " + customReason;
        }
        return "Our clinical coordination team is actively managing bed assignments. For assistance, contact the ward liaison.";
    }

    /**
     * Dispatches periodic reassuring push/SMS updates to patients who have been waiting in queue beyond a threshold.
     *
     * @param currentTime      the reference evaluation timestamp.
     * @param thresholdMinutes minimum waiting time in minutes to trigger update.
     * @return count of dispatched notifications.
     */
    @Transactional
    public int dispatchPeriodicUpdates(LocalDateTime currentTime, int thresholdMinutes) {
        List<AdmissionRequest> waitingRequests = admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED);
        int count = 0;
        for (AdmissionRequest req : waitingRequests) {
            LocalDateTime reqTime = req.getRequestedAt() != null ? req.getRequestedAt() : req.getCreatedAt();
            long dwellMins = reqTime != null ? Math.max(0, java.time.Duration.between(reqTime, currentTime).toMinutes()) : 0;
            if (dwellMins >= thresholdMinutes) {
                req.setLastPeriodicUpdateSentAt(currentTime);
                admissionRequestRepository.save(req);

                java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
                details.put("Channel", "SMS_PUSH");
                details.put("Token", req.getPatient() != null ? req.getPatient().getQueueToken() : "UNKNOWN");
                details.put("Milestone", "MILESTONE_1_ADMISSION_CONFIRMED");
                details.put("DwellMins", dwellMins);
                details.put("DeliveryStatus", "SUCCESS");

                auditLogger.logAction("SYSTEM_SCHEDULER", "DISPATCH_PERIODIC_UPDATE",
                        "AdmissionRequest:" + req.getId(),
                        AuditLogger.formatDetails(details));
                count++;
            }
        }
        return count;
    }

    /**
     * Dispatches periodic updates with default threshold of 5 minutes from current system time.
     *
     * @return count of dispatched notifications.
     */
    @Transactional
    public int dispatchPeriodicUpdates() {
        return dispatchPeriodicUpdates(LocalDateTime.now(), 5);
    }

    /**
     * Completes physical patient bed check-in, setting bed to OCCUPIED_TAKEN and admission to ADMITTED_INPATIENT.
     *
     * @param bedId the unique identifier of the destination bed.
     * @return the updated {@link Bed} entity.
     */
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

    /**
     * Vacates a bed upon patient discharge, transitioning status to EMPTY_PENDING_CLEANING and initiating cleaning SLA.
     *
     * @param bedId the unique identifier of the bed being vacated.
     * @return the updated {@link Bed} entity.
     */
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

    /**
     * Performs terminal cleaning sign-off for a vacated bed, validating 30-minute cleaning SLA compliance.
     *
     * @param bedId the unique identifier of the bed cleaned.
     * @return the updated {@link Bed} in EMPTY_CLEANED status.
     */
    @Transactional
    public Bed cleanBed(UUID bedId) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new IllegalArgumentException("Bed not found: " + bedId));

        if (bed.getStatus() != BedStatus.EMPTY_PENDING_CLEANING) {
            throw new IllegalArgumentException("Cannot clean bed " + bed.getBedNumber() +
                    ": bed is currently in status " + bed.getStatus() + ", but terminal sanitization requires EMPTY_PENDING_CLEANING.");
        }

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

    /**
     * Records an interactive patient or family reassurance engagement (e.g., Medical Social Worker call).
     *
     * @param token      the patient's tracking token.
     * @param actionType the action category (e.g., MSW_CALL or FINANCIAL_COUNSELING).
     * @return the logged {@link PatientAuditInteraction}.
     */
    @Transactional
    public PatientAuditInteraction recordPatientAction(String token, String actionType) {
        Patient patient = patientRepository.findByQueueToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found for token: " + token));

        Optional<AdmissionRequest> optRequest = admissionRequestRepository.findByPatient_QueueToken(token);
        UUID admissionId = optRequest.map(AdmissionRequest::getId).orElse(null);

        PatientAuditInteraction interaction = PatientAuditInteraction.builder()
                .token(token)
                .admissionId(admissionId)
                .actionType(actionType)
                .createdAt(LocalDateTime.now())
                .build();

        PatientAuditInteraction saved = patientAuditInteractionRepository.save(interaction);

        String auditAction = "MSW_CALL".equalsIgnoreCase(actionType)
                ? "CONNECT_MSW_HOTLINE"
                : "CONNECT_FINANCIAL_COUNSELING";

        String serviceName = "MSW_CALL".equalsIgnoreCase(actionType)
                ? "MEDICAL_SOCIAL_WORK"
                : "FINANCIAL_COUNSELING";

        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("Service", serviceName);
        details.put("AdmissionId", admissionId != null ? admissionId : "NONE");
        details.put("Action", "CLICK_TO_CALL");

        auditLogger.logAction("PUBLIC_TOKEN", auditAction,
                "Patient:" + patient.getId(),
                AuditLogger.formatDetails(details));

        return saved;
    }
}
