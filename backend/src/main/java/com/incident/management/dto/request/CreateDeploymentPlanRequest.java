package com.incident.management.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateDeploymentPlanRequest(
        @NotBlank String planContent,
        String submittedBy
) {}
