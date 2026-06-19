package com.incident.management.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.management.dto.WorkPlanData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocxRenderer {

    private final ObjectMapper objectMapper;
    private static final String OUTPUT_DIR = "generated-docs";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String renderReleasePlan(String llmJsonOutput) throws IOException {
        JsonNode node = objectMapper.readTree(llmJsonOutput);
        String filename = OUTPUT_DIR + "/release-plan-" + LocalDateTime.now().format(FMT) + ".docx";
        ensureDir();

        try (XWPFDocument doc = new XWPFDocument()) {
            addTitle(doc, node.path("title").asText("작업 계획서"));
            addSection(doc, "반영 목적", node.path("purpose").asText());
            addSection(doc, "반영 범위", node.path("scope").asText());

            XWPFParagraph changeTitle = doc.createParagraph();
            XWPFRun run = changeTitle.createRun();
            run.setText("변경 항목");
            run.setBold(true);

            JsonNode changes = node.path("changes");
            if (changes.isArray()) {
                for (JsonNode change : changes) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setIndentationLeft(720);
                    XWPFRun r = p.createRun();
                    r.setText("• " + change.path("item").asText() + ": " + change.path("description").asText());
                }
            }

            addSection(doc, "롤백 방안", node.path("rollback_plan").asText());
            addSection(doc, "위험도 및 영향", node.path("risk").asText());

            try (FileOutputStream fos = new FileOutputStream(filename)) {
                doc.write(fos);
            }
        }
        return filename;
    }

    public String renderSideEffect(String llmJsonOutput) throws IOException {
        JsonNode node = objectMapper.readTree(llmJsonOutput);
        String filename = OUTPUT_DIR + "/side-effect-" + LocalDateTime.now().format(FMT) + ".docx";
        ensureDir();

        try (XWPFDocument doc = new XWPFDocument()) {
            addTitle(doc, "사이드이펙트 분석 보고서");
            addSection(doc, "권고 사항", node.path("recommendation").asText());

            JsonNode risks = node.path("risk_items");
            if (risks.isArray()) {
                addHeading(doc, "위험 항목");
                for (JsonNode risk : risks) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setIndentationLeft(720);
                    XWPFRun r = p.createRun();
                    r.setText(String.format("• [%s] %s - %s",
                            risk.path("level").asText(),
                            risk.path("module").asText(),
                            risk.path("risk").asText()));
                }
            }

            try (FileOutputStream fos = new FileOutputStream(filename)) {
                doc.write(fos);
            }
        }
        return filename;
    }

    public String renderIncidentReport(String llmJsonOutput) throws IOException {
        JsonNode node = objectMapper.readTree(llmJsonOutput);
        String filename = OUTPUT_DIR + "/incident-report-" + LocalDateTime.now().format(FMT) + ".docx";
        ensureDir();

        try (XWPFDocument doc = new XWPFDocument()) {
            addTitle(doc, "장애 분석 보고서");
            addSection(doc, "근본 원인", node.path("root_cause").asText());
            addSection(doc, "타임라인", node.path("timeline").asText());
            addSection(doc, "조치 방안", node.path("resolution").asText());
            addSection(doc, "재발 방지", node.path("prevention").asText());

            try (FileOutputStream fos = new FileOutputStream(filename)) {
                doc.write(fos);
            }
        }
        return filename;
    }

    /** 반영이력 기반 '시스템 작업(적용) 계획서' docx 생성 (현재 UI 는 텍스트 사용, docx 는 추후용). */
    public String renderWorkPlan(WorkPlanData data) throws IOException {
        String filename = OUTPUT_DIR + "/work-plan-" + LocalDateTime.now().format(FMT) + ".docx";
        ensureDir();

        try (XWPFDocument doc = new XWPFDocument()) {
            // 제목
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun tr = title.createRun();
            tr.setText("시스템 작업(적용) 계획서");
            tr.setBold(true);
            tr.setFontSize(18);
            tr.setUnderline(UnderlinePatterns.SINGLE);
            doc.createParagraph();

            // 결재(서명) 표: 위탁운영사(작업자/확인자(PL)/승인자(PM)) + KERIS(담당자)
            XWPFTable approval = doc.createTable(3, 4);
            removeTableBorderToggle(approval);
            cell(approval, 0, 0, "위탁운영사", true, true, ParagraphAlignment.CENTER);
            mergeAcross(approval.getRow(0), 0, 3);
            cell(approval, 0, 1, "KERIS", true, true, ParagraphAlignment.CENTER); // 병합 후 인덱스 1 = 원래 col3
            cell(approval, 1, 0, "작업자", true, true, ParagraphAlignment.CENTER);
            cell(approval, 1, 1, "확인자(PL)", true, true, ParagraphAlignment.CENTER);
            cell(approval, 1, 2, "승인자(PM)", true, true, ParagraphAlignment.CENTER);
            cell(approval, 1, 3, "담당자", true, true, ParagraphAlignment.CENTER);
            // 3행은 서명 공란 (높이 확보)
            for (int c = 0; c < 4; c++) {
                approval.getRow(2).getCell(c).setText(" ");
            }
            approval.getRow(2).setHeight(700);

            // 작성일 / 작성자
            XWPFParagraph meta = doc.createParagraph();
            meta.setAlignment(ParagraphAlignment.RIGHT);
            XWPFRun mr = meta.createRun();
            mr.setText("작성일 : " + nv(data.createdDate()) + ". / 작성자 : " + nv(data.author()));
            doc.createParagraph();

            // 본문 표 (4열 그리드). 행을 모두 4셀로 먼저 만든 뒤 내용 채우고, 병합은 마지막에 한다.
            XWPFTable t = doc.createTable(1, 4);
            for (int i = 0; i < 6; i++) {
                t.createRow();   // row1~6 (row0 이 4셀이므로 새 행도 4셀로 생성됨)
            }

            // row0: 작업명
            cell(t, 0, 0, "작 업 명", true, true, null);
            cell(t, 0, 1, nv(data.taskName()), false, false, null);
            // row1: 작업예정일 | date | 작업대상 서버 | server
            cell(t, 1, 0, "작업예정일", true, true, null);
            cell(t, 1, 1, nv(data.taskDate()), false, false, null);
            cell(t, 1, 2, "작업대상 서버", true, true, null);
            cell(t, 1, 3, nv(data.targetServer()), false, false, null);
            // row2: 보안 사전확인 | 확인완료 | 확인방법 | ...
            cell(t, 2, 0, "소스코드 보안(취약점 등)\n사전확인 여부", true, true, null);
            cell(t, 2, 1, "확인완료", false, false, ParagraphAlignment.CENTER);
            cell(t, 2, 2, "확인방법", true, true, ParagraphAlignment.CENTER);
            cell(t, 2, 3, "보안 tool 활용,\nPL(PM) 직접확인", false, false, null);
            // row3: 작업내용 (big)
            cell(t, 3, 0, "작업내용", true, true, null);
            fillWorkContent(t.getRow(3).getCell(1), data);
            // row4: 작업지원 요청사항
            cell(t, 4, 0, "작업지원\n요청사항", true, true, null);
            multiLine(t.getRow(4).getCell(1), List.of(
                    "○ 사전 지원 요청사항", "  - 없음",
                    "○ 작업 간 지원 요청사항", "  - 없음"));
            // row5: 작업 후 점검사항
            cell(t, 5, 0, "작업 후\n점검사항", true, true, null);
            multiLine(t.getRow(5).getCell(1), List.of("○ 점검 사항", "  - 서비스 모니터링"));
            // row6: 추후 예정사항
            cell(t, 6, 0, "추후\n예정사항", true, true, null);
            cell(t, 6, 1, "해당 사항 없음", false, false, null);

            // 병합은 모든 셀을 채운 뒤 마지막에 (cols 1~3 → 내용 셀 전체폭)
            mergeAcross(t.getRow(0), 1, 3);
            mergeAcross(t.getRow(3), 1, 3);
            mergeAcross(t.getRow(4), 1, 3);
            mergeAcross(t.getRow(5), 1, 3);
            mergeAcross(t.getRow(6), 1, 3);

            // 하단 안내
            doc.createParagraph();
            XWPFParagraph n1 = doc.createParagraph();
            XWPFRun n1r = n1.createRun();
            n1r.setText("※ 시스템 적용은 KERIS 담당자 사전 승인 후 적용");
            n1r.setBold(true);
            XWPFParagraph n2 = doc.createParagraph();
            n2.createRun().setText("  - 시스템 적용 시간 : 16:00~19:00");

            try (FileOutputStream fos = new FileOutputStream(filename)) {
                doc.write(fos);
            }
        }
        return filename;
    }

    /** '작업내용' 셀: 세부수행내용(요청자/재기동/서비스별 항목/작업절차/작업자) + 서비스 원복 계획 */
    private void fillWorkContent(XWPFTableCell c, WorkPlanData data) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("○ 세부수행내용");
        lines.add("  - 요청자 : " + nv(data.requesters()));
        lines.add("  - 재기동 여부 : " + nv(data.restart()));
        lines.add("  - 작업내용 :");
        int idx = 1;
        List<WorkPlanData.ServiceWork> services = data.services();
        if (services != null) {
            for (WorkPlanData.ServiceWork sw : services) {
                lines.add("    " + idx + ". " + nv(sw.service()));
                lines.add("    가) 세부 작업내용");
                if (sw.items() != null) {
                    for (String item : sw.items()) {
                        lines.add("      - " + item);
                    }
                }
                lines.add("    나) 작업 절차");
                lines.add("      1) 소스 백업");
                lines.add("         ㆍ서비스별 각 WEB, WAS 서버의 소스 백업");
                lines.add("      2) 소스 변경 작업");
                lines.add("         ㆍ서비스별 각 WEB, WAS 서버의 작업 파일 적용");
                lines.add("      3) 테스트 진행");
                idx++;
            }
        }
        lines.add("  - 작업자 / 관리자 : " + nv(data.workers()) + " / " + nv(data.manager()));
        lines.add("");
        lines.add("○ 서비스 원복 계획");
        lines.add("  - 기존 운영서버에 소스 파일을 백업하여 문제 발생 시 백업 소스를 원복 할 예정입니다.");
        multiLine(c, lines);
    }

    /** 셀에 여러 줄을 단락으로 채운다(기본 단락 재사용). */
    private void multiLine(XWPFTableCell cell, List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            XWPFParagraph p = (i == 0) ? cell.getParagraphs().get(0) : cell.addParagraph();
            p.createRun().setText(lines.get(i));
        }
    }

    /** 표 셀 설정 (텍스트/굵게/음영/정렬). \n 은 줄바꿈 단락으로 처리. */
    private void cell(XWPFTable table, int row, int col, String text,
                      boolean bold, boolean shaded, ParagraphAlignment align) {
        XWPFTableCell cell = table.getRow(row).getCell(col);
        String[] parts = text.split("\n", -1);
        for (int i = 0; i < parts.length; i++) {
            XWPFParagraph p = (i == 0) ? cell.getParagraphs().get(0) : cell.addParagraph();
            if (align != null) p.setAlignment(align);
            XWPFRun r = p.createRun();
            r.setText(parts[i]);
            r.setBold(bold);
        }
        if (shaded) {
            CTTcPr pr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
            pr.addNewShd().setFill("D9D9D9");
        }
    }

    /** row 의 fromCol 셀을 span 개 만큼 가로 병합한다. */
    private void mergeAcross(XWPFTableRow row, int fromCol, int span) {
        XWPFTableCell cell = row.getCell(fromCol);
        CTTcPr pr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        pr.addNewGridSpan().setVal(BigInteger.valueOf(span));
        // 병합된 뒤쪽 셀 제거
        for (int i = 0; i < span - 1; i++) {
            row.removeCell(fromCol + 1);
        }
    }

    /** 표 테두리 유지(기본). 자리표시용 — 필요 시 스타일 조정. */
    private void removeTableBorderToggle(XWPFTable table) {
        // 기본 테두리 사용. (POI 기본 표는 테두리가 있음)
    }

    private String nv(String s) {
        return s == null ? "" : s;
    }

    private void addTitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(16);
    }

    private void addHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(12);
    }

    private void addSection(XWPFDocument doc, String heading, String content) {
        addHeading(doc, heading);
        XWPFParagraph p = doc.createParagraph();
        p.setIndentationLeft(360);
        XWPFRun r = p.createRun();
        r.setText(content);
        doc.createParagraph();
    }

    private void ensureDir() throws IOException {
        Path dir = Paths.get(OUTPUT_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
    }
}
