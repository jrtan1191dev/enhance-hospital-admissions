package com.hospital.admissions.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpNoteRequest {
    @NotBlank
    private String notes;
}
