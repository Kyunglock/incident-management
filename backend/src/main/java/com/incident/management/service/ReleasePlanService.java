package com.incident.management.service;

import com.incident.management.common.DocxRenderer;
import com.incident.management.common.ExcelParser;
import com.incident.management.common.GitAdapter;
import com.incident.management.common.LlmClient;
import com.incident.management.common.PromptBuilder;
import com.incident.management.dto.excel.ParsedSheet;
import com.incident.management.dto.excel.ParsedSrRow;
import com.incident.management.dto.response.PageResponse;
import com.incident.management.dto.response.ReleasePlanImportResponse;
import com.incident.management.dto.response.ReleasePlanResponse;
import com.incident.management.entity.Document;
import com.incident.management.entity.ReleaseHistory;
import com.incident.management.entity.ReleasePlan;
import com.incident.management.exception.ResourceNotFoundException;
import com.incident.management.repository.DocumentRepository;
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
    private final GitAdapter gitAdapter;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final DocxRenderer docxRenderer;
    private final ReleasePlanRepository releasePlanRepository;
    private final ReleaseHistoryRepository releaseHistoryRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public ReleasePlanResponse generatePlan(
            MultipartFile excelFile,
            boolean useGit,
            String repoPath,
            String commitFrom,
            String commitTo,
            String releaseTitle) {
        try {
            if (excelFile == null || excelFile.isEmpty()) {
                throw new IllegalArgumentException("공유 Excel 파일은 필수입니다.");
            }
            String excelSummary = excelParser.parseWorkItems(excelFile);

            String commitMessages = (useGit && repoPath != null && !repoPath.isBlank())
                    ? gitAdapter.getCommitMessages(repoPath, 10)
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
     */
    @Transactional
    public ReleasePlanImportResponse importFromExcel(MultipartFile excelFile) {
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

            ReleasePlan plan = ReleasePlan.builder()
                    .title(title)
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

    private ReleasePlanResponse toResponse(ReleasePlan plan) {
        return ReleasePlanResponse.builder()
                .id(plan.getId())
                .title(plan.getTitle())
                .docPath(plan.getDocPath())
                .llmOutput(plan.getLlmOutput())
                .createdAt(plan.getCreatedAt())
                .build();
    }
}
