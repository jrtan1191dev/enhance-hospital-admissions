package com.hospital.admissions.controller;

import com.hospital.admissions.entity.AdmissionRequest;
import com.hospital.admissions.entity.Bed;
import com.hospital.admissions.entity.BmuAlgorithmConfig;
import com.hospital.admissions.dto.*;
import com.hospital.admissions.service.BmuService;
import com.hospital.admissions.service.WardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Bed Management Unit (BMU) operations.
 * <p>
 * Exposes endpoints for prioritized admission queues, bed recommendation scoring,
 * bed allocation and override management, ward inventory forecasting, algorithm parameter tuning,
 * sister hospital diversion workflows, batch holding ward conversions, and cohort swap approvals.
 */
@RestController
@RequestMapping("/api/v1/bmu")
@RequiredArgsConstructor
public class BmuController {

    private final BmuService bmuService;
    private final WardService wardService;

    /**
     * Retrieves the aggregate bed capacity forecast across all wards for 24, 48, and 72-hour horizons.
     *
     * @return {@link ResponseEntity} containing {@link BmuCapacityForecastDto} forecast statistics.
     */
    @GetMapping("/capacity-forecast")
    public ResponseEntity<BmuCapacityForecastDto> getCapacityForecast() {
        return ResponseEntity.ok(wardService.getCapacityForecast());
    }

    /**
     * Retrieves the prioritized queue of pending admission requests sorted by clinical urgency and FIFO order.
     *
     * @return {@link ResponseEntity} containing a list of prioritized {@link AdmissionRequest} entities.
     */
    @GetMapping("/queue")
    public ResponseEntity<List<AdmissionRequest>> getPrioritizedQueue() {
        return ResponseEntity.ok(bmuService.getPrioritizedQueue());
    }

    /**
     * Calculates ranked bed recommendations for a specific admission request using constraint optimization.
     *
     * @param requestId the unique identifier of the {@link AdmissionRequest}.
     * @return {@link ResponseEntity} containing a list of scored {@link BedRecommendation} candidates.
     */
    @GetMapping("/recommendations/{requestId}")
    public ResponseEntity<List<BedRecommendation>> getRecommendations(@PathVariable UUID requestId) {
        return ResponseEntity.ok(bmuService.getRecommendations(requestId));
    }

    /**
     * Allocates a bed to an admission request, validating safety invariants and optional overrides.
     *
     * @param request the {@link BedAllocationRequest} containing request ID, target bed ID, and optional override metadata.
     * @return {@link ResponseEntity} containing the updated {@link AdmissionRequest}.
     */
    @PostMapping("/allocate")
    public ResponseEntity<AdmissionRequest> allocateBed(@Valid @RequestBody BedAllocationRequest request) {
        if (request.getOverrideReason() != null || request.getRank() != null || request.getScore() != null) {
            return ResponseEntity.ok(bmuService.allocateBed(
                    request.getAdmissionRequestId(),
                    request.getBedId(),
                    request.getRank(),
                    request.getScore(),
                    request.getOverrideReason()
            ));
        }
        return ResponseEntity.ok(bmuService.allocateBed(request.getAdmissionRequestId(), request.getBedId()));
    }

    /**
     * Overrides automated allocation rules with clinician/coordinator clinical justification.
     *
     * @param request the {@link BedAllocationRequest} specifying the override reason and score details.
     * @return {@link ResponseEntity} containing the updated {@link AdmissionRequest}.
     */
    @PostMapping("/allocations/override")
    public ResponseEntity<AdmissionRequest> overrideAllocation(@Valid @RequestBody BedAllocationRequest request) {
        return ResponseEntity.ok(bmuService.allocateBed(
                request.getAdmissionRequestId(),
                request.getBedId(),
                request.getRank(),
                request.getScore(),
                request.getOverrideReason()
        ));
    }

    /**
     * Allocates a specific bed for a given admission request ID specified in the path variable.
     *
     * @param requestId the unique identifier of the admission request.
     * @param request   the {@link BedAllocationRequest} specifying target bed ID and override metadata.
     * @return {@link ResponseEntity} containing the updated {@link AdmissionRequest}.
     */
    @PostMapping("/requests/{requestId}/allocate")
    public ResponseEntity<AdmissionRequest> allocateBedForRequest(
            @PathVariable UUID requestId,
            @RequestBody BedAllocationRequest request) {
        return ResponseEntity.ok(bmuService.allocateBed(
                requestId,
                request.getBedId(),
                request.getRank(),
                request.getScore(),
                request.getOverrideReason()
        ));
    }

