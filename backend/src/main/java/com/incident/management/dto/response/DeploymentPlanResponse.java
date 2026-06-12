package com.incident.management.dto.response;

import com.incident.management.entity.DeploymentPlan;

import java.time.LocalDateTime;

public record DeploymentPlanResponse(
        Long id,
        Long incidentId,
        String planContent,
        String status,
        String submittedBy,
        String approvedBy,
        LocalDateTime submittedAt,
        LocalDateTime approvedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DeploymentPlanResponse from(DeploymentPlan plan) {
        return new DeploymentPlanResponse(
                plan.getId(),
                plan.getIncident().getId(),
                plan.getPlanContent(),
                plan.getStatus().name(),
                plan.getSubmittedBy(),
                plan.getApprovedBy(),
                plan.getSubmittedAt(),
                plan.getApprovedAt(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }
}
