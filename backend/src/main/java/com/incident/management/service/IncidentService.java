package com.incident.management.service;

import com.incident.management.dto.response.IncidentResponse;
import com.incident.management.entity.Incident;
import com.incident.management.entity.ReleaseHistory;
import com.incident.management.exception.ResourceNotFoundException;
import com.incident.management.repository.IncidentRepository;
import com.incident.management.repository.ReleaseHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final ReleaseHistoryRepository releaseHistoryRepository;

    @Transactional
    public IncidentResponse create(Long releaseHistoryId, String symptom, LocalDateTime occurredAt) {
        ReleaseHistory history = releaseHistoryRepository.findById(releaseHistoryId)
                .orElseThrow(() -> new ResourceNotFoundException("반영 이력을 찾을 수 없습니다: " + releaseHistoryId));

        Incident incident = Incident.builder()
                .releaseHistory(history)
                .symptom(symptom)
                .occurredAt(occurredAt != null ? occurredAt : LocalDateTime.now())
                .build();
        incident = incidentRepository.save(incident);
        return toResponse(incident);
    }

    public List<IncidentResponse> getByHistory(Long releaseHistoryId) {
        return incidentRepository.findByReleaseHistoryIdOrderByOccurredAtDesc(releaseHistoryId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public IncidentResponse getById(Long id) {
        return toResponse(incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("장애 이력을 찾을 수 없습니다: " + id)));
    }

    private IncidentResponse toResponse(Incident incident) {
        return IncidentResponse.builder()
                .id(incident.getId())
                .releaseHistoryId(incident.getReleaseHistory().getId())
                .occurredAt(incident.getOccurredAt())
                .symptom(incident.getSymptom())
                .createdAt(incident.getCreatedAt())
                .build();
    }
}