    /**
     * Retrieves the real-time ward and bed inventory status across all inpatient units.
     *
     * @return {@link ResponseEntity} containing a list of {@link WardDto} objects with bed occupancy details.
     */
    @GetMapping("/inventory")
    public ResponseEntity<List<WardDto>> getInventory() {
        return ResponseEntity.ok(bmuService.getInventory());
    }

    /**
     * Retrieves the current BMU optimization algorithm weightings and operational parameters.
     *
     * @return {@link ResponseEntity} containing {@link BmuAlgorithmConfig}.
     */
    @GetMapping("/config")
    public ResponseEntity<BmuAlgorithmConfig> getConfig() {
        return ResponseEntity.ok(bmuService.getOrCreateConfig());
    }

    /**
     * Updates the BMU optimization algorithm configuration and scoring weights.
     *
     * @param request the {@link BmuConfigUpdateRequest} containing new weights and thresholds.
     * @return {@link ResponseEntity} containing the updated {@link BmuAlgorithmConfig}.
     */
    @PutMapping("/config")
    public ResponseEntity<BmuAlgorithmConfig> updateConfig(@RequestBody BmuConfigUpdateRequest request) {
        return ResponseEntity.ok(bmuService.updateConfig(request));
    }

    /**
     * Initiates a fast-track diversion referral to an external sister or community hospital.
     *
     * @param request the {@link DiversionReferralRequest} detailing the admission request and destination facility.
     * @return {@link ResponseEntity} containing the {@link SisterHospitalReferralResponse} confirmation.
     */
    @PostMapping("/diversion/refer")
    public ResponseEntity<SisterHospitalReferralResponse> referToSisterHospital(
            @Valid @RequestBody DiversionReferralRequest request) {
        return ResponseEntity
                .ok(bmuService.referToSisterHospital(request.getAdmissionRequestId(), request.getFacility()));
    }

    /**
     * Triggers clinical reconciliation for an admission request when ED and specialist recommendations conflict.
     *
     * @param id the unique identifier of the admission request.
     * @return {@link ResponseEntity} containing the updated {@link AdmissionRequest}.
     */
    @PostMapping("/requests/{id}/reconcile")
    public ResponseEntity<AdmissionRequest> requestReconciliation(@PathVariable UUID id) {
        return ResponseEntity.ok(bmuService.requestReconciliation(id));
    }

    /**
     * Manually assigns or overrides the admitting specialty cluster for an admission request.
     *
     * @param id      the unique identifier of the admission request.
     * @param request the {@link AdmittingClusterRequest} containing the assigned specialty cluster.
     * @return {@link ResponseEntity} containing the updated {@link AdmissionRequest}.
     */
    @PostMapping("/requests/{id}/admitting-cluster")
    public ResponseEntity<AdmissionRequest> assignAdmittingCluster(
            @PathVariable UUID id,
            @Valid @RequestBody AdmittingClusterRequest request) {
        return ResponseEntity.ok(bmuService.assignAdmittingCluster(id, request.getAdmittingSpecialtyCluster()));
    }

    /**
     * Deallocates a previously assigned bed, returning the admission request to the pending queue.
     *
     * @param request the {@link BedDeallocationRequest} containing the admission request ID to deallocate.
     * @return {@link ResponseEntity} containing the updated {@link AdmissionRequest}.
     */
    @PostMapping("/deallocate")
    public ResponseEntity<AdmissionRequest> deallocateBed(@Valid @RequestBody BedDeallocationRequest request) {
        return ResponseEntity.ok(bmuService.deallocateBed(request.getAdmissionRequestId()));
    }

    /**
     * Confirms that a patient has physically arrived at their allocated bed, updating status to OCCUPIED.
     *
     * @param bedId the unique identifier of the target bed.
     * @return {@link ResponseEntity} containing the updated {@link Bed} entity.
     */
    @PostMapping("/beds/{bedId}/arrive")
    public ResponseEntity<Bed> confirmArrival(@PathVariable UUID bedId) {
        return ResponseEntity.ok(bmuService.confirmArrival(bedId));
    }

    /**
     * Vacates an occupied bed upon patient discharge or transfer, setting status to EMPTY_PENDING_CLEANING.
     *
     * @param bedId the unique identifier of the bed being vacated.
     * @return {@link ResponseEntity} containing the vacated {@link Bed} entity.
     */
    @PostMapping("/beds/{bedId}/vacate")
    public ResponseEntity<Bed> vacateBed(@PathVariable UUID bedId) {
        return ResponseEntity.ok(bmuService.vacateBed(bedId));
    }

