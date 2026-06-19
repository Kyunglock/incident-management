package com.incident.management.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class IncidentResponse {
    private Long id;
    private Long releaseHistoryId;
    /** 소속 SR 컨텍스트 (전역 목록 표시용) */
    private String srNumber;
    private String service;
    private Long releasePlanId;
    private LocalDateTime occurredAt;
    private String symptom;
    private LocalDateTime createdAt;
}
