package com.incident.management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIncidentRequest(
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 500, message = "제목은 500자 이하여야 합니다")
        String title,

        String description,

        String status,

        String priority,

        @Size(max = 100)
        String reporterName,

        @Size(max = 100)
        String assigneeName
) {}
