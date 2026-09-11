package com.hospital.admissions.dto;

import com.hospital.admissions.entity.DelayReasonCode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for tagging an admission request with an operational delay reason code and notes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelayTagRequest {

    @NotNull(message = "Delay reason code is mandatory")
    private DelayReasonCode delayReasonCode;

    private String note;
    private String operationalDelayReason;

    /**
     * Resolves the effective explanatory note, prioritizing explicit notes over operational delay notes.
     *
     * @return effective delay note string
     */
    public String getEffectiveNote() {
        if (note != null && !note.isBlank()) {
            return note;
        }
        return operationalDelayReason;
    }
}
