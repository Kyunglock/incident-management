package com.incident.management.repository;

import com.incident.management.entity.IncidentDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentDocumentRepository extends JpaRepository<IncidentDocument, Long> {
    List<IncidentDocument> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);
    Optional<IncidentDocument> findByIdAndIncidentId(Long id, Long incidentId);
}
