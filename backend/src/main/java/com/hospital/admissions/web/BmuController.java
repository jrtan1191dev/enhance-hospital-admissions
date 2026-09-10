package com.hospital.admissions.web;

import com.hospital.admissions.domain.AdmissionRequest;
import com.hospital.admissions.domain.BmuAlgorithmConfig;
import com.hospital.admissions.dto.WardDto;
import com.hospital.admissions.dto.BedAllocationRequest;
import com.hospital.admissions.dto.BedRecommendation;
import com.hospital.admissions.dto.BmuConfigUpdateRequest;
import com.hospital.admissions.dto.DelayTagRequest;
import com.hospital.admissions.dto.DiversionReferralRequest;
import com.hospital.admissions.dto.SisterHospitalReferralResponse;
import com.hospital.admissions.service.BmuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bmu")
@RequiredArgsConstructor
public class BmuController {

    private final BmuService bmuService;
    private final com.hospital.admissions.service.WardService wardService;

    @GetMapping("/capacity-forecast")
    public ResponseEntity<com.hospital.admissions.dto.BmuCapacityForecastDto> getCapacityForecast() {
        return ResponseEntity.ok(wardService.getCapacityForecast());
    }

    @GetMapping("/queue")
    public ResponseEntity<List<AdmissionRequest>> getPrioritizedQueue() {
        return ResponseEntity.ok(bmuService.getPrioritizedQueue());
    }

    @GetMapping("/recommendations/{requestId}")
    public ResponseEntity<List<BedRecommendation>> getRecommendations(@PathVariable UUID requestId) {
        return ResponseEntity.ok(bmuService.getRecommendations(requestId));
    }

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

    @GetMapping("/inventory")
    public ResponseEntity<List<WardDto>> getInventory() {
        return ResponseEntity.ok(bmuService.getInventory());
    }

    @GetMapping("/config")
    public ResponseEntity<BmuAlgorithmConfig> getConfig() {
        return ResponseEntity.ok(bmuService.getOrCreateConfig());
    }

    @PutMapping("/config")
    public ResponseEntity<BmuAlgorithmConfig> updateConfig(@RequestBody BmuConfigUpdateRequest request) {
        return ResponseEntity.ok(bmuService.updateConfig(request));
    }

    @PostMapping("/diversion/refer")
    public ResponseEntity<SisterHospitalReferralResponse> referToSisterHospital(
            @Valid @RequestBody DiversionReferralRequest request) {
        return ResponseEntity
                .ok(bmuService.referToSisterHospital(request.getAdmissionRequestId(), request.getFacility()));
    }

    @PostMapping("/requests/{id}/reconcile")
    public ResponseEntity<AdmissionRequest> requestReconciliation(@PathVariable UUID id) {
        return ResponseEntity.ok(bmuService.requestReconciliation(id));
    }

    @PostMapping("/requests/{id}/admitting-cluster")
    public ResponseEntity<AdmissionRequest> assignAdmittingCluster(
            @PathVariable UUID id,
            @Valid @RequestBody com.hospital.admissions.dto.AdmittingClusterRequest request) {
        return ResponseEntity.ok(bmuService.assignAdmittingCluster(id, request.getAdmittingSpecialtyCluster()));
    }

    @PostMapping("/deallocate")
    public ResponseEntity<AdmissionRequest> deallocateBed(@Valid @RequestBody com.hospital.admissions.dto.BedDeallocationRequest request) {
        return ResponseEntity.ok(bmuService.deallocateBed(request.getAdmissionRequestId()));
    }

    @PostMapping("/beds/{bedId}/arrive")
    public ResponseEntity<com.hospital.admissions.domain.Bed> confirmArrival(@PathVariable UUID bedId) {
        return ResponseEntity.ok(bmuService.confirmArrival(bedId));
    }

    @PostMapping("/beds/{bedId}/vacate")
    public ResponseEntity<com.hospital.admissions.domain.Bed> vacateBed(@PathVariable UUID bedId) {
        return ResponseEntity.ok(bmuService.vacateBed(bedId));
    }

    @PostMapping("/beds/{bedId}/clean")
    public ResponseEntity<com.hospital.admissions.domain.Bed> cleanBed(@PathVariable UUID bedId) {
        return ResponseEntity.ok(bmuService.signOffCleaning(bedId));
    }

    @GetMapping("/batch-suggestions")
    public ResponseEntity<List<com.hospital.admissions.dto.BatchSuggestion>> getBatchSuggestions() {
        return ResponseEntity.ok(bmuService.getBatchSuggestions());
    }

    @PostMapping("/batch-holding-wards/approve")
    public ResponseEntity<List<AdmissionRequest>> approveBatchHoldingWard(
            @Valid @RequestBody com.hospital.admissions.dto.BatchApprovalRequest request) {
        return ResponseEntity.ok(bmuService.approveBatchHoldingWard(request));
    }

    @GetMapping("/cohort-swap-suggestions")
    public ResponseEntity<List<com.hospital.admissions.dto.CohortSwapSuggestion>> getCohortSwapSuggestions() {
        return ResponseEntity.ok(bmuService.getCohortSwapSuggestions());
    }

    @PostMapping("/cohort-swap/approve")
    public ResponseEntity<AdmissionRequest> approveCohortSwap(
            @Valid @RequestBody com.hospital.admissions.dto.CohortSwapApprovalRequest request) {
        return ResponseEntity.ok(bmuService.approveCohortSwap(request));
    }

    @PostMapping("/diversion/{id}/recall")
    public ResponseEntity<AdmissionRequest> recallDiversion(@PathVariable UUID id) {
        return ResponseEntity.ok(bmuService.recallDiversionToAcuteQueue(id));
    }

    @PostMapping("/diversion/{id}/extend-sla")
    public ResponseEntity<AdmissionRequest> extendDiversionSla(@PathVariable UUID id) {
        return ResponseEntity.ok(bmuService.extendDiversionSla(id));
    }

    @PostMapping("/diversion/{id}/follow-up")
    public ResponseEntity<AdmissionRequest> logTelephoneFollowUp(
            @PathVariable UUID id,
            @Valid @RequestBody com.hospital.admissions.dto.FollowUpNoteRequest request) {
        return ResponseEntity.ok(bmuService.logTelephoneFollowUp(id, request.getNotes()));
    }

    @PostMapping("/requests/{id}/delay-tag")
    public ResponseEntity<AdmissionRequest> attachDelayTag(
            @PathVariable UUID id,
            @Valid @RequestBody DelayTagRequest request) {
        return ResponseEntity.ok(bmuService.attachDelayTag(id, request));
    }
}
