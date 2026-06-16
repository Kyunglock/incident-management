package com.incident.management.service;

import com.incident.management.dto.response.ReleaseHistoryResponse;
import com.incident.management.entity.ReleaseHistory;
import com.incident.management.entity.ReleasePlan;
import com.incident.management.exception.ResourceNotFoundException;
import com.incident.management.repository.ReleaseHistoryRepository;
import com.incident.management.repository.ReleasePlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReleaseHistoryService {

    private final ReleaseHistoryRepository releaseHistoryRepository;
    private final ReleasePlanRepository releasePlanRepository;

    @Transactional
    public ReleaseHistoryResponse create(Long releasePlanId, LocalDateTime deployedAt, String memo) {
        ReleasePlan plan = releasePlanRepository.findById(releasePlanId)
                .orElseThrow(() -> new ResourceNotFoundException("반영 계획서를 찾을 수 없습니다: " + releasePlanId));

        ReleaseHistory history = ReleaseHistory.builder()
                .releasePlan(plan)
                .deployedAt(deployedAt != null ? deployedAt : LocalDateTime.now())
                .memo(memo)
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
    public ReleaseHistoryResponse updateStatus(Long id, ReleaseHistory.Status status) {
        ReleaseHistory history = releaseHistoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("반영 이력을 찾을 수 없습니다: " + id));
        history.setStatus(status);
        return toResponse(history);
    }

    private ReleaseHistoryResponse toResponse(ReleaseHistory history) {
        return ReleaseHistoryResponse.builder()
                .id(history.getId())
                .releasePlanId(history.getReleasePlan().getId())
                .deployedAt(history.getDeployedAt())
                .status(history.getStatus())
                .memo(history.getMemo())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
