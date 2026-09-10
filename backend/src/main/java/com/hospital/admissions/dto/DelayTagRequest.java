package com.hospital.admissions.dto;

import com.hospital.admissions.entity.DelayReasonCode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelayTagRequest {

    @NotNull(message = "Delay reason code is mandatory")
    private DelayReasonCode delayReasonCode;

    private String note;
    private String operationalDelayReason;

    public String getEffectiveNote() {
        if (note != null && !note.isBlank()) {
            return note;
        }
        return operationalDelayReason;
    }
}
