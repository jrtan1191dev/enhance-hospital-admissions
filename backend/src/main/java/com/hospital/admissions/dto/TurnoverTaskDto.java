package com.hospital.admissions.dto;

import com.hospital.admissions.domain.TurnoverSlaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO representing an active bed turnover cleaning task for Environmental Services (EVS).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnoverTaskDto {
    private UUID bedId;
    private String bedNumber;
    private String wardCode;
    private Integer levelNumber;
    private LocalDateTime vacatedAt;
    private LocalDateTime cleaningStartedAt;
    private long remainingMinutes;
    private TurnoverSlaStatus slaStatus;
}
