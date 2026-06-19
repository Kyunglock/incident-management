package com.incident.management.service;

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
    private final DocumentRepository documentRepository;

    /** LLM 으로 보낼 diff 최대 길이. 초과분은 분석에서 제외(속도 우선). */
    private static final int MAX_DIFF_CHARS = 12000;

    /** 분석 결과: LLM 추론 텍스트 */
    public record AnalysisResult(String content) {}

    /** from..to 범위 변경분으로 사이드이펙트를 분석한다. */
    @Transactional
    public AnalysisResult analyze(String system, String project, String commitFrom, String commitTo,
                                  String refType, Long refId) {
        try {
            String diff = gitProvider.rangeDiff(system, project, commitFrom, commitTo);
            return runAnalysis(diff, refType, refId);
        } catch (Exception e) {
            log.error("사이드이펙트 분석 실패", e);
            throw new RuntimeException("사이드이펙트 분석 중 오류: " + e.getMessage());
        }
    }

    /** 선택된 여러 커밋의 변경분을 합쳐서 사이드이펙트를 분석한다. */
    @Transactional
    public AnalysisResult analyzeCommits(String system, List<GitCommitRef> commits,
                                         String refType, Long refId) {
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
            return runAnalysis(combined.toString(), refType, refId);
        } catch (Exception e) {
            log.error("사이드이펙트 분석 실패", e);
            throw new RuntimeException("사이드이펙트 분석 중 오류: " + e.getMessage());
        }
    }

    private AnalysisResult runAnalysis(String diff, String refType, Long refId) throws Exception {
        int diffLen = diff == null ? 0 : diff.length();
        log.info("사이드이펙트 분석 시작 - diff 길이={}자", diffLen);

        // diff 가 비면 LLM 호출이 무의미하므로(빈/엉뚱한 응답 방지) 명확한 메시지를 남긴다.
        if (diff == null || diff.isBlank()) {
            String msg = "연동된 커밋의 변경 내용(diff)을 가져오지 못했습니다. "
                    + "GitLab 토큰 권한(read_api) 또는 프로젝트 경로/커밋 연동을 확인하세요.";
            documentRepository.save(Document.builder()
                    .type("SIDE_EFFECT").refType(refType).refId(refId).content(msg).build());
            return new AnalysisResult(msg);
        }

        // 속도를 위해 단일 호출. diff 가 크면 앞부분만 분석한다(전체는 보지 못함).
        String forLlm = diff;
        boolean truncated = false;
        if (diffLen > MAX_DIFF_CHARS) {
            forLlm = diff.substring(0, MAX_DIFF_CHARS);
            truncated = true;
            log.info("사이드이펙트 분석 - diff 가 길어 앞 {}자만 분석", MAX_DIFF_CHARS);
        }

        String llmOutput = llmClient.chat(promptBuilder.buildSideEffectPrompt(forLlm));
        if (truncated && llmOutput != null && !llmOutput.isBlank()) {
            llmOutput = llmOutput + "\n\n※ diff 가 길어 앞부분 " + MAX_DIFF_CHARS
                    + "자만 분석했습니다. (전체 " + diffLen + "자)";
        }
        log.info("사이드이펙트 분석 - LLM 출력 길이={}자, 미리보기={}",
                llmOutput == null ? 0 : llmOutput.length(),
                llmOutput == null ? "" : llmOutput.substring(0, Math.min(150, llmOutput.length())));

        documentRepository.save(Document.builder()
                .type("SIDE_EFFECT")
                .refType(refType)
                .refId(refId)
                .content(llmOutput)
                .build());

        return new AnalysisResult(llmOutput);
    }
}
