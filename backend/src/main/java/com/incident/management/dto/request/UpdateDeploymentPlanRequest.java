package com.incident.management.dto.request;

public record UpdateDeploymentPlanRequest(
        String planContent,
        String submittedBy
) {}
