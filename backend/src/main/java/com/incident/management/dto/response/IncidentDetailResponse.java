package com.incident.management.dto.response;

import com.incident.management.entity.Incident;

import java.time.LocalDateTime;
import java.util.List;

public record IncidentDetailResponse(
        Long id,
        String title,
        String description,
        String status,
        String priority,
        String reporterName,
        String assigneeName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt,
        List<IncidentActionResponse> actions,
        List<DeploymentPlanResponse> deploymentPlans,
        List<IncidentDocumentResponse> documents
) {
    public static IncidentDetailResponse from(Incident incident) {
        return new IncidentDetailResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getStatus().name(),
                incident.getPriority().name(),
                incident.getReporterName(),
                incident.getAssigneeName(),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                incident.getResolvedAt(),
                incident.getActions().stream().map(IncidentActionResponse::from).toList(),
                incident.getDeploymentPlans().stream().map(DeploymentPlanResponse::from).toList(),
                incident.getDocuments().stream().map(IncidentDocumentResponse::from).toList()
        );
    }
}
