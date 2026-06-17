package com.incident.management.repository;

import com.incident.management.entity.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByReleaseHistoryIdOrderByOccurredAtDesc(Long releaseHistoryId);

    List<Incident> findByReleaseHistoryIdIn(Collection<Long> releaseHistoryIds);

    void deleteByReleaseHistoryIdIn(Collection<Long> releaseHistoryIds);
}
