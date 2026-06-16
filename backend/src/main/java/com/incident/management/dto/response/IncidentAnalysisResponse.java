package com.incident.management.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IncidentAnalysisResponse {
    private Long id;
    private String symptom;
    private String cause;
    private String docPath;
    private LocalDateTime occurredAt;
}
