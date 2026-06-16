package com.incident.management.service;

import com.incident.management.dto.request.CreateReleaseHistoryRequest;
import com.incident.management.dto.response.ReleaseHistoryResponse;
import com.incident.management.entity.CommitRef;
import com.incident.management.entity.ReleaseHistory;
import com.incident.management.entity.ReleasePlan;
import com.incident.management.exception.ResourceNotFoundException;
import com.incident.management.repository.ReleaseHistoryRepository;
import com.incident.management.repository.ReleasePlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReleaseHistoryService {

    private final ReleaseHistoryRepository releaseHistoryRepository;
    private final ReleasePlanRepository releasePlanRepository;

    @Transactional
    public ReleaseHistoryResponse create(Long releasePlanId, CreateReleaseHistoryRequest request) {
        ReleasePlan plan = releasePlanRepository.findById(releasePlanId)
                .orElseThrow(() -> new ResourceNotFoundException("반영 계획서를 찾을 수 없습니다: " + releasePlanId));

        List<CommitRef> commits = request.getCommits() == null ? new ArrayList<>()
                : request.getCommits().stream()
                        .map(c -> CommitRef.builder()
                                .hash(c.getHash())
                                .author(c.getAuthor())
                                .date(c.getDate())
                                .message(c.getMessage())
                                .build())
                        .collect(Collectors.toList());

        ReleaseHistory history = ReleaseHistory.builder()
                .releasePlan(plan)
                .service(request.getService())
                .workContent(request.getWorkContent())
                .requester(request.getRequester())
                .worker(request.getWorker())
                .testUrlVerify(request.getTestUrlVerify())
                .testUrlProd(request.getTestUrlProd())
                .testDetail(request.getTestDetail())
                .frontendChanged(request.getFrontendChanged())
                .backendChanged(request.getBackendChanged())
                .note(request.getNote())
                .commits(commits)
                .build();
        history = releaseHistoryRepository.save(history);
        return toResponse(history);
    }

    public List<ReleaseHistoryResponse> getByPlan(Long releasePlanId) {
        return releaseHistoryRepository.findByReleasePlanIdOrderByCreatedAtDesc(releasePlanId).stream()
                .map(this::toResponse)
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

    private ReleaseHistoryResponse toResponse(ReleaseHistory history) {
        return ReleaseHistoryResponse.builder()
                .id(history.getId())
                .releasePlanId(history.getReleasePlan().getId())
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
                .commits(history.getCommits())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
