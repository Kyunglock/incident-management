package com.incident.management.service;

import com.incident.management.common.DocxRenderer;
import com.incident.management.common.LlmClient;
import com.incident.management.common.PromptBuilder;
import com.incident.management.entity.Document;
import com.incident.management.git.GitProvider;
import com.incident.management.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VulnCheckService {

    private final GitProvider gitProvider;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final DocxRenderer docxRenderer;
    private final DocumentRepository documentRepository;

    @Transactional
    public String analyze(String system, String project, String commitFrom, String commitTo, Long releasePlanId) {
        try {
            String diff = gitProvider.rangeDiff(system, project, commitFrom, commitTo);
            String prompt = promptBuilder.buildVulnCheckPrompt(diff);
            String llmOutput = llmClient.chat(prompt);

            // reuse renderSideEffect shape but with vuln content
            String docPath = docxRenderer.renderSideEffect(llmOutput);

            documentRepository.save(Document.builder()
                    .type("VULN_CHECK")
                    .filePath(docPath)
                    .refId(releasePlanId)
                    .build());

            return docPath;
        } catch (Exception e) {
            log.error("취약점 체크 실패", e);
            throw new RuntimeException("취약점 체크 중 오류: " + e.getMessage());
        }
    }
}