    /**
     * Signs off terminal sanitization for a vacated bed, making it available as EMPTY_CLEANED.
     *
     * @param bedId the unique identifier of the cleaned bed.
     * @return {@link ResponseEntity} containing the cleaned {@link Bed} entity.
     */
    @PostMapping("/beds/{bedId}/clean")
    public ResponseEntity<Bed> cleanBed(@PathVariable UUID bedId) {
        return ResponseEntity.ok(bmuService.signOffCleaning(bedId));
    }

    /**
     * Retrieves AI/solver-generated batch placement suggestions for temporary holding ward conversion.
     *
     * @return {@link ResponseEntity} containing a list of {@link BatchSuggestion} recommendations.
     */
    @GetMapping("/batch-suggestions")
    public ResponseEntity<List<BatchSuggestion>> getBatchSuggestions() {
        return ResponseEntity.ok(bmuService.getBatchSuggestions());
    }

    /**
     * Approves the batch assignment of multiple pending patients into a designated holding ward.
     *
     * @param request the {@link BatchApprovalRequest} detailing the target ward and admitted patient requests.
     * @return {@link ResponseEntity} containing the list of updated {@link AdmissionRequest} instances.
     */
    @PostMapping("/batch-holding-wards/approve")
    public ResponseEntity<List<AdmissionRequest>> approveBatchHoldingWard(
            @Valid @RequestBody BatchApprovalRequest request) {
        return ResponseEntity.ok(bmuService.approveBatchHoldingWard(request));
    }

    /**
     * Retrieves suggested lateral cohort bed swaps to resolve gender mismatches or unlock capacity.
     *
     * @return {@link ResponseEntity} containing a list of {@link CohortSwapSuggestion} pairs.
     */
    @GetMapping("/cohort-swap-suggestions")
    public ResponseEntity<List<CohortSwapSuggestion>> getCohortSwapSuggestions() {
        return ResponseEntity.ok(bmuService.getCohortSwapSuggestions());
    }

    /**
     * Approves and executes a suggested lateral cohort bed swap between two patients.
     *
     * @param request the {@link CohortSwapApprovalRequest} specifying the swap details.
     * @return {@link ResponseEntity} containing the resulting {@link AdmissionRequest}.
     */
    @PostMapping("/cohort-swap/approve")
    public ResponseEntity<AdmissionRequest> approveCohortSwap(
            @Valid @RequestBody CohortSwapApprovalRequest request) {
        return ResponseEntity.ok(bmuService.approveCohortSwap(request));
    }

    /**
     * Recalls a diverted patient back to the acute inpatient admission queue upon condition escalation.
     *
     * @param id the unique identifier of the diverted admission request.
     * @return {@link ResponseEntity} containing the recalled {@link AdmissionRequest}.
     */
    @PostMapping("/diversion/{id}/recall")
    public ResponseEntity<AdmissionRequest> recallDiversion(@PathVariable UUID id) {
        return ResponseEntity.ok(bmuService.recallDiversionToAcuteQueue(id));
    }

    /**
     * Extends the referral SLA timer for a pending diversion referral to a community hospital.
     *
     * @param id the unique identifier of the admission request.
     * @return {@link ResponseEntity} containing the updated {@link AdmissionRequest}.
     */
    @PostMapping("/diversion/{id}/extend-sla")
    public ResponseEntity<AdmissionRequest> extendDiversionSla(@PathVariable UUID id) {
        return ResponseEntity.ok(bmuService.extendDiversionSla(id));
    }

    /**
     * Logs a telephone follow-up note for a patient receiving subacute or hospital-at-home diversion care.
     *
     * @param id      the unique identifier of the admission request.
     * @param request the {@link FollowUpNoteRequest} containing the clinician's notes.
     * @return {@link ResponseEntity} containing the updated {@link AdmissionRequest}.
     */
    @PostMapping("/diversion/{id}/follow-up")
    public ResponseEntity<AdmissionRequest> logTelephoneFollowUp(
            @PathVariable UUID id,
            @Valid @RequestBody FollowUpNoteRequest request) {
        return ResponseEntity.ok(bmuService.logTelephoneFollowUp(id, request.getNotes()));
    }

    /**
     * Attaches an operational delay tag and reason code to an admission request.
     *
     * @param id      the unique identifier of the admission request.
     * @param request the {@link DelayTagRequest} specifying delay reason code and narrative explanation.
     * @return {@link ResponseEntity} containing the updated {@link AdmissionRequest}.
     */
    @PostMapping("/requests/{id}/delay-tag")
    public ResponseEntity<AdmissionRequest> attachDelayTag(
            @PathVariable UUID id,
            @Valid @RequestBody DelayTagRequest request) {
        return ResponseEntity.ok(bmuService.attachDelayTag(id, request));
    }
}
