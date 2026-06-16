package com.incident.management.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IncidentResponse {
    private Long id;
    private Long releaseHistoryId;
    private LocalDateTime occurredAt;
    private String symptom;
    private LocalDateTime createdAt;
}
