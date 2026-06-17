package com.incident.management.common;

import com.incident.management.dto.excel.ParsedSheet;
import com.incident.management.dto.excel.ParsedSrRow;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ExcelParser {

    /** 연도는 2026으로 고정한다. */
    private static final int FIXED_YEAR = 2026;

    // "시스템 반영 작업 요청" 시트의 컬럼 인덱스 (0-based)
    private static final int COL_NO = 1;            // B
    private static final int COL_SERVICE = 2;       // C 서비스
    private static final int COL_WORK_CONTENT = 3;  // D 작업내용
    private static final int COL_REQUESTER = 8;     // I 요청자
    private static final int COL_WORKER = 9;        // J 작업자
    private static final int COL_TEST_URL = 10;     // K TEST URL
    private static final int COL_TEST_DETAIL = 12;  // M TEST 상세
    private static final int COL_FRONTEND = 16;     // Q Frontend
    private static final int COL_BACKEND = 17;      // R Backend
    private static final int COL_NOTE = 18;         // S 비고
    private static final int COL_FINAL_CONFIRM = 21;// V 최종확인

    /**
     * 워크북의 모든 시트를 파싱한다. 시트명(MM.DD)을 2026-MM-DD 날짜로 해석하고,
     * 각 시트의 SR 행(반영 이력)을 추출한다. 날짜 형식이 잘못된 시트는 date=null 로 반환한다.
     */
    public List<ParsedSheet> parseAllSheets(MultipartFile file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            List<ParsedSheet> result = new ArrayList<>();
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String sheetName = sheet.getSheetName();
                LocalDate date = parseSheetDate(sheetName);
                List<ParsedSrRow> rows = parseSrRows(sheet);
                result.add(new ParsedSheet(sheetName, date, rows));
            }
            return result;
        }
    }

    /** 시트명 "MM.DD" 를 2026년 날짜로 해석한다. 형식이 맞지 않으면 null. */
    private LocalDate parseSheetDate(String sheetName) {
        if (sheetName == null) return null;
        String trimmed = sheetName.trim();
        // "01.13", "1.13", "01-13" 등 . 또는 - 구분자 허용
        String[] parts = trimmed.split("[.\\-]");
        if (parts.length != 2) return null;
        try {
            int month = Integer.parseInt(parts[0].trim());
            int day = Integer.parseInt(parts[1].trim());
            return LocalDate.of(FIXED_YEAR, month, day);
        } catch (NumberFormatException | java.time.DateTimeException e) {
            return null;
        }
    }

    /**
     * 시트 상단 "시스템 반영 작업 요청" 표의 데이터 행(= SR 1건)을 추출한다.
     * 시트 하단의 반영 대상 서버 목록 등 다른 표는 제외하기 위해, No(B열)가 숫자인 행만 SR로 본다.
     * (병합된 No 셀은 첫 행에만 값이 있으므로 SR 1건당 한 번만 잡힌다.)
     */
    private List<ParsedSrRow> parseSrRows(Sheet sheet) {
        List<ParsedSrRow> rows = new ArrayList<>();
        for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            // No(B열)가 숫자가 아니면 SR 행이 아님(헤더/하단 다른 표/병합 잔여 행).
            if (!isNumericNo(row.getCell(COL_NO))) continue;

            String workContent = getCellValue(row.getCell(COL_WORK_CONTENT)).trim();
            String service = getCellValue(row.getCell(COL_SERVICE)).trim();
            String testDetail = getCellValue(row.getCell(COL_TEST_DETAIL)).trim();

            // 실제 SR이라면 작업내용 또는 TEST 상세 중 하나는 채워져 있다.
            // 서비스명만 있는 행(동일 작업의 서버별 반영 대상 목록 등)은 제외한다.
            if (workContent.isBlank() && testDetail.isBlank()) continue;

            String testUrlRaw = getCellValue(row.getCell(COL_TEST_URL));
            String[] urls = splitTestUrl(testUrlRaw);

            rows.add(new ParsedSrRow(
                    emptyToNull(service),
                    emptyToNull(workContent),
                    emptyToNull(getCellValue(row.getCell(COL_REQUESTER)).trim()),
                    emptyToNull(getCellValue(row.getCell(COL_WORKER)).trim()),
                    emptyToNull(urls[0]),
                    emptyToNull(urls[1]),
                    emptyToNull(testDetail),
                    parseOX(getCellValue(row.getCell(COL_FRONTEND))),
                    parseOX(getCellValue(row.getCell(COL_BACKEND))),
                    emptyToNull(getCellValue(row.getCell(COL_NOTE)).trim()),
                    parseOX(getCellValue(row.getCell(COL_FINAL_CONFIRM)))
            ));
        }
        return rows;
    }

    /**
     * TEST URL 셀을 검수/운영 URL로 분리한다.
     * 다음 두 형태를 모두 지원한다.
     * <pre>
     *   검수 : https://a        // 라벨과 URL 이 같은 줄
     *   운영 : https://b
     *
     *   검수                    // 라벨이 헤더 줄로 분리되고 아래 줄에 URL
     *   신청페이지: https://a
     *   운영
     *   신청페이지: https://b
     * </pre>
     * 라벨이 없으면 첫 URL을 검수로 본다.
     */
    private String[] splitTestUrl(String raw) {
        StringBuilder verify = new StringBuilder();
        StringBuilder prod = new StringBuilder();
        if (raw != null && !raw.isBlank()) {
            StringBuilder current = verify; // 라벨이 없으면 기본은 검수
            for (String line : raw.split("\\r?\\n")) {
                String t = line.trim();
                if (t.isEmpty()) continue;

                boolean hasProd = t.contains("운영");
                boolean hasVerify = t.contains("검수") || t.contains("개발");
                if (hasProd && !hasVerify) current = prod;
                else if (hasVerify && !hasProd) current = verify;

                if (t.contains("http")) {
                    append(current, stripLeadingLabel(t));
                } else if (!hasProd && !hasVerify) {
                    // URL 도 라벨도 없는 줄(예: "없음")은 그대로 보존
                    append(current, t);
                }
            }
        }
        return new String[]{verify.toString(), prod.toString()};
    }

    private void append(StringBuilder sb, String value) {
        if (value.isBlank()) return;
        if (sb.length() > 0) sb.append("\n");
        sb.append(value);
    }

    /** 줄 맨 앞의 검수/운영/개발 라벨과 콜론만 제거한다(신청페이지: 등 하위 라벨은 보존). */
    private String stripLeadingLabel(String line) {
        String t = line.trim();
        for (String label : new String[]{"검수", "운영", "개발"}) {
            if (t.startsWith(label)) {
                t = t.substring(label.length()).trim();
                if (t.startsWith(":")) t = t.substring(1).trim();
                break;
            }
        }
        return t;
    }

    /** No(B열) 셀이 숫자(날짜 아님)인지 확인한다. */
    private boolean isNumericNo(Cell cell) {
        if (cell == null) return false;
        if (cell.getCellType() == CellType.NUMERIC) {
            return !DateUtil.isCellDateFormatted(cell);
        }
        if (cell.getCellType() == CellType.STRING) {
            // 텍스트로 입력된 숫자("1", "2" 등)도 허용
            try {
                Integer.parseInt(cell.getStringCellValue().trim());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    /** "O"(대소문자 무관)면 true, "X"/빈값이면 false. */
    private Boolean parseOX(String value) {
        return value != null && value.trim().equalsIgnoreCase("O");
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    public String parseWorkItems(MultipartFile file) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            StringBuilder sb = new StringBuilder();

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return "작업 항목 없음";

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValue(cell));
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                sb.append("항목 ").append(i).append(": ");
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    if (cell != null) {
                        sb.append(headers.get(j)).append("=").append(getCellValue(cell)).append(", ");
                    }
                }
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
