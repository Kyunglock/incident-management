package com.incident.management.dto.response;

import com.incident.management.entity.IncidentAction;

import java.time.LocalDateTime;

public record IncidentActionResponse(
        Long id,
        Long incidentId,
        String actionDescription,
        String actionType,
        String performedBy,
        LocalDateTime performedAt,
        LocalDateTime createdAt
) {
    public static IncidentActionResponse from(IncidentAction action) {
        return new IncidentActionResponse(
                action.getId(),
                action.getIncident().getId(),
                action.getActionDescription(),
                action.getActionType().name(),
                action.getPerformedBy(),
                action.getPerformedAt(),
                action.getCreatedAt()
        );
    }
}
