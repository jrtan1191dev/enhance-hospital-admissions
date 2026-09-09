package com.hospital.admissions.web;

import com.hospital.admissions.domain.AdmissionRequest;
import com.hospital.admissions.domain.BmuAlgorithmConfig;
import com.hospital.admissions.domain.Ward;
import com.hospital.admissions.dto.BedAllocationRequest;
import com.hospital.admissions.dto.BedRecommendation;
import com.hospital.admissions.dto.BmuConfigUpdateRequest;
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

    @GetMapping("/inventory")
    public ResponseEntity<List<Ward>> getInventory() {
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
}
