package com.incident.management.dto.excel;

/**
 * 엑셀 "시스템 반영 작업 요청" 시트의 한 행(= SR 1건)에서 추출한 값.
 * 필요한 컬럼(서비스/작업내용/요청자/작업자/TEST URL/TEST 상세/Frontend/Backend/비고/최종확인)만 담는다.
 */
public record ParsedSrRow(
        String service,
        String workContent,
        String requester,
        String worker,
        String testUrlVerify,
        String testUrlProd,
        String testDetail,
        Boolean frontendChanged,
        Boolean backendChanged,
        String note,
        Boolean finalConfirmed
) {
}
