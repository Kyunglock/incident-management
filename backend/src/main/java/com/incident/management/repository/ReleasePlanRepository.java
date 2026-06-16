package com.incident.management.repository;

import com.incident.management.entity.ReleasePlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReleasePlanRepository extends JpaRepository<ReleasePlan, Long> {
    List<ReleasePlan> findAllByOrderByCreatedAtDesc();
}
