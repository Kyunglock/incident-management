package com.incident.management.repository;

import com.incident.management.entity.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Page<Incident> findByStatus(Incident.IncidentStatus status, Pageable pageable);

    @Query("SELECT i FROM Incident i ORDER BY i.createdAt DESC")
    List<Incident> findTop5ByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(Incident.IncidentStatus status);
}
