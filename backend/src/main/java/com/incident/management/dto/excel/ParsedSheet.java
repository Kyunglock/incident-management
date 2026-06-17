package com.incident.management.dto.excel;

import java.time.LocalDate;
import java.util.List;

/**
 * 엑셀 시트 1개를 파싱한 결과. 시트명은 MM.DD 형식이며 연도는 2026으로 고정해 날짜를 구성한다.
 *
 * @param sheetName 원본 시트명 (예: "01.13")
 * @param date      시트명으로부터 해석한 반영 날짜 (연도 2026 고정), 형식이 잘못되면 null
 * @param rows      해당 시트의 SR 행 목록 (= 반영 이력)
 */
public record ParsedSheet(
        String sheetName,
        LocalDate date,
        List<ParsedSrRow> rows
) {
}
