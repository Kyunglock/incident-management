package com.incident.management.service;

import com.incident.management.dto.request.CreateReleaseHistoryRequest;
import com.incident.management.dto.response.ReleaseHistoryResponse;
import com.incident.management.dto.response.ReleaseHistorySummaryResponse;
import com.incident.management.dto.response.SideEffectReportResponse;
import com.incident.management.entity.Document;
import com.incident.management.entity.ReleaseHistory;
import com.incident.management.entity.ReleasePlan;
import com.incident.management.exception.ResourceNotFoundException;
import com.incident.management.git.GitCommitRef;
import com.incident.management.git.GitProvider;
import com.incident.management.repository.DocumentRepository;
import com.incident.management.repository.IncidentRepository;
import com.incident.management.repository.ReleaseHistoryRepository;
import com.incident.management.repository.ReleasePlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReleaseHistoryService {

    private final ReleaseHistoryRepository releaseHistoryRepository;
    private final ReleasePlanRepository releasePlanRepository;
    private final RedmineService redmineService;
    private final IncidentRepository incidentRepository;
    private final SideEffectService sideEffectService;
    private final DocumentRepository documentRepository;
    private final com.incident.management.common.LlmClient llmClient;
    private final com.incident.management.common.PromptBuilder promptBuilder;
    private final GitProvider gitProvider;

    /** 테스트케이스 생성 시 LLM 에 보낼 diff 최대 길이 */
    private static final int TEST_CASE_DIFF_LIMIT = 12000;

    /** Document.refType 값 (SR 연동 사이드이펙트 리포트) */
    private static final String REF_TYPE_HISTORY = "RELEASE_HISTORY";

    @Transactional
    public ReleaseHistoryResponse create(Long releasePlanId, CreateReleaseHistoryRequest request) {
        if (request.getSrNumber() == null || request.getSrNumber().isBlank()) {
            throw new IllegalArgumentException("SR 번호는 필수입니다.");
        }
        ReleasePlan plan = releasePlanRepository.findById(releasePlanId)
                .orElseThrow(() -> new ResourceNotFoundException("작업 계획서를 찾을 수 없습니다: " + releasePlanId));

        ReleaseHistory history = ReleaseHistory.builder()
                .releasePlan(plan)
                .srNumber(request.getSrNumber())
                .build();

        // SR 번호로 레드마인 연동해 서비스/작업내용/요청자 등 상세 정보를 채운다.
        redmineService.enrich(history);

        history = releaseHistoryRepository.save(history);
        return toResponse(history);
    }

    public List<ReleaseHistoryResponse> getByPlan(Long releasePlanId) {
        List<ReleaseHistory> histories =
                releaseHistoryRepository.findByReleasePlanIdOrderByCreatedAtDesc(releasePlanId);
        // 장애 등록 여부 / 사이드이펙트 리포트 존재 여부를 한 번에 조회 (N+1 방지)
        List<Long> ids = histories.stream().map(ReleaseHistory::getId).collect(Collectors.toList());
        Set<Long> withIncident = historyIdsWithIncident(ids);
        Set<Long> withReport = historyIdsWithReport(ids);
        return histories.stream()
                .map(h -> toResponse(h, withIncident.contains(h.getId()), withReport.contains(h.getId())))
                .collect(Collectors.toList());
    }

    /** SR 선택용 전체 요약 목록 (장애 등록 드롭다운 등) */
    public List<ReleaseHistorySummaryResponse> getAllSummaries() {
        return releaseHistoryRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(h -> ReleaseHistorySummaryResponse.builder()
                        .id(h.getId())
                        .srNumber(h.getSrNumber())
                        .service(h.getService())
                        .releasePlanId(h.getReleasePlan() != null ? h.getReleasePlan().getId() : null)
                        .build())
                .collect(Collectors.toList());
    }

    public ReleaseHistoryResponse getById(Long id) {
        return toResponse(releaseHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("반영 이력을 찾을 수 없습니다: " + id)));
    }

    @Transactional
    public ReleaseHistoryResponse updateFinalConfirmed(Long id, boolean finalConfirmed) {
        ReleaseHistory history = releaseHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("반영 이력을 찾을 수 없습니다: " + id));
        history.setFinalConfirmed(finalConfirmed);
        return toResponse(history);
    }

    @Transactional
    public ReleaseHistoryResponse updateSrNumber(Long id, String srNumber) {
        ReleaseHistory history = releaseHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("반영 이력을 찾을 수 없습니다: " + id));
        history.setSrNumber(srNumber);
        // SR 번호가 바뀌면 레드마인 연동으로 상세 정보를 다시 채운다.
        redmineService.enrich(history);
        return toResponse(history);
    }

    /**
     * SR(반영 이력)에 git 커밋을 연동한다. 여러 커밋을 콤마로 구분해 전달.
     * commitHashes 가 비면 연동 해제.
     */
    @Transactional
    public ReleaseHistoryResponse updateGitCommit(Long id, String system, String commitHashes) {
        ReleaseHistory history = releaseHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("반영 이력을 찾을 수 없습니다: " + id));
        List<String> hashes = parseHashes(commitHashes);
        boolean cleared = hashes.isEmpty();
        history.setGitSystem(cleared ? null : system);
        history.setGitCommitHashes(cleared ? null : String.join(",", hashes));
        return toResponse(history);
    }

    /** 연동된 git 커밋(들) 기준으로 사이드이펙트 검토를 수행하고 결과(docPath+분석원문)를 반환한다. */
    @Transactional
    public SideEffectService.AnalysisResult analyzeSideEffect(Long id) {
        ReleaseHistory history = releaseHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("반영 이력을 찾을 수 없습니다: " + id));
        List<GitCommitRef> refs = parseHashes(history.getGitCommitHashes()).stream()
                .map(GitCommitRef::parse)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        if (refs.isEmpty()) {
            throw new IllegalArgumentException("git 커밋이 연동되지 않았습니다. 먼저 커밋을 연동하세요.");
        }
        // 연동된 커밋들(project@hash)의 변경분을 합쳐서 분석한다. 리포트는 이 SR(history)에 연결.
        return sideEffectService.analyzeCommits(history.getGitSystem(), refs, REF_TYPE_HISTORY, id);
    }

    /** 작업내용 + 연동 커밋의 git diff 를 바탕으로 로컬 LLM 으로 테스트케이스를 생성해 저장한다. */
    @Transactional
    public ReleaseHistoryResponse generateTestCases(Long id) {
        ReleaseHistory history = releaseHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("반영 이력을 찾을 수 없습니다: " + id));
        String workContent = history.getWorkContent();
        if (workContent == null || workContent.isBlank()) {
            throw new IllegalArgumentException("작업내용이 없어 테스트케이스를 생성할 수 없습니다.");
        }
        String diff = buildLinkedDiff(history);
        String testCase = llmClient.chat(promptBuilder.buildTestCasePrompt(workContent, diff));
        history.setTestCase(testCase);
        return toResponse(history);
    }

    /** 연동된 커밋(project@hash)들의 변경분을 합쳐서 반환 (길면 잘라 속도 보호). */
    private String buildLinkedDiff(ReleaseHistory history) {
        List<GitCommitRef> refs = parseHashes(history.getGitCommitHashes()).stream()
                .map(GitCommitRef::parse)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        for (GitCommitRef ref : refs) {
            String d = gitProvider.commitDiff(history.getGitSystem(), ref.project(), ref.hash());
            if (d != null && !d.isBlank()) {
                sb.append("===== commit ").append(ref.toToken()).append(" =====\n")
                        .append(d).append("\n\n");
            }
            if (sb.length() > TEST_CASE_DIFF_LIMIT) {
                break;
            }
        }
        return sb.length() > TEST_CASE_DIFF_LIMIT ? sb.substring(0, TEST_CASE_DIFF_LIMIT) : sb.toString();
    }

    /** SR 에 연동된 사이드이펙트 검토 결과(최신 1건) 조회. 없으면 exists=false. */
    public SideEffectReportResponse getSideEffectReport(Long id) {
        Document doc = documentRepository
                .findFirstByRefTypeAndRefIdAndTypeOrderByCreatedAtDesc(REF_TYPE_HISTORY, id, "SIDE_EFFECT");
        if (doc == null) {
            return SideEffectReportResponse.builder().exists(false).build();
        }
        return SideEffectReportResponse.builder()
                .exists(true)
                .content(doc.getContent())
                .createdAt(doc.getCreatedAt())
                .build();
    }

    /** 콤마 구분 해시 문자열 → 공백/중복 제거된 리스트 */
    private List<String> parseHashes(String commitHashes) {
        if (commitHashes == null || commitHashes.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(commitHashes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    /** 주어진 반영 이력 ID 중 장애가 등록된 것들의 ID 집합 */
    private Set<Long> historyIdsWithIncident(List<Long> historyIds) {
        if (historyIds.isEmpty()) {
            return Set.of();
        }
        return incidentRepository.findByReleaseHistoryIdIn(historyIds).stream()
                .map(i -> i.getReleaseHistory().getId())
                .collect(Collectors.toSet());
    }

    /** 주어진 반영 이력 ID 중 사이드이펙트 리포트가 있는 것들의 ID 집합 */
    private Set<Long> historyIdsWithReport(List<Long> historyIds) {
        if (historyIds.isEmpty()) {
            return Set.of();
        }
        return documentRepository
                .findByRefTypeAndRefIdInAndType(REF_TYPE_HISTORY, historyIds, "SIDE_EFFECT").stream()
                .map(Document::getRefId)
                .collect(Collectors.toSet());
    }

    private ReleaseHistoryResponse toResponse(ReleaseHistory history) {
        boolean hasIncident = !incidentRepository
                .findByReleaseHistoryIdOrderByOccurredAtDesc(history.getId()).isEmpty();
        boolean hasReport = documentRepository.findFirstByRefTypeAndRefIdAndTypeOrderByCreatedAtDesc(
                REF_TYPE_HISTORY, history.getId(), "SIDE_EFFECT") != null;
        return toResponse(history, hasIncident, hasReport);
    }

    private ReleaseHistoryResponse toResponse(ReleaseHistory history, boolean incidentRegistered,
                                              boolean hasSideEffectReport) {
        return ReleaseHistoryResponse.builder()
                .id(history.getId())
                .releasePlanId(history.getReleasePlan().getId())
                .srNumber(history.getSrNumber())
                .service(history.getService())
                .workContent(history.getWorkContent())
                .requester(history.getRequester())
                .worker(history.getWorker())
                .testUrlVerify(history.getTestUrlVerify())
                .testUrlProd(history.getTestUrlProd())
                .testDetail(history.getTestDetail())
                .testCase(history.getTestCase())
                .frontendChanged(history.getFrontendChanged())
                .backendChanged(history.getBackendChanged())
                .note(history.getNote())
                .finalConfirmed(history.getFinalConfirmed())
                .gitSystem(history.getGitSystem())
                .gitCommitHashes(parseHashes(history.getGitCommitHashes()))
                .incidentRegistered(incidentRegistered)
                .hasSideEffectReport(hasSideEffectReport)
                .createdAt(history.getCreatedAt())
                .build();
    }
}
