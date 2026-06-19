package com.incident.management.dto;

import java.util.List;

/**
 * 작업계획서(시스템 작업(적용) 계획서) docx 렌더링용 데이터.
 * 반영이력에서 채울 수 있는 항목만 담고, 고정 문구/빈 항목은 렌더러에서 처리한다.
 */
public record WorkPlanData(
        String taskName,        // 작업명
        String taskDate,        // 작업예정일
        String targetServer,    // 작업대상 서버 (데이터에 없으면 빈칸)
        String requesters,      // 요청자 (반영이력 집계)
        String restart,         // 재기동 여부
        List<ServiceWork> services,  // 서비스별 세부 작업내용
        String workers,         // 작업자 (반영이력 집계)
        String manager,         // 관리자 (데이터에 없으면 빈칸)
        String author,          // 작성자 (빈칸)
        String createdDate      // 작성일
) {
    /** 서비스 1개 + 그 서비스의 작업 항목들 */
    public record ServiceWork(String service, List<String> items) {}
}
