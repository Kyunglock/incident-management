package com.incident.management.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
            addTitle(doc, node.path("title").asText("반영 계획서"));
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
