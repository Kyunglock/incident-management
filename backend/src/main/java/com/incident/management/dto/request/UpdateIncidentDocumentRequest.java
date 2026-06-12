package com.incident.management.dto.request;

public record UpdateIncidentDocumentRequest(
        String symptom,
        String rootCause,
        String actionTaken,
        String deploymentSummary,
        String result,
        String reviewedBy
) {}
