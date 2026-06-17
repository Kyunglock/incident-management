package com.incident.management.service;

import com.incident.management.config.GitProperties;
import com.incident.management.dto.request.CreateReleaseHistoryRequest;
import com.incident.management.dto.response.ReleaseHistoryResponse;
import com.incident.management.entity.ReleaseHistory;
import com.incident.management.entity.ReleasePlan;
import com.incident.management.exception.ResourceNotFoundException;
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
    private final GitProperties gitProperties;
    private final SideEffectService sideEffectService;

    @Transactional
    public ReleaseHistoryResponse create(Long releasePlanId, CreateReleaseHistoryRequest request) {
        if (request.getSrNumber() == null || request.getSrNumber().isBlank()) {
            throw new IllegalArgumentException("SR 번호는 필수입니다.");
        }
        ReleasePlan plan = releasePlanRepository.findById(releasePlanId)
                .orElseThrow(() -> new ResourceNotFoundException("반영 계획서를 찾을 수 없습니다: " + releasePlanId));

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
        // 장애 등록 여부를 한 번에 조회 (N+1 방지)
        Set<Long> withIncident = historyIdsWithIncident(
                histories.stream().map(ReleaseHistory::getId).collect(Collectors.toList()));
        return histories.stream()
                .map(h -> toResponse(h, withIncident.contains(h.getId())))
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

    /** 연동된 git 커밋(들) 기준으로 사이드이펙트 검토를 수행하고 보고서 docPath 를 반환한다. */
    @Transactional
    public String analyzeSideEffect(Long id) {
        ReleaseHistory history = releaseHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("반영 이력을 찾을 수 없습니다: " + id));
        List<String> hashes = parseHashes(history.getGitCommitHashes());
        if (hashes.isEmpty()) {
            throw new IllegalArgumentException("git 커밋이 연동되지 않았습니다. 먼저 커밋을 연동하세요.");
        }
        String repoPath = gitProperties.resolveRepoPath(history.getGitSystem());
        if (repoPath == null || repoPath.isBlank()) {
            throw new IllegalArgumentException("git 저장소 경로가 설정되지 않았습니다.");
        }
        // 연동된 커밋들의 변경분을 합쳐서 분석한다.
        return sideEffectService.analyzeCommits(repoPath, hashes, history.getReleasePlan().getId());
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

    private ReleaseHistoryResponse toResponse(ReleaseHistory history) {
        boolean hasIncident = !incidentRepository
                .findByReleaseHistoryIdOrderByOccurredAtDesc(history.getId()).isEmpty();
        return toResponse(history, hasIncident);
    }

    private ReleaseHistoryResponse toResponse(ReleaseHistory history, boolean incidentRegistered) {
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
                .frontendChanged(history.getFrontendChanged())
                .backendChanged(history.getBackendChanged())
                .note(history.getNote())
                .finalConfirmed(history.getFinalConfirmed())
                .gitSystem(history.getGitSystem())
                .gitCommitHashes(parseHashes(history.getGitCommitHashes()))
                .incidentRegistered(incidentRegistered)
                .createdAt(history.getCreatedAt())
                .build();
    }
}
