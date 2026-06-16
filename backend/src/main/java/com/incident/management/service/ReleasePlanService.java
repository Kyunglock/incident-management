package com.incident.management.service;

import com.incident.management.common.DocxRenderer;
import com.incident.management.common.ExcelParser;
import com.incident.management.common.GitAdapter;
import com.incident.management.common.LlmClient;
import com.incident.management.common.PromptBuilder;
import com.incident.management.dto.response.ReleasePlanResponse;
import com.incident.management.entity.Document;
import com.incident.management.entity.ReleaseHistory;
import com.incident.management.repository.DocumentRepository;
import com.incident.management.repository.ReleaseHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReleasePlanService {

    private final ExcelParser excelParser;
    private final GitAdapter gitAdapter;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final DocxRenderer docxRenderer;
    private final ReleaseHistoryRepository releaseHistoryRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public ReleasePlanResponse generatePlan(
            MultipartFile excelFile,
            String srContent,
            String repoPath,
            String commitFrom,
            String commitTo,
            String releaseTitle) {
        try {
            String excelSummary = (excelFile != null && !excelFile.isEmpty())
                    ? excelParser.parseWorkItems(excelFile)
                    : "Excel 파일 없음";

            String commitMessages = (repoPath != null && !repoPath.isBlank())
                    ? gitAdapter.getCommitMessages(repoPath, 10)
                    : "git 정보 없음";

            String prompt = promptBuilder.buildReleasePlanPrompt(srContent, commitMessages, excelSummary);
            String llmOutput = llmClient.chat(prompt);

            String docPath = docxRenderer.renderReleasePlan(llmOutput);

            ReleaseHistory history = ReleaseHistory.builder()
                    .title(releaseTitle != null ? releaseTitle : "반영 계획서 " + LocalDateTime.now())
                    .releaseAt(LocalDateTime.now())
                    .docPath(docPath)
                    .rawInput("{\"sr\": \"" + srContent.replace("\"", "'") + "\"}")
                    .llmOutput(llmOutput)
                    .build();
            history = releaseHistoryRepository.save(history);

            Document doc = Document.builder()
                    .type("RELEASE_PLAN")
                    .filePath(docPath)
                    .refId(history.getId())
                    .build();
            documentRepository.save(doc);

            return ReleasePlanResponse.builder()
                    .id(history.getId())
                    .title(history.getTitle())
                    .docPath(docPath)
                    .llmOutput(llmOutput)
                    .createdAt(history.getCreatedAt())
                    .build();
        } catch (Exception e) {
            log.error("반영 계획서 생성 실패", e);
            throw new RuntimeException("반영 계획서 생성 중 오류: " + e.getMessage());
        }
    }

    public List<ReleasePlanResponse> getHistory() {
        return releaseHistoryRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(h -> ReleasePlanResponse.builder()
                        .id(h.getId())
                        .title(h.getTitle())
                        .docPath(h.getDocPath())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
