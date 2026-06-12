package com.incident.management.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateIncidentRequest(
        @Size(max = 500)
        String title,

        String description,

        String status,

        String priority,

        @Size(max = 100)
        String reporterName,

        @Size(max = 100)
        String assigneeName
) {}
