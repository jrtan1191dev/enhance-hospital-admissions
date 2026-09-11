package com.hospital.admissions.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request payload for appending follow-up clinical or social notes to an admission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpNoteRequest {
    @NotBlank
    private String notes;
}
