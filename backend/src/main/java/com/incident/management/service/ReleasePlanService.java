package com.incident.management.service;

import com.incident.management.common.DocxRenderer;
import com.incident.management.common.ExcelParser;
import com.incident.management.common.GitAdapter;
import com.incident.management.common.LlmClient;
import com.incident.management.common.PromptBuilder;
import com.incident.management.dto.response.PageResponse;
import com.incident.management.dto.response.ReleasePlanResponse;
import com.incident.management.entity.Document;
import com.incident.management.entity.ReleasePlan;
import com.incident.management.exception.ResourceNotFoundException;
import com.incident.management.repository.DocumentRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ReleasePlanService {

    private final ExcelParser excelParser;
    private final GitAdapter gitAdapter;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final DocxRenderer docxRenderer;
    private final ReleasePlanRepository releasePlanRepository;
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
