package com.hospital.admissions.service;

import com.hospital.admissions.entity.*;
import com.hospital.admissions.dto.BmuCapacityForecastDto;
import com.hospital.admissions.dto.DischargeRunwayDto;
import com.hospital.admissions.dto.EddUpdateRequest;
import com.hospital.admissions.dto.TurnoverTaskDto;
import com.hospital.admissions.repository.AdmissionRequestRepository;
import com.hospital.admissions.repository.BedRepository;
import com.hospital.admissions.security.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing inpatient discharge runways, advance EDD forecasting,
 * morning discharge sign-offs, bedside medication delivery, and turnover tasks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WardService {

    private final AdmissionRequestRepository admissionRequestRepository;
    private final BedRepository bedRepository;
    private final AuditLogger auditLogger;

    /**
     * Establishes or updates an advance Estimated Date of Discharge (EDD) for an admitted inpatient.
     * Emits a structured RECORD_EDD audit log event to track forward planning compliance (KPI 20).
     *
     * @param patientId the unique identifier of the admitted patient.
     * @param request   the {@link EddUpdateRequest} containing date, confidence, and rationale.
     * @return the updated {@link AdmissionRequest}.
     */
    @Transactional
    public AdmissionRequest updateEdd(UUID patientId, EddUpdateRequest request) {
        AdmissionRequest admissionRequest = admissionRequestRepository.findByPatient_Id(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Admission request not found for patient: " + patientId));

        if (request.getEdd() == null || request.getEddConfidence() == null) {
            throw new IllegalArgumentException("EDD and confidence rating must be provided");
        }

        admissionRequest.setEdd(request.getEdd());
        admissionRequest.setEddConfidence(request.getEddConfidence());
        admissionRequest.setEddRationale(request.getRationale());
        admissionRequest.setEddRecordedAt(LocalDateTime.now());

        AdmissionRequest saved = admissionRequestRepository.save(admissionRequest);

        DischargeRunwayStage runwayStage = calculateRunwayStage(
                saved.getEdd(),
                saved.getDischargeSignoffAt(),
                saved.getMedicationDeliveryStatus()
        );

        String currentUser = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "SYSTEM";

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("PatientId", patientId);
        details.put("EDD", saved.getEdd());
        details.put("Confidence", saved.getEddConfidence());
        details.put("RunwayStage", runwayStage);

        auditLogger.logAction(currentUser, "RECORD_EDD",
                "AdmissionRequest:" + saved.getId(),
                AuditLogger.formatDetails(details));

        return saved;
    }

    /**
     * Pure function calculating the dynamic discharge runway stage based on the target EDD,
     * morning sign-off timestamp, and bedside medication dispensing status.
     *
     * @param edd                the estimated date of discharge.
     * @param dischargeSignoffAt optional morning clinician authorization timestamp.
     * @param medicationStatus   current dispensing status of discharge medications.
     * @return dynamic {@link DischargeRunwayStage}.
     */
    public static DischargeRunwayStage calculateRunwayStage(
            LocalDate edd,
            LocalDateTime dischargeSignoffAt,
            MedicationDeliveryStatus medicationStatus) {

        if (medicationStatus == MedicationDeliveryStatus.DELIVERED_BEDSIDE) {
            return DischargeRunwayStage.READY_TO_VACATE;
        }
        if (dischargeSignoffAt != null || medicationStatus == MedicationDeliveryStatus.PACKING_IN_PROGRESS) {
            return DischargeRunwayStage.MEDICATIONS_PENDING;
        }

        if (edd == null) {
            return DischargeRunwayStage.RUNWAY_D3; // Default or unassigned
        }

        LocalDate today = LocalDate.now();
        long daysUntilDischarge = ChronoUnit.DAYS.between(today, edd);

        if (daysUntilDischarge >= 3) {
            return DischargeRunwayStage.RUNWAY_D3;
        } else if (daysUntilDischarge == 2) {
            return DischargeRunwayStage.RUNWAY_D2;
        } else if (daysUntilDischarge == 1) {
            return DischargeRunwayStage.RUNWAY_D1;
        }

        return DischargeRunwayStage.READY_FOR_MORNING_SIGNOFF;
    }

    /**
     * Returns the active discharge runway list across all wards for currently admitted inpatients.
     *
     * @return list of {@link DischargeRunwayDto} tracking each inpatient's discharge runway status.
     */
    @Transactional(readOnly = true)
    public List<DischargeRunwayDto> getRunway() {
        List<AdmissionRequest> admittedList = admissionRequestRepository.findByStatus(AdmissionStatus.ADMITTED_INPATIENT);

        return admittedList.stream()
                .map(req -> {
                    Patient p = req.getPatient();
                    Bed bed = req.getAssignedBed();
                    Ward ward = bed != null ? bed.getWard() : null;

                    DischargeRunwayStage stage = calculateRunwayStage(
                            req.getEdd(),
                            req.getDischargeSignoffAt(),
                            req.getMedicationDeliveryStatus()
                    );

                    return DischargeRunwayDto.builder()
                            .patientId(p != null ? p.getId() : null)
                            .patientName(p != null ? p.getName() : "Unknown")
                            .bedId(bed != null ? bed.getId() : null)
                            .bedNumber(bed != null ? bed.getBedNumber() : "UNASSIGNED")
                            .wardCode(ward != null ? ward.getName() : "UNKNOWN")
                            .levelNumber(ward != null ? ward.getLevel() : null)
                            .estimatedDateOfDischarge(req.getEdd())
                            .confidence(req.getEddConfidence())
                            .runwayStage(stage)
                            .dischargeSignoffAt(req.getDischargeSignoffAt())
                            .medicationStatus(req.getMedicationDeliveryStatus() != null ? req.getMedicationDeliveryStatus() : MedicationDeliveryStatus.NOT_DISPATCHED)
                            .rationale(req.getEddRationale())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Computes aggregate 24 to 72-hour discharge capacity projections grouped by ward and specialty cluster.
     *
     * @return {@link BmuCapacityForecastDto} containing projected vacancy metrics.
     */
    @Transactional(readOnly = true)
    public BmuCapacityForecastDto getCapacityForecast() {
        List<AdmissionRequest> admittedList = admissionRequestRepository.findByStatus(AdmissionStatus.ADMITTED_INPATIENT);
        LocalDate today = LocalDate.now();

        Map<String, BmuCapacityForecastDto.WardCapacityProjection> wardMap = new LinkedHashMap<>();
        Map<SpecialtyCluster, BmuCapacityForecastDto.ClusterCapacityProjection> clusterMap = new LinkedHashMap<>();

        int total24 = 0;
        int total48 = 0;
        int total72 = 0;

        for (AdmissionRequest req : admittedList) {
            if (req.getEdd() == null) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(today, req.getEdd());
            if (days < 0 || days > 3) {
                continue;
            }

            Bed bed = req.getAssignedBed();
            Ward ward = bed != null ? bed.getWard() : null;
            String wardCode = ward != null ? ward.getName() : "Other";
            SpecialtyCluster cluster = (ward != null && ward.getServiceCluster() != null)
                    ? ward.getServiceCluster()
                    : req.getSuspectedDiagnosisService();

            boolean is24 = (days <= 1);
            boolean is48 = (days == 2);
            boolean is72 = (days == 3);

            if (is24) total24++;
            if (is48) total48++;
            if (is72) total72++;

            // Ward Aggregation
            BmuCapacityForecastDto.WardCapacityProjection wardProj = wardMap.computeIfAbsent(wardCode, k ->
                    BmuCapacityForecastDto.WardCapacityProjection.builder()
                            .wardCode(wardCode)
                            .cluster(cluster)
                            .build());
            if (is24) wardProj.setNext24Hours(wardProj.getNext24Hours() + 1);
            if (is48) wardProj.setNext48Hours(wardProj.getNext48Hours() + 1);
            if (is72) wardProj.setNext72Hours(wardProj.getNext72Hours() + 1);
            wardProj.setTotal(wardProj.getTotal() + 1);

            // Cluster Aggregation
            if (cluster != null) {
                BmuCapacityForecastDto.ClusterCapacityProjection clusterProj = clusterMap.computeIfAbsent(cluster, k ->
                        BmuCapacityForecastDto.ClusterCapacityProjection.builder()
                                .cluster(cluster)
                                .build());
                if (is24) clusterProj.setNext24Hours(clusterProj.getNext24Hours() + 1);
                if (is48) clusterProj.setNext48Hours(clusterProj.getNext48Hours() + 1);
                if (is72) clusterProj.setNext72Hours(clusterProj.getNext72Hours() + 1);
                clusterProj.setTotal(clusterProj.getTotal() + 1);
            }
        }

        return BmuCapacityForecastDto.builder()
                .totalNext24Hours(total24)
                .totalNext48Hours(total48)
                .totalNext72Hours(total72)
                .byWard(new ArrayList<>(wardMap.values()))
                .byCluster(new ArrayList<>(clusterMap.values()))
                .build();
    }

    /**
     * Executes morning discharge authorization (targeted before 09:30 AM),
     * moving medication delivery status to PACKING_IN_PROGRESS and logging audit events.
     *
     * @param patientId the unique identifier of the patient being authorized.
     * @return the updated {@link AdmissionRequest}.
     */
    @Transactional
    public AdmissionRequest dischargeSignoff(UUID patientId) {
        AdmissionRequest admissionRequest = admissionRequestRepository.findByPatient_Id(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Admission request not found for patient: " + patientId));

        LocalDateTime now = LocalDateTime.now();
        admissionRequest.setDischargeSignoffAt(now);
        admissionRequest.setMedicationDeliveryStatus(MedicationDeliveryStatus.PACKING_IN_PROGRESS);

        AdmissionRequest saved = admissionRequestRepository.save(admissionRequest);

        String doctorId = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "SYSTEM";

        // Audit Event 1: DISCHARGE_SIGNOFF
        Map<String, Object> signoffDetails = new LinkedHashMap<>();
        signoffDetails.put("DoctorId", doctorId);
        signoffDetails.put("SignOffTime", now);
        signoffDetails.put("PreDischargeHour", now.getHour());

        auditLogger.logAction(doctorId, "DISCHARGE_SIGNOFF",
                "AdmissionRequest:" + saved.getId(),
                AuditLogger.formatDetails(signoffDetails));

        // Audit Event 2: DISPENSE_MEDICATION (Target SLA 11:00 AM)
        Map<String, Object> dispenseDetails = new LinkedHashMap<>();
        dispenseDetails.put("PatientId", patientId);
        dispenseDetails.put("WardBed", saved.getAssignedBed() != null ? saved.getAssignedBed().getBedNumber() : "UNKNOWN");
        dispenseDetails.put("TargetSla", "11:00 AM");

        auditLogger.logAction(doctorId, "DISPENSE_MEDICATION",
                "AdmissionRequest:" + saved.getId(),
                AuditLogger.formatDetails(dispenseDetails));

        return saved;
    }

    /**
     * Confirms bedside delivery of pre-packed medications, advancing runway stage to READY_TO_VACATE.
     *
     * @param patientId the unique identifier of the patient receiving discharge meds.
     * @return the updated {@link AdmissionRequest}.
     */
    @Transactional
    public AdmissionRequest deliverMedication(UUID patientId) {
        AdmissionRequest admissionRequest = admissionRequestRepository.findByPatient_Id(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Admission request not found for patient: " + patientId));

        LocalDateTime now = LocalDateTime.now();
        admissionRequest.setMedicationDeliveryStatus(MedicationDeliveryStatus.DELIVERED_BEDSIDE);

        AdmissionRequest saved = admissionRequestRepository.save(admissionRequest);

        String confirmerUser = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "SYSTEM";

        UUID bedId = saved.getAssignedBed() != null ? saved.getAssignedBed().getId() : null;

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("PatientId", patientId);
        details.put("BedId", bedId != null ? bedId : "UNKNOWN");
        details.put("ConfirmedBy", confirmerUser);
        details.put("DeliveryTime", now);

        auditLogger.logAction(confirmerUser, "DELIVER_BEDSIDE_MEDICATION",
                "AdmissionRequest:" + saved.getId(),
                AuditLogger.formatDetails(details));

        return saved;
    }

    /**
     * Retrieves active vacated beds in EMPTY_PENDING_CLEANING with dynamic 30-minute SLA countdowns.
     *
     * @return list of {@link TurnoverTaskDto} representing pending terminal sanitization tasks.
     */
    @Transactional(readOnly = true)
    public List<TurnoverTaskDto> getTurnoverTasks() {
        List<Bed> pendingBeds = bedRepository.findByStatus(BedStatus.EMPTY_PENDING_CLEANING);
        LocalDateTime now = LocalDateTime.now();

        return pendingBeds.stream()
                .map(bed -> {
                    LocalDateTime start = bed.getCleaningStartedAt() != null ? bed.getCleaningStartedAt() : bed.getLastModifiedAt();
                    if (start == null) {
                        start = now;
                    }
                    long elapsedMinutes = Duration.between(start, now).toMinutes();
                    long remainingMinutes = 30 - elapsedMinutes;

                    TurnoverSlaStatus slaStatus;
                    if (remainingMinutes < 0) {
                        slaStatus = TurnoverSlaStatus.BREACHED;
                    } else if (remainingMinutes <= 10) {
                        slaStatus = TurnoverSlaStatus.APPROACHING_SLA;
                    } else {
                        slaStatus = TurnoverSlaStatus.ON_TRACK;
                    }

                    Ward ward = bed.getWard();

                    return TurnoverTaskDto.builder()
                            .bedId(bed.getId())
                            .bedNumber(bed.getBedNumber())
                            .wardCode(ward != null ? ward.getName() : "UNKNOWN")
                            .levelNumber(ward != null ? ward.getLevel() : null)
                            .vacatedAt(start)
                            .cleaningStartedAt(start)
                            .remainingMinutes(remainingMinutes)
                            .slaStatus(slaStatus)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
