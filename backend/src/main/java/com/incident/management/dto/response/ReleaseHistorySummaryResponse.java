package com.incident.management.dto.response;

import lombok.Builder;
import lombok.Data;

/** SR(반영 이력) 선택용 경량 요약 (장애 등록 시 드롭다운 등). */
@Data
@Builder
public class ReleaseHistorySummaryResponse {
    private Long id;
    private String srNumber;
    private String service;
    private Long releasePlanId;
}
