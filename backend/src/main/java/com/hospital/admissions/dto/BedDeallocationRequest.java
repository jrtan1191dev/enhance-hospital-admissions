package com.hospital.admissions.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Request payload for deallocating or reverting an assigned bed back to the unallocated queue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BedDeallocationRequest {
    @NotNull
    private UUID admissionRequestId;
}
