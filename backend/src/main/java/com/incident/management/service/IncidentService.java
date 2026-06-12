package com.incident.management.service;

import com.incident.management.dto.request.CreateIncidentActionRequest;
import com.incident.management.dto.request.CreateIncidentRequest;
import com.incident.management.dto.request.UpdateIncidentRequest;
import com.incident.management.dto.response.IncidentActionResponse;
import com.incident.management.dto.response.IncidentDetailResponse;
import com.incident.management.dto.response.IncidentResponse;
import com.incident.management.entity.Incident;
import com.incident.management.entity.IncidentAction;
import com.incident.management.exception.ResourceNotFoundException;
import com.incident.management.repository.IncidentActionRepository;
import com.incident.management.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentActionRepository incidentActionRepository;

    public Page<IncidentResponse> getIncidents(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (status != null && !status.isBlank()) {
            Incident.IncidentStatus incidentStatus = Incident.IncidentStatus.valueOf(status.toUpperCase());
            return incidentRepository.findByStatus(incidentStatus, pageable)
                    .map(IncidentResponse::from);
        }
        return incidentRepository.findAll(pageable).map(IncidentResponse::from);
    }

    public IncidentDetailResponse getIncidentDetail(Long id) {
        Incident incident = findIncidentById(id);
        List<IncidentActionResponse> actions = incidentActionRepository
                .findByIncidentIdOrderByPerformedAtDesc(id)
                .stream().map(IncidentActionResponse::from).toList();
        return IncidentDetailResponse.from(incident, actions);
    }

    @Transactional
    public IncidentResponse createIncident(CreateIncidentRequest request) {
        Incident incident = Incident.builder()
                .title(request.title())
                .description(request.description())
                .reporterName(request.reporterName())
                .assigneeName(request.assigneeName())
                .build();

        if (request.status() != null) {
            incident.setStatus(Incident.IncidentStatus.valueOf(request.status().toUpperCase()));
        }
        if (request.priority() != null) {
            incident.setPriority(Incident.Priority.valueOf(request.priority().toUpperCase()));
        }

        return IncidentResponse.from(incidentRepository.save(incident));
    }

    @Transactional
    public IncidentResponse updateIncident(Long id, UpdateIncidentRequest request) {
        Incident incident = findIncidentById(id);

        if (request.title() != null) incident.setTitle(request.title());
        if (request.description() != null) incident.setDescription(request.description());
        if (request.reporterName() != null) incident.setReporterName(request.reporterName());
        if (request.assigneeName() != null) incident.setAssigneeName(request.assigneeName());
        if (request.status() != null) {
            Incident.IncidentStatus newStatus = Incident.IncidentStatus.valueOf(request.status().toUpperCase());
            incident.setStatus(newStatus);
            if (newStatus == Incident.IncidentStatus.RESOLVED && incident.getResolvedAt() == null) {
                incident.setResolvedAt(LocalDateTime.now());
            }
        }
        if (request.priority() != null) {
            incident.setPriority(Incident.Priority.valueOf(request.priority().toUpperCase()));
        }

        return IncidentResponse.from(incidentRepository.save(incident));
    }

    @Transactional
    public IncidentResponse updateIncidentStatus(Long id, String status) {
        Incident incident = findIncidentById(id);
        Incident.IncidentStatus newStatus = Incident.IncidentStatus.valueOf(status.toUpperCase());
        incident.setStatus(newStatus);
        if (newStatus == Incident.IncidentStatus.RESOLVED && incident.getResolvedAt() == null) {
            incident.setResolvedAt(LocalDateTime.now());
        }
        return IncidentResponse.from(incidentRepository.save(incident));
    }

    public List<IncidentActionResponse> getIncidentActions(Long incidentId) {
        findIncidentById(incidentId);
        return incidentActionRepository.findByIncidentIdOrderByPerformedAtDesc(incidentId)
                .stream().map(IncidentActionResponse::from).toList();
    }

    @Transactional
    public IncidentActionResponse addIncidentAction(Long incidentId, CreateIncidentActionRequest request) {
        Incident incident = findIncidentById(incidentId);

        IncidentAction action = IncidentAction.builder()
                .incident(incident)
                .actionDescription(request.actionDescription())
                .actionType(IncidentAction.ActionType.valueOf(request.actionType().toUpperCase()))
                .performedBy(request.performedBy())
                .performedAt(request.performedAt() != null ? request.performedAt() : LocalDateTime.now())
                .build();

        return IncidentActionResponse.from(incidentActionRepository.save(action));
    }

    private Incident findIncidentById(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("장애를 찾을 수 없습니다. ID: " + id));
    }
}
