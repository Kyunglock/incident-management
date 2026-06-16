package com.incident.management.service;

import com.incident.management.common.DocxRenderer;
import com.incident.management.common.GitAdapter;
import com.incident.management.common.LlmClient;
import com.incident.management.common.PromptBuilder;
import com.incident.management.entity.Document;
import com.incident.management.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SideEffectService {

    private final GitAdapter gitAdapter;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final DocxRenderer docxRenderer;
    private final DocumentRepository documentRepository;

    @Transactional
    public String analyze(String repoPath, String commitFrom, String commitTo, Long releaseHistoryId) {
        try {
            String diff = gitAdapter.getDiff(repoPath, commitFrom, commitTo);
            String prompt = promptBuilder.buildSideEffectPrompt(diff);
            String llmOutput = llmClient.chat(prompt);
            String docPath = docxRenderer.renderSideEffect(llmOutput);

            documentRepository.save(Document.builder()
                    .type("SIDE_EFFECT")
                    .filePath(docPath)
                    .refId(releaseHistoryId)
                    .build());

            return docPath;
        } catch (Exception e) {
            log.error("사이드이펙트 분석 실패", e);
            throw new RuntimeException("사이드이펙트 분석 중 오류: " + e.getMessage());
        }
    }
}
