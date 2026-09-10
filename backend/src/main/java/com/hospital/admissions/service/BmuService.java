package com.hospital.admissions.service;

import com.hospital.admissions.entity.*;
import com.hospital.admissions.dto.BatchApprovalRequest;
import com.hospital.admissions.dto.BatchSuggestion;
import com.hospital.admissions.dto.BedDto;
import com.hospital.admissions.dto.CohortSwapApprovalRequest;
import com.hospital.admissions.dto.CohortSwapSuggestion;
import com.hospital.admissions.dto.BedRecommendation;
import com.hospital.admissions.dto.BmuConfigUpdateRequest;
import com.hospital.admissions.dto.SisterHospitalReferralResponse;
import com.hospital.admissions.dto.WardDto;
import com.hospital.admissions.gateway.SisterHospitalGateway;
import com.hospital.admissions.repository.AdmissionRequestRepository;
import com.hospital.admissions.repository.BedRepository;
import com.hospital.admissions.repository.BmuAlgorithmConfigRepository;
import com.hospital.admissions.repository.WardRepository;
import com.hospital.admissions.security.AuditLogger;
import com.hospital.admissions.solver.BedAllocationSolver;
import com.hospital.admissions.dto.DelayTagRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BmuService {

    private final AdmissionRequestRepository admissionRequestRepository;
    private final BedRepository bedRepository;
    private final WardRepository wardRepository;
    private final BmuAlgorithmConfigRepository configRepository;
    private final BedAllocationSolver solver;
    private final SisterHospitalGateway sisterHospitalGateway;
    private final AuditLogger auditLogger;

    public List<AdmissionRequest> getPrioritizedQueue() {
        List<AdmissionRequest> pendingRequests = admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED);

        // Sort by Primary: Effective Acuity Tier (ordinal 0=Tier1 is highest priority), Secondary: Telemetry requirement, Tertiary: requestedAt FIFO
        return pendingRequests.stream()
                .sorted(Comparator.comparing((AdmissionRequest r) -> {
                            AcuityTier tier = r.getEffectiveAcuityTier() != null ? r.getEffectiveAcuityTier() : r.getPrimaryAcuityTier();
                            return tier != null ? tier.ordinal() : Integer.MAX_VALUE;
                        })
                        .thenComparing((AdmissionRequest r) -> Boolean.TRUE.equals(r.getEffectiveTelemetry()) || Boolean.TRUE.equals(r.getPrimaryTelemetry()) ? 0 : 1)
                        .thenComparing(AdmissionRequest::getRequestedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public List<AdmissionRequest> getQueue() {
        return getPrioritizedQueue();
    }

    public List<BedRecommendation> getRecommendations(UUID admissionRequestId) {
        AdmissionRequest request = admissionRequestRepository.findById(admissionRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Admission request not found: " + admissionRequestId));

        BmuAlgorithmConfig config = getOrCreateConfig();
        return solver.recommendBeds(request, config);
    }

    @Transactional
    public AdmissionRequest allocateBed(UUID admissionRequestId, UUID bedId) {
        return allocateBed(admissionRequestId, bedId, 1, 85.0, null);
    }

    @Transactional
    public AdmissionRequest allocateBed(UUID admissionRequestId, UUID bedId, Integer rank, Double score, String overrideReason) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        AdmissionRequest request = admissionRequestRepository.findById(admissionRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Admission request not found: " + admissionRequestId));

        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new IllegalArgumentException("Bed not found: " + bedId));

        if (bed.getStatus() != BedStatus.EMPTY_CLEANED) {
            throw new IllegalStateException("Bed " + bed.getBedNumber() + " is not EMPTY_CLEANED. Current status: " + bed.getStatus());
        }

        Patient patient = request.getPatient();
        Ward ward = bed.getWard();

        // 1. Absolute Safety Invariants (Zero Override Allowed -> 422)
        // Biological gender cohorting in multi-bed wards
        if (ward.getCapacity() > 1 && patient != null && patient.getGender() != null) {
            if (ward.getLockedGender() != null && !ward.getLockedGender().equals(patient.getGender())) {
                throw new com.hospital.admissions.exception.SafetyInvariantViolationException("Cannot allocate bed " + bed.getBedNumber() +
                        ": Biological gender cohorting violation in multi-bed ward " + ward.getName() +
                        " (Ward locked to " + ward.getLockedGender() + ", patient is " + patient.getGender() + ").");
            }
        }

        // Negative pressure airborne isolation
        if (patient != null && patient.getInfectionStatus() == InfectionStatus.RESPIRATORY) {
            if (!ward.isNegativePressure()) {
                throw new com.hospital.admissions.exception.SafetyInvariantViolationException("Cannot allocate bed " + bed.getBedNumber() +
                        ": Airborne respiratory infection requires negative pressure isolation, but ward " +
                        ward.getName() + " does not have negative pressure capability.");
            }
        }

        // 2. Overridable Operational Constraints (Requires structured reason code -> 400)
        boolean wardClassMismatch = request.getRequestedWardClass() != null &&
                !ward.getWardClass().equals(request.getRequestedWardClass());

        boolean requiresTelemetry = Boolean.TRUE.equals(request.getEffectiveTelemetry())
                || Boolean.TRUE.equals(request.getPrimaryTelemetry())
                || (patient != null && patient.isNeedsTelemetry());
        boolean telemetryMismatch = requiresTelemetry && !bed.isHasTelemetry();

        boolean hasOperationalBreach = wardClassMismatch || telemetryMismatch;
        if (hasOperationalBreach && (overrideReason == null || overrideReason.isBlank())) {
            throw new IllegalArgumentException("Operational constraint override requires a valid structured reason code (e.g. GOVERNMENT_SUBSIDY_CLASS_UPGRADE, EMERGENCY_PORTABLE_TELEMETRY_DEPLOYED).");
        }

        // If ward is multi-bed and has no locked gender, cohort lock to first patient
        if (ward.getCapacity() > 1 && ward.getLockedGender() == null && patient != null && patient.getGender() != null) {
            ward.setLockedGender(patient.getGender());
            wardRepository.save(ward);
        }

        // Dynamic Reallocation: If request was already tentatively allocated to a different bed, free the old bed
        if (request.getAssignedBed() != null && !request.getAssignedBed().getId().equals(bedId)) {
            Bed oldBed = request.getAssignedBed();
            oldBed.setStatus(BedStatus.EMPTY_CLEANED);
            oldBed.setCurrentPatient(null);
            bedRepository.save(oldBed);
        }

        bed.setStatus(BedStatus.EMPTY_ASSIGNED);
        bed.setCurrentPatient(request.getPatient());
        bedRepository.save(bed);

        request.setStatus(AdmissionStatus.BED_ALLOCATED);
        request.setAssignedBed(bed);
        request.setAllocatedAt(LocalDateTime.now());

        boolean isOverride = (overrideReason != null && !overrideReason.isBlank());
        request.setIsRecommendationAccepted(!isOverride);
        if (isOverride) {
            request.setOverrideReasonCode(overrideReason);
        }

        if (request.getDelayReasonTag() != null) {
            request.setArchivedDelayReasonTag(request.getDelayReasonTag());
            request.setArchivedOperationalDelayReason(request.getOperationalDelayReason());
            auditLogger.logAction(currentUser, "ARCHIVE_DELAY_TAG",
                    "AdmissionRequest:" + admissionRequestId,
                    String.format("Archived delay reason %s upon bed allocation", request.getDelayReasonTag()));
            request.setDelayReasonTag(null);
            request.setOperationalDelayReason(null);
        }

        AdmissionRequest savedRequest = admissionRequestRepository.save(request);

        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("AssignedBed", bed.getBedNumber());
        details.put("Ward", bed.getWard().getName());

        if (isOverride) {
            details.put("SelectedRank", rank != null ? rank : 2);
            details.put("OverrideReason", overrideReason);
            details.put("Override", true);
            auditLogger.logAction(currentUser, "OVERRIDE_ALLOCATION",
                    "AdmissionRequest:" + admissionRequestId,
                    AuditLogger.formatDetails(details));
        } else {
            details.put("Rank", rank != null ? rank : 1);
            details.put("Score", score != null ? score : 85.0);
            details.put("Override", false);
            auditLogger.logAction(currentUser, "ALLOCATE_BED",
                    "AdmissionRequest:" + admissionRequestId,
                    AuditLogger.formatDetails(details));
        }

        return savedRequest;
    }

    public List<WardDto> getInventory() {
        return wardRepository.findAllByOrderByLevelAscNameAsc().stream()
                .map(w -> new WardDto(
                        w.getId(),
                        w.getName().replace("Ward ", ""),
                        w.getLevel(),
                        w.getWardClass(),
                        w.getServiceCluster(),
                        w.getLockedGender(),
                        w.getLockedInfectionStatus(),
                        w.getBeds().stream()
                                .map(b -> new BedDto(
                                        b.getId(),
                                        b.getBedNumber(),
                                        b.getStatus(),
                                        b.isHasTelemetry(),
                                        b.isNearNursingStation(),
                                        b.getCurrentPatient()
                                ))
                                .toList()
                ))
                .toList();
    }

    public BmuAlgorithmConfig getOrCreateConfig() {
        return configRepository.findAll().stream().findFirst().orElseGet(() -> {
            BmuAlgorithmConfig defaultConfig = BmuAlgorithmConfig.builder()
                    .weightSpecialtyCluster(40)
                    .weightConsolidation(30)
                    .weightFallRiskStation(15)
                    .batchHoldingWardThreshold(3)
                    .build();
            return configRepository.save(defaultConfig);
        });
    }

    @Transactional
    public BmuAlgorithmConfig updateConfig(BmuConfigUpdateRequest req) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        BmuAlgorithmConfig config = getOrCreateConfig();
        config.setWeightSpecialtyCluster(req.getWeightSpecialtyCluster());
        config.setWeightConsolidation(req.getWeightConsolidation());
        config.setWeightFallRiskStation(req.getWeightFallRiskStation());
        config.setBatchHoldingWardThreshold(req.getBatchHoldingWardThreshold());

        BmuAlgorithmConfig updated = configRepository.save(config);
        auditLogger.logAction(currentUser, "UPDATE_BMU_CONFIG",
                "BmuAlgorithmConfig:" + updated.getId(),
                "Weights: Specialty=" + req.getWeightSpecialtyCluster() + ", Consolidation=" + req.getWeightConsolidation());
        return updated;
    }

    @Transactional
    public SisterHospitalReferralResponse referToSisterHospital(UUID admissionRequestId, String targetFacility) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();

        AdmissionRequest request = admissionRequestRepository.findById(admissionRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Admission request not found: " + admissionRequestId));

        SisterHospitalReferralResponse response = sisterHospitalGateway.referPatient(request.getPatient(), request, targetFacility);

        request.setDiversionRecommended(true);
        request.setSisterHospitalReferralId(response.getReferralId());
        request.setReferralDispatchedAt(LocalDateTime.now());
        request.setReferralSlaMinutes(30);
        request.setReferralFacility(targetFacility);

        boolean isMic = (targetFacility != null && (targetFacility.toUpperCase().contains("MIC") || targetFacility.toUpperCase().contains("HOME")))
                || request.getDiversionPathway() == DiversionPathway.HOSPITAL_AT_HOME_MIC;

        if (isMic) {
            String virtualBed = "MIC-V" + (100 + (request.getPatient() != null && request.getPatient().getId() != null ? Math.abs(request.getPatient().getId().hashCode() % 900) : 101));
            request.setVirtualBedNumber(virtualBed);
            request.setStatus(AdmissionStatus.DIVERTED_HAH);
        }

        admissionRequestRepository.save(request);

        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("Facility", response.getDestinationFacility());
        details.put("ReferralId", response.getReferralId());
        details.put("SlaWindowMins", 30);
        details.put("AcuityTier", request.getPrimaryAcuityTier());
        if (isMic) {
            details.put("VirtualBed", request.getVirtualBedNumber());
        }

        auditLogger.logAction(currentUser, "DIVERSION_REFERRAL",
                "AdmissionRequest:" + admissionRequestId,
                AuditLogger.formatDetails(details));

        return response;
    }

    @Transactional
    public AdmissionRequest recallDiversionToAcuteQueue(UUID admissionRequestId) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "bmu_coord";

        AdmissionRequest request = admissionRequestRepository.findById(admissionRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Admission request not found: " + admissionRequestId));

        request.setStatus(AdmissionStatus.BED_REQUESTED);
        request.setSisterHospitalReferralId(null);
        request.setDiversionRecommended(false);
        request.setReferralDispatchedAt(null);
        request.setVirtualBedNumber(null);
        AdmissionRequest saved = admissionRequestRepository.save(request);

        auditLogger.logAction(currentUser, "RECALL_TO_ACUTE_QUEUE",
                "AdmissionRequest:" + admissionRequestId,
                "Referral recalled. Patient returned to acute admission queue.");

        return saved;
    }

    @Transactional
    public AdmissionRequest extendDiversionSla(UUID admissionRequestId) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "bmu_coord";

        AdmissionRequest request = admissionRequestRepository.findById(admissionRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Admission request not found: " + admissionRequestId));

        int currentSla = request.getReferralSlaMinutes() != null ? request.getReferralSlaMinutes() : 30;
        request.setReferralSlaMinutes(currentSla + 15);
        request.setOperationalDelayReason("Diversion SLA extended by 15 mins");
        AdmissionRequest saved = admissionRequestRepository.save(request);

        auditLogger.logAction(currentUser, "EXTEND_DIVERSION_SLA",
                "AdmissionRequest:" + admissionRequestId,
                "Extended diversion SLA to " + (currentSla + 15) + " minutes.");

        return saved;
    }

    @Transactional
    public AdmissionRequest logTelephoneFollowUp(UUID admissionRequestId, String notes) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "bmu_coord";

        AdmissionRequest request = admissionRequestRepository.findById(admissionRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Admission request not found: " + admissionRequestId));

        request.setOperationalDelayReason(notes);
        AdmissionRequest saved = admissionRequestRepository.save(request);

        auditLogger.logAction(currentUser, "LOG_TELEPHONE_FOLLOW_UP",
                "AdmissionRequest:" + admissionRequestId,
                "Telephone follow-up note: " + notes);

        return saved;
    }

    @Transactional
    public AdmissionRequest requestReconciliation(UUID admissionRequestId) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "bmu_coordinator";

        AdmissionRequest request = admissionRequestRepository.findById(admissionRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Admission request not found: " + admissionRequestId));

        request.setReconciliationRequested(true);
        AdmissionRequest saved = admissionRequestRepository.save(request);

        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("EffectiveAcuity", request.getEffectiveAcuityTier());
        details.put("Discordant", request.getIsDiscordant());
        details.put("ReconciliationRequested", true);

        auditLogger.logAction(currentUser, "REQUEST_CLINICAL_RECONCILIATION",
                "AdmissionRequest:" + admissionRequestId,
                AuditLogger.formatDetails(details));

        return saved;
    }

    @Transactional
    public AdmissionRequest assignAdmittingCluster(UUID admissionRequestId, SpecialtyCluster cluster) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "bmu_coordinator";
        AdmissionRequest request = admissionRequestRepository.findById(admissionRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Admission request not found: " + admissionRequestId));

        request.setAdmittingSpecialtyCluster(cluster);
        AdmissionRequest saved = admissionRequestRepository.save(request);

        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("AdmittingSpecialtyCluster", cluster);
        details.put("EffectiveAcuity", request.getEffectiveAcuityTier());

        auditLogger.logAction(currentUser, "ASSIGN_ADMITTING_CLUSTER",
                "AdmissionRequest:" + admissionRequestId,
                AuditLogger.formatDetails(details));

        return saved;
    }

    @Transactional
    public AdmissionRequest deallocateBed(UUID admissionRequestId) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "bmu_coordinator";

        AdmissionRequest request = admissionRequestRepository.findById(admissionRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Admission request not found: " + admissionRequestId));

        Bed bed = request.getAssignedBed();
        if (bed == null) {
            throw new IllegalArgumentException("No bed currently allocated to request: " + admissionRequestId);
        }

        if (bed.getStatus() == BedStatus.OCCUPIED_TAKEN || request.getStatus() == AdmissionStatus.ADMITTED_INPATIENT) {
            throw new IllegalStateException("Cannot deallocate: Patient has already arrived at bed " + bed.getBedNumber());
        }

        bed.setStatus(BedStatus.EMPTY_CLEANED);
        bed.setCurrentPatient(null);
        bedRepository.save(bed);

        request.setAssignedBed(null);
        request.setStatus(AdmissionStatus.BED_REQUESTED);
        request.setAllocatedAt(null);
        AdmissionRequest saved = admissionRequestRepository.save(request);

        auditLogger.logAction(currentUser, "DEALLOCATE_BED",
                "AdmissionRequest:" + admissionRequestId,
                "Reverted bed " + bed.getBedNumber() + " to EMPTY_CLEANED; reset request to BED_REQUESTED");

        return saved;
    }

    @Transactional
    public Bed confirmArrival(UUID bedId) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "ward_nurse";

        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new IllegalArgumentException("Bed not found: " + bedId));

        if (bed.getStatus() != BedStatus.EMPTY_ASSIGNED) {
            throw new IllegalStateException("Bed " + bed.getBedNumber() + " is not EMPTY_ASSIGNED. Current status: " + bed.getStatus());
        }

        bed.setStatus(BedStatus.OCCUPIED_TAKEN);
        Bed savedBed = bedRepository.save(bed);

        admissionRequestRepository.findByAssignedBed_Id(bedId).ifPresent(req -> {
            req.setStatus(AdmissionStatus.ADMITTED_INPATIENT);
            admissionRequestRepository.save(req);
        });

        auditLogger.logAction(currentUser, "CONFIRM_PATIENT_ARRIVAL",
                "Bed:" + bedId,
                "Confirmed physical arrival for Bed " + bed.getBedNumber() + "; status OCCUPIED_TAKEN");

        return savedBed;
    }

    @Transactional
    public Bed vacateBed(UUID bedId) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "ward_nurse";

        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new IllegalArgumentException("Bed not found: " + bedId));

        if (bed.getStatus() != BedStatus.OCCUPIED_TAKEN) {
            throw new IllegalStateException("Bed " + bed.getBedNumber() + " is not OCCUPIED_TAKEN. Current status: " + bed.getStatus());
        }

        bed.setStatus(BedStatus.EMPTY_PENDING_CLEANING);
        bed.setCurrentPatient(null);
        bed.setCleaningStartedAt(LocalDateTime.now());
        Bed savedBed = bedRepository.save(bed);

        admissionRequestRepository.findByAssignedBed_Id(bedId).ifPresent(req -> {
            req.setStatus(AdmissionStatus.DISCHARGED);
            req.setAssignedBed(null);
            admissionRequestRepository.save(req);
        });

        auditLogger.logAction(currentUser, "VACATE_BED",
                "Bed:" + bedId,
                "Bed " + bed.getBedNumber() + " vacated, transitioned to EMPTY_PENDING_CLEANING with 30m cleaning SLA");

        return savedBed;
    }

    @Transactional
    public Bed signOffCleaning(UUID bedId) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "housekeeping";

        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new IllegalArgumentException("Bed not found: " + bedId));

        if (bed.getStatus() != BedStatus.EMPTY_PENDING_CLEANING) {
            throw new IllegalStateException("Bed " + bed.getBedNumber() + " is not EMPTY_PENDING_CLEANING. Current status: " + bed.getStatus());
        }

        bed.setStatus(BedStatus.EMPTY_CLEANED);
        bed.setLastCleanedAt(LocalDateTime.now());
        bed.setCleaningStartedAt(null);
        Bed savedBed = bedRepository.save(bed);

        auditLogger.logAction(currentUser, "SIGN_OFF_CLEANING",
                "Bed:" + bedId,
                "Housekeeping terminal clean signed off for Bed " + bed.getBedNumber());

        // All-Clean Ward Reset
        Ward ward = bed.getWard();
        if (ward != null) {
            List<Bed> wardBeds = bedRepository.findByWard_Id(ward.getId());
            boolean allClean = wardBeds.stream().allMatch(b -> b.getStatus() == BedStatus.EMPTY_CLEANED);
            if (allClean) {
                ward.setLockedGender(null);
                ward.setLockedInfectionStatus(null);
                ward.setHoldingWard(false);
                wardRepository.save(ward);

                auditLogger.logAction(currentUser, "ALL_CLEAN_WARD_RESET",
                        "Ward:" + ward.getId(),
                        "All beds in ward " + ward.getName() + " reached EMPTY_CLEANED; cohort locks released");
            }
        }

        return savedBed;
    }

    public List<BatchSuggestion> getBatchSuggestions() {
        List<AdmissionRequest> pendingRequests = admissionRequestRepository.findByStatus(AdmissionStatus.BED_REQUESTED);

        Map<String, List<AdmissionRequest>> clusters = pendingRequests.stream()
                .filter(r -> r.getPatient() != null && r.getPatient().getGender() != null && r.getRequestedWardClass() != null)
                .collect(Collectors.groupingBy(r -> {
                    InfectionStatus inf = r.getPatient().getInfectionStatus() != null ? r.getPatient().getInfectionStatus() : InfectionStatus.NON_INFECTIOUS;
                    return r.getRequestedWardClass().name() + "_" + r.getPatient().getGender().name() + "_" + inf.name();
                }));

        List<Ward> allWards = wardRepository.findAll();
        List<BatchSuggestion> suggestions = new ArrayList<>();

        for (Map.Entry<String, List<AdmissionRequest>> entry : clusters.entrySet()) {
            List<AdmissionRequest> clusterRequests = new ArrayList<>(entry.getValue());
            if (clusterRequests.size() < 3) {
                continue;
            }

            clusterRequests.sort(Comparator.comparing(AdmissionRequest::getRequestedAt, Comparator.nullsLast(Comparator.naturalOrder())));

            AdmissionRequest first = clusterRequests.get(0);
            WardClass wardClass = first.getRequestedWardClass();
            Gender gender = first.getPatient().getGender();
            InfectionStatus infectionStatus = first.getPatient().getInfectionStatus() != null ? first.getPatient().getInfectionStatus() : InfectionStatus.NON_INFECTIOUS;

            for (Ward ward : allWards) {
                if (ward.getWardClass() != wardClass) {
                    continue;
                }
                if (infectionStatus == InfectionStatus.RESPIRATORY && !ward.isNegativePressure()) {
                    continue;
                }
                if (infectionStatus != InfectionStatus.RESPIRATORY && ward.isNegativePressure()) {
                    continue;
                }

                List<Bed> wardBeds = bedRepository.findByWard_Id(ward.getId());
                if (wardBeds.isEmpty() || !wardBeds.stream().allMatch(b -> b.getStatus() == BedStatus.EMPTY_CLEANED)) {
                    continue;
                }

                int sliceSize = Math.min(clusterRequests.size(), wardBeds.size());
                if (sliceSize < 3) {
                    continue;
                }
                List<AdmissionRequest> sliced = clusterRequests.subList(0, sliceSize);

                suggestions.add(BatchSuggestion.builder()
                        .suggestionId(UUID.randomUUID().toString())
                        .targetWardId(ward.getId())
                        .targetWardName(ward.getName())
                        .patientIds(sliced.stream().map(r -> r.getPatient().getId()).collect(Collectors.toList()))
                        .patientNames(sliced.stream().map(r -> r.getPatient().getName()).collect(Collectors.toList()))
                        .admissionRequestIds(sliced.stream().map(AdmissionRequest::getId).collect(Collectors.toList()))
                        .commonWardClass(wardClass)
                        .commonGender(gender)
                        .commonInfectionStatus(infectionStatus)
                        .unlockedCapacityCount(wardBeds.size())
                        .build());
            }
        }

        return suggestions;
    }

    @Transactional
    public List<AdmissionRequest> approveBatchHoldingWard(BatchApprovalRequest request) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "bmu_coord";

        UUID wardId = request.getTargetWardId();
        Ward ward = wardRepository.findById(wardId)
                .orElseThrow(() -> new IllegalArgumentException("Ward not found: " + wardId));

        List<Bed> wardBeds = bedRepository.findByWard_Id(wardId);
        List<Bed> cleanBeds = wardBeds.stream()
                .filter(b -> b.getStatus() == BedStatus.EMPTY_CLEANED)
                .collect(Collectors.toList());

        List<UUID> reqIds = request.getAdmissionRequestIds();
        if (cleanBeds.size() < reqIds.size()) {
            throw new IllegalStateException("Insufficient clean beds in ward " + ward.getName() + " to satisfy batch of " + reqIds.size());
        }

        List<AdmissionRequest> allocatedRequests = new ArrayList<>();

        for (int i = 0; i < reqIds.size(); i++) {
            UUID reqId = reqIds.get(i);
            AdmissionRequest req = admissionRequestRepository.findById(reqId)
                    .orElseThrow(() -> new IllegalArgumentException("Admission request not found: " + reqId));

            Bed bed = cleanBeds.get(i);
            bed.setStatus(BedStatus.EMPTY_ASSIGNED);
            bed.setCurrentPatient(req.getPatient());
            bedRepository.save(bed);

            req.setAssignedBed(bed);
            req.setStatus(AdmissionStatus.BED_ALLOCATED);
            req.setAllocatedAt(LocalDateTime.now());
            admissionRequestRepository.save(req);
            allocatedRequests.add(req);
        }

        if (!allocatedRequests.isEmpty()) {
            AdmissionRequest first = allocatedRequests.get(0);
            ward.setLockedGender(first.getPatient().getGender());
            ward.setLockedInfectionStatus(first.getPatient().getInfectionStatus() != null ? first.getPatient().getInfectionStatus() : InfectionStatus.NON_INFECTIOUS);
            ward.setHoldingWard(true);
            wardRepository.save(ward);
        }

        auditLogger.logAction(currentUser, "APPROVE_BATCH_HOLDING_WARD",
                "Ward:" + wardId,
                "Approved batch holding ward for " + reqIds.size() + " patients: " + reqIds);

        return allocatedRequests;
    }

    public List<CohortSwapSuggestion> getCohortSwapSuggestions() {
        List<Ward> allWards = wardRepository.findAll();
        List<CohortSwapSuggestion> suggestions = new ArrayList<>();

        for (Ward flexWard : allWards) {
            if (flexWard.getCapacity() < 3) {
                continue;
            }

            List<Bed> flexBeds = bedRepository.findByWard_Id(flexWard.getId());
            long emptyAssignedCount = flexBeds.stream().filter(b -> b.getStatus() == BedStatus.EMPTY_ASSIGNED).count();
            long emptyCleanedCount = flexBeds.stream().filter(b -> b.getStatus() == BedStatus.EMPTY_CLEANED).count();
            boolean hasOccupiedOrDirty = flexBeds.stream().anyMatch(b -> b.getStatus() == BedStatus.OCCUPIED_TAKEN || b.getStatus() == BedStatus.EMPTY_PENDING_CLEANING);

            // Flex ward is blocked if 1 or 2 beds are EMPTY_ASSIGNED, none are occupied/dirty, and rest are EMPTY_CLEANED
            if ((emptyAssignedCount == 1 || emptyAssignedCount == 2) && !hasOccupiedOrDirty && (emptyAssignedCount + emptyCleanedCount == flexBeds.size())) {
                List<Bed> blockedBeds = flexBeds.stream()
                        .filter(b -> b.getStatus() == BedStatus.EMPTY_ASSIGNED)
                        .collect(Collectors.toList());

                for (Bed blockedBed : blockedBeds) {
                    Optional<AdmissionRequest> reqOpt = admissionRequestRepository.findByAssignedBed_Id(blockedBed.getId());
                    if (reqOpt.isEmpty()) {
                        continue;
                    }
                    AdmissionRequest req = reqOpt.get();
                    if (req.getStatus() != AdmissionStatus.BED_ALLOCATED || !Boolean.TRUE.equals(req.getWaitingInEd())) {
                        continue;
                    }

                    Patient patient = req.getPatient();
                    if (patient == null) {
                        continue;
                    }

                    WardClass wardClass = req.getRequestedWardClass();
                    Gender gender = patient.getGender();
                    InfectionStatus infection = patient.getInfectionStatus() != null ? patient.getInfectionStatus() : InfectionStatus.NON_INFECTIOUS;
                    boolean needsTelemetry = Boolean.TRUE.equals(req.getEffectiveTelemetry()) || Boolean.TRUE.equals(req.getPrimaryTelemetry()) || patient.isNeedsTelemetry();

                    // Search for candidate partially occupied wards
                    for (Ward candidateWard : allWards) {
                        if (candidateWard.getId().equals(flexWard.getId()) || candidateWard.getWardClass() != wardClass) {
                            continue;
                        }
                        if (infection == InfectionStatus.RESPIRATORY && !candidateWard.isNegativePressure()) {
                            continue;
                        }
                        if (infection != InfectionStatus.RESPIRATORY && candidateWard.isNegativePressure()) {
                            continue;
                        }
                        if (candidateWard.getLockedGender() != null && candidateWard.getLockedGender() != gender) {
                            continue;
                        }

                        List<Bed> candidateBeds = bedRepository.findByWard_Id(candidateWard.getId());
                        boolean hasOccupied = candidateBeds.stream().anyMatch(b -> b.getStatus() == BedStatus.OCCUPIED_TAKEN || b.getStatus() == BedStatus.EMPTY_ASSIGNED);
                        if (!hasOccupied) {
                            continue; // Must be partially occupied, not another empty flex ward
                        }

                        Optional<Bed> candidateBedOpt = candidateBeds.stream()
                                .filter(b -> b.getStatus() == BedStatus.EMPTY_CLEANED)
                                .filter(b -> !needsTelemetry || b.isHasTelemetry())
                                .findFirst();

                        if (candidateBedOpt.isPresent()) {
                            Bed candidateBed = candidateBedOpt.get();
                            suggestions.add(CohortSwapSuggestion.builder()
                                    .suggestionId(UUID.randomUUID().toString())
                                    .admissionRequestId(req.getId())
                                    .patientId(patient.getId())
                                    .patientName(patient.getName())
                                    .patientGender(gender)
                                    .patientWardClass(wardClass)
                                    .currentBedId(blockedBed.getId())
                                    .currentBedNumber(blockedBed.getBedNumber())
                                    .currentWardId(flexWard.getId())
                                    .currentWardName(flexWard.getName())
                                    .targetBedId(candidateBed.getId())
                                    .targetBedNumber(candidateBed.getBedNumber())
                                    .targetWardId(candidateWard.getId())
                                    .targetWardName(candidateWard.getName())
                                    .unlockedCapacityCount(flexWard.getCapacity())
                                    .build());
                            break; // Found transfer bed for this blocked bed
                        }
                    }
                }
            }
        }

        return suggestions;
    }

    @Transactional
    public AdmissionRequest approveCohortSwap(CohortSwapApprovalRequest request) {
        String currentUser = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "bmu_coord";

        AdmissionRequest req = admissionRequestRepository.findById(request.getAdmissionRequestId())
                .orElseThrow(() -> new IllegalArgumentException("Admission request not found: " + request.getAdmissionRequestId()));

        if (!Boolean.TRUE.equals(req.getWaitingInEd())) {
            throw new IllegalStateException("Patient " + req.getPatient().getName() + " has already departed ED; cannot execute cohort swap.");
        }

        Bed targetBed = bedRepository.findById(request.getTargetBedId())
                .orElseThrow(() -> new IllegalArgumentException("Target bed not found: " + request.getTargetBedId()));

        if (targetBed.getStatus() != BedStatus.EMPTY_CLEANED) {
            throw new IllegalStateException("Target bed " + targetBed.getBedNumber() + " is no longer available (current status: " + targetBed.getStatus() + ").");
        }

        Bed flexBed = req.getAssignedBed();

        // Assign patient to target bed
        targetBed.setStatus(BedStatus.EMPTY_ASSIGNED);
        targetBed.setCurrentPatient(req.getPatient());
        bedRepository.save(targetBed);

        req.setAssignedBed(targetBed);
        admissionRequestRepository.save(req);

        // Clear flex bed
        if (flexBed != null) {
            flexBed.setStatus(BedStatus.EMPTY_CLEANED);
            flexBed.setCurrentPatient(null);
            bedRepository.save(flexBed);

            Ward flexWard = flexBed.getWard();
            if (flexWard != null) {
                List<Bed> flexBeds = bedRepository.findByWard_Id(flexWard.getId());
                boolean allClean = flexBeds.stream().allMatch(b -> b.getStatus() == BedStatus.EMPTY_CLEANED);
                if (allClean) {
                    flexWard.setLockedGender(null);
                    flexWard.setLockedInfectionStatus(null);
                    flexWard.setHoldingWard(false);
                    wardRepository.save(flexWard);
                }
            }
        }

        auditLogger.logAction(currentUser, "APPROVE_COHORT_SWAP",
                "AdmissionRequest:" + req.getId(),
                "Reallocated patient " + req.getPatient().getName() + " from flex bed "
                        + (flexBed != null ? flexBed.getBedNumber() : "none")
                        + " to target bed " + targetBed.getBedNumber()
                        + " liberating ward " + (flexBed != null && flexBed.getWard() != null ? flexBed.getWard().getName() : ""));

        return req;
    }

    @Transactional
    public AdmissionRequest attachDelayTag(UUID admissionRequestId, DelayTagRequest delayTagRequest) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isBmu = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_BMU_COORDINATOR"));
        if (!isBmu) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only BMU coordinators possess operational authority to tag delay reasons.");
        }

        String currentUser = auth != null ? auth.getName() : "bmu_coord_wong";

        AdmissionRequest request = admissionRequestRepository.findById(admissionRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Admission request not found: " + admissionRequestId));

        long dwellMinutes = 0;
        if (request.getRequestedAt() != null) {
            dwellMinutes = java.time.Duration.between(request.getRequestedAt(), LocalDateTime.now()).toMinutes();
        }

        AcuityTier effectiveAcuity = request.getEffectiveAcuityTier() != null
                ? request.getEffectiveAcuityTier()
                : request.getPrimaryAcuityTier();

        request.setDelayReasonTag(delayTagRequest.getDelayReasonCode().name());
        request.setOperationalDelayReason(delayTagRequest.getEffectiveNote());

        AdmissionRequest saved = admissionRequestRepository.save(request);

        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
        details.put("Coordinator", currentUser);
        details.put("DelayCode", delayTagRequest.getDelayReasonCode().name());
        details.put("DwellMinutes", dwellMinutes);
        details.put("AcuityTier", effectiveAcuity);
        details.put("Note", delayTagRequest.getEffectiveNote());

        auditLogger.logAction(currentUser, "TAG_DELAY_REASON",
                "AdmissionRequest:" + admissionRequestId,
                AuditLogger.formatDetails(details));

        return saved;
    }
}

