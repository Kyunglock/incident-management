package com.incident.management.service;

import com.incident.management.common.DocxRenderer;
import com.incident.management.common.ExcelParser;
import com.incident.management.common.LlmClient;
import com.incident.management.common.PromptBuilder;
import com.incident.management.git.GitProvider;
import com.incident.management.dto.excel.ParsedSheet;
import com.incident.management.dto.excel.ParsedSrRow;
import com.incident.management.dto.response.PageResponse;
import com.incident.management.dto.response.ReleasePlanImportResponse;
import com.incident.management.dto.response.ReleasePlanResponse;
import com.incident.management.entity.Document;
import com.incident.management.entity.Incident;
import com.incident.management.entity.ReleaseHistory;
import com.incident.management.entity.ReleasePlan;
import com.incident.management.exception.ResourceNotFoundException;
import com.incident.management.repository.DocumentRepository;
import com.incident.management.repository.IncidentAnalysisRepository;
import com.incident.management.repository.IncidentRepository;
import com.incident.management.repository.ReleaseHistoryRepository;
import com.incident.management.repository.ReleasePlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReleasePlanService {

    /** 반영 계획서 제목 형식: 연도 2026 고정. */
    private static final DateTimeFormatter TITLE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ExcelParser excelParser;
    private final GitProvider gitProvider;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final DocxRenderer docxRenderer;
    private final ReleasePlanRepository releasePlanRepository;
    private final ReleaseHistoryRepository releaseHistoryRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentAnalysisRepository incidentAnalysisRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public ReleasePlanResponse generatePlan(
            MultipartFile excelFile,
            boolean useGit,
            String system,
            String commitFrom,
            String commitTo,
            String releaseTitle) {
        try {
            if (excelFile == null || excelFile.isEmpty()) {
                throw new IllegalArgumentException("공유 Excel 파일은 필수입니다.");
            }
            String excelSummary = excelParser.parseWorkItems(excelFile);

            String commitMessages = (useGit && system != null && !system.isBlank())
                    ? gitProvider.commitMessages(system, 10)
                    : "git 정보 없음";

            String prompt = promptBuilder.buildReleasePlanPrompt(commitMessages, excelSummary);
            String llmOutput = llmClient.chat(prompt);

            String docPath = docxRenderer.renderReleasePlan(llmOutput);

            ReleasePlan plan = ReleasePlan.builder()
                    .title(releaseTitle != null ? releaseTitle : "반영 계획서 " + LocalDateTime.now())
                    .docPath(docPath)
                    .excelPath(excelFile.getOriginalFilename())
                    .rawInput("{\"excel\": \"" + excelFile.getOriginalFilename() + "\"}")
                    .llmOutput(llmOutput)
                    .build();
            plan = releasePlanRepository.save(plan);

            Document doc = Document.builder()
                    .type("RELEASE_PLAN")
                    .filePath(docPath)
                    .refId(plan.getId())
                    .build();
            documentRepository.save(doc);

            return toResponse(plan);
        } catch (Exception e) {
            log.error("반영 계획서 생성 실패", e);
            throw new RuntimeException("반영 계획서 생성 중 오류: " + e.getMessage());
        }
    }

    /**
     * 다중 시트 엑셀을 업로드해 시트(날짜)별로 반영 계획서를 생성한다.
     * - 시트명 "MM.DD" → 제목 "2026-MM-DD"
     * - 각 SR 행 → 반영 이력 1건
     * - 같은 날짜(제목)가 이미 DB에 있으면 해당 시트는 무시한다.
     * - 날짜 형식이 잘못되었거나 SR 행이 없는 시트도 건너뛴다.
     * - summarize=true 이면 시트별 작업내용을 LLM으로 한 줄 요약해 summary 에 저장한다.
     */
    @Transactional
    public ReleasePlanImportResponse importFromExcel(MultipartFile excelFile, boolean summarize) {
        if (excelFile == null || excelFile.isEmpty()) {
            throw new IllegalArgumentException("공유 Excel 파일은 필수입니다.");
        }

        List<ParsedSheet> sheets;
        try {
            sheets = excelParser.parseAllSheets(excelFile);
        } catch (Exception e) {
            log.error("엑셀 파싱 실패", e);
            throw new RuntimeException("엑셀 파싱 중 오류: " + e.getMessage());
        }

        List<ReleasePlanImportResponse.Created> created = new ArrayList<>();
        List<String> skippedExisting = new ArrayList<>();
        List<String> skippedEmptyOrInvalid = new ArrayList<>();
        String excelName = excelFile.getOriginalFilename();
        // LLM 호출이 연속 실패하면(예: LLM 미구동) 이후 시트는 요약을 건너뛴다.
        boolean llmAvailable = summarize;

        for (ParsedSheet sheet : sheets) {
            // 날짜 형식이 잘못되었거나 SR 행이 없으면 건너뛴다.
            if (sheet.date() == null || sheet.rows().isEmpty()) {
                skippedEmptyOrInvalid.add(sheet.sheetName());
                continue;
            }

            String title = sheet.date().format(TITLE_DATE_FORMAT);

            // 이미 같은 날짜가 저장되어 있으면 무시한다.
            if (releasePlanRepository.existsByTitle(title)) {
                skippedExisting.add(title);
                continue;
            }

            String summary = null;
            if (llmAvailable) {
                try {
                    summary = summarizeSheet(sheet);
                } catch (Exception e) {
                    // 한 번 실패하면 나머지 시트도 실패할 가능성이 높으므로 이후 요약은 생략한다.
                    llmAvailable = false;
                    log.warn("LLM 요약 실패 — 이후 시트는 요약 없이 진행합니다: {}", e.getMessage());
                }
            }

            ReleasePlan plan = ReleasePlan.builder()
                    .title(title)
                    .summary(summary)
                    .excelPath(excelName)
                    .rawInput(String.format("{\"excel\": \"%s\", \"sheet\": \"%s\"}",
                            excelName, sheet.sheetName()))
                    .build();
            plan = releasePlanRepository.save(plan);

            for (ParsedSrRow row : sheet.rows()) {
                ReleaseHistory history = ReleaseHistory.builder()
                        .releasePlan(plan)
                        .service(row.service())
                        .workContent(row.workContent())
                        .requester(row.requester())
                        .worker(row.worker())
                        .testUrlVerify(row.testUrlVerify())
                        .testUrlProd(row.testUrlProd())
                        .testDetail(row.testDetail())
                        .frontendChanged(row.frontendChanged())
                        .backendChanged(row.backendChanged())
                        .note(row.note())
                        .finalConfirmed(Boolean.TRUE.equals(row.finalConfirmed()))
                        .build();
                releaseHistoryRepository.save(history);
            }

            created.add(ReleasePlanImportResponse.Created.builder()
                    .planId(plan.getId())
                    .title(title)
                    .summary(summary)
                    .historyCount(sheet.rows().size())
                    .build());
        }

        log.info("엑셀 다중 시트 임포트 완료: 전체 {}시트, 생성 {}건, 기존 스킵 {}건, 무효/빈 시트 {}건",
                sheets.size(), created.size(), skippedExisting.size(), skippedEmptyOrInvalid.size());

        return ReleasePlanImportResponse.builder()
                .totalSheets(sheets.size())
                .created(created)
                .skippedExisting(skippedExisting)
                .skippedEmptyOrInvalid(skippedEmptyOrInvalid)
                .build();
    }

    /** 시트의 SR 작업내용을 모아 LLM으로 한 줄 요약한다. */
    private String summarizeSheet(ParsedSheet sheet) {
        StringBuilder items = new StringBuilder();
        int no = 1;
        for (ParsedSrRow row : sheet.rows()) {
            String content = row.workContent() != null ? row.workContent() : row.testDetail();
            if (content == null || content.isBlank()) continue;
            String service = row.service() != null ? row.service() : "";
            items.append(no++).append(". ");
            if (!service.isBlank()) items.append("[").append(service).append("] ");
            // 작업내용이 길 수 있으니 줄바꿈은 공백으로 정리
            items.append(content.replaceAll("\\s+", " ").trim()).append("\n");
        }
        if (items.length() == 0) return null;

        String prompt = promptBuilder.buildWorkSummaryPrompt(items.toString());
        String summary = llmClient.chat(prompt);
        if (summary == null) return null;
        summary = summary.trim();
        if (summary.isEmpty()) return null;
        // 컬럼 길이(500) 보호
        return summary.length() > 500 ? summary.substring(0, 500) : summary;
    }

    public PageResponse<ReleasePlanResponse> getAll(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ReleasePlan> result = releasePlanRepository
                .findByTitleContainingIgnoreCase(keyword == null ? "" : keyword, pageable);
        return PageResponse.of(result, this::toResponse);
    }

    public ReleasePlanResponse getById(Long id) {
        return toResponse(releasePlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("반영 계획서를 찾을 수 없습니다: " + id)));
    }

    /**
     * 반영 계획서와 그 하위(반영 이력 → 장애 → 장애 분석) 및 관련 문서 메타를 함께 삭제한다.
     * FK 제약을 제거했으므로 하위 데이터를 직접 정리해 고아 레코드를 방지한다.
     */
    @Transactional
    public void deletePlan(Long id) {
        ReleasePlan plan = releasePlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("반영 계획서를 찾을 수 없습니다: " + id));

        List<Long> historyIds = releaseHistoryRepository
                .findByReleasePlanIdOrderByCreatedAtDesc(id).stream()
                .map(ReleaseHistory::getId)
                .toList();

        if (!historyIds.isEmpty()) {
            List<Long> incidentIds = incidentRepository.findByReleaseHistoryIdIn(historyIds).stream()
                    .map(Incident::getId)
                    .toList();
            if (!incidentIds.isEmpty()) {
                incidentAnalysisRepository.deleteByIncidentIdIn(incidentIds);
            }
            incidentRepository.deleteByReleaseHistoryIdIn(historyIds);
        }
        releaseHistoryRepository.deleteByReleasePlanId(id);

        // 계획서에 연결된 문서 메타(있으면) 정리
        documentRepository.findByRefId(id).stream()
                .filter(d -> "RELEASE_PLAN".equals(d.getType()))
                .forEach(documentRepository::delete);

        releasePlanRepository.delete(plan);
        log.info("반영 계획서 삭제 완료: id={}, title={}, 반영이력 {}건",
                id, plan.getTitle(), historyIds.size());
    }

    private ReleasePlanResponse toResponse(ReleasePlan plan) {
        return ReleasePlanResponse.builder()
                .id(plan.getId())
                .title(plan.getTitle())
                .summary(plan.getSummary())
                .docPath(plan.getDocPath())
                .llmOutput(plan.getLlmOutput())
                .createdAt(plan.getCreatedAt())
                .build();
    }
}
