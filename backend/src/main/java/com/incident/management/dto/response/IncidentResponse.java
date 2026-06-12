package com.incident.management.dto.response;

import com.incident.management.entity.Incident;

import java.time.LocalDateTime;

public record IncidentResponse(
        Long id,
        String title,
        String description,
        String status,
        String priority,
        String reporterName,
        String assigneeName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt
) {
    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getStatus().name(),
                incident.getPriority().name(),
                incident.getReporterName(),
                incident.getAssigneeName(),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                incident.getResolvedAt()
        );
    }
}
