package com.incident.management.repository;

import com.incident.management.entity.IncidentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentAnalysisRepository extends JpaRepository<IncidentAnalysis, Long> {
    List<IncidentAnalysis> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);
}
