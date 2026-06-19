package com.incident.management.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** SR(반영 이력)에 연동된 사이드이펙트 검토 결과. content 는 LLM 추론 텍스트. */
@Data
@Builder
public class SideEffectReportResponse {
    private boolean exists;
    private String content;     // LLM 추론 텍스트 (git diff 기반 사이드이펙트 분석)
    private LocalDateTime createdAt;
}
