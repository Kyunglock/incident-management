package com.incident.management.dto.response;

import com.incident.management.entity.IncidentDocument;

import java.time.LocalDateTime;

public record IncidentDocumentResponse(
        Long id,
        Long incidentId,
        String symptom,
        String rootCause,
        String actionTaken,
        String deploymentSummary,
        String result,
        Boolean isAiGenerated,
        String reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static IncidentDocumentResponse from(IncidentDocument doc) {
        return new IncidentDocumentResponse(
                doc.getId(),
                doc.getIncident().getId(),
                doc.getSymptom(),
                doc.getRootCause(),
                doc.getActionTaken(),
                doc.getDeploymentSummary(),
                doc.getResult(),
                doc.getIsAiGenerated(),
                doc.getReviewedBy(),
                doc.getReviewedAt(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
