package com.incident.management.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateIncidentStatusRequest(
        @NotBlank(message = "상태값은 필수입니다")
        String status
) {}
