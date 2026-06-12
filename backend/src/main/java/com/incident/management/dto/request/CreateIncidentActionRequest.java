package com.incident.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateIncidentActionRequest(
        @NotBlank(message = "조치 내용은 필수입니다")
        String actionDescription,

        @NotNull(message = "조치 유형은 필수입니다")
        String actionType,

        String performedBy,

        LocalDateTime performedAt
) {}
