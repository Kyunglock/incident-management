package com.incident.management.service;

import com.incident.management.common.DocxRenderer;
import com.incident.management.common.LlmClient;
import com.incident.management.common.PromptBuilder;
import com.incident.management.entity.Document;
import com.incident.management.git.GitCommitRef;
import com.incident.management.git.GitProvider;
import com.incident.management.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SideEffectService {

    private final GitProvider gitProvider;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final DocxRenderer docxRenderer;
    private final DocumentRepository documentRepository;

    /** from..to 범위 변경분으로 사이드이펙트를 분석한다. */
    @Transactional
    public String analyze(String system, String project, String commitFrom, String commitTo, Long releasePlanId) {
        try {
            String diff = gitProvider.rangeDiff(system, project, commitFrom, commitTo);
            return runAnalysis(diff, releasePlanId);
        } catch (Exception e) {
            log.error("사이드이펙트 분석 실패", e);
            throw new RuntimeException("사이드이펙트 분석 중 오류: " + e.getMessage());
        }
    }

    /** 선택된 여러 커밋의 변경분을 합쳐서 사이드이펙트를 분석한다. */
    @Transactional
    public String analyzeCommits(String system, List<GitCommitRef> commits, Long releasePlanId) {
        try {
            StringBuilder combined = new StringBuilder();
            for (GitCommitRef ref : commits) {
                if (ref == null || ref.hash() == null || ref.hash().isBlank()) {
                    continue;
                }
                // 각 커밋의 부모 대비 변경분을 모은다.
                String diff = gitProvider.commitDiff(system, ref.project(), ref.hash());
                if (diff != null && !diff.isBlank()) {
                    combined.append("===== commit ").append(ref.toToken()).append(" =====\n")
                            .append(diff).append("\n\n");
                }
            }
            return runAnalysis(combined.toString(), releasePlanId);
        } catch (Exception e) {
            log.error("사이드이펙트 분석 실패", e);
            throw new RuntimeException("사이드이펙트 분석 중 오류: " + e.getMessage());
        }
    }

    private String runAnalysis(String diff, Long releasePlanId) throws Exception {
        String prompt = promptBuilder.buildSideEffectPrompt(diff);
        String llmOutput = llmClient.chat(prompt);
        String docPath = docxRenderer.renderSideEffect(llmOutput);

        documentRepository.save(Document.builder()
                .type("SIDE_EFFECT")
                .filePath(docPath)
                .refId(releasePlanId)
                .build());

        return docPath;
    }
}
