package com.incident.management.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 다중 시트 엑셀 업로드 결과 요약.
 * 시트(날짜)별로 반영 계획서를 생성하며, 이미 같은 날짜(제목)가 존재하면 건너뛴다.
 */
@Data
@Builder
public class ReleasePlanImportResponse {

    /** 엑셀의 전체 시트 수 */
    private int totalSheets;

    /** 새로 생성한 반영 계획서 목록 */
    private List<Created> created;

    /** 이미 DB에 존재해 건너뛴 날짜(제목) 목록 */
    private List<String> skippedExisting;

    /** 날짜 형식이 잘못되었거나 SR 행이 없어 건너뛴 시트명 목록 */
    private List<String> skippedEmptyOrInvalid;

    @Data
    @Builder
    public static class Created {
        private Long planId;
        private String title;
        /** 생성된 반영 이력(SR) 수 */
        private int historyCount;
    }
}
