package com.incident.management.service;

import com.incident.management.dto.response.DashboardStatsResponse;
import com.incident.management.dto.response.IncidentResponse;
import com.incident.management.entity.Incident;
import com.incident.management.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final IncidentRepository incidentRepository;

    public DashboardStatsResponse getStats() {
        long totalOpen = incidentRepository.countByStatus(Incident.IncidentStatus.OPEN);
        long totalInProgress = incidentRepository.countByStatus(Incident.IncidentStatus.IN_PROGRESS);
        long totalResolved = incidentRepository.countByStatus(Incident.IncidentStatus.RESOLVED);
        long totalClosed = incidentRepository.countByStatus(Incident.IncidentStatus.CLOSED);

        List<IncidentResponse> recentIncidents = incidentRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(IncidentResponse::from)
                .toList();

        return new DashboardStatsResponse(totalOpen, totalInProgress, totalResolved, totalClosed, recentIncidents);
    }
}
