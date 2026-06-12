package com.incident.management.repository;

import com.incident.management.entity.IncidentAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentActionRepository extends JpaRepository<IncidentAction, Long> {
    List<IncidentAction> findByIncidentIdOrderByPerformedAtDesc(Long incidentId);
}
