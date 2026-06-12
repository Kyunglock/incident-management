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
    public static IncidentDetailResponse from(Incident incident, List<IncidentActionResponse> actions) {
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
                actions,
                List.of(),
                List.of()
        );
    }
}
