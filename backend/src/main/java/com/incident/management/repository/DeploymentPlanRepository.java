package com.incident.management.repository;

import com.incident.management.entity.DeploymentPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeploymentPlanRepository extends JpaRepository<DeploymentPlan, Long> {
    List<DeploymentPlan> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);
    Optional<DeploymentPlan> findByIdAndIncidentId(Long id, Long incidentId);
}
