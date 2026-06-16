package com.incident.management.repository;

import com.incident.management.entity.ReleasePlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleasePlanRepository extends JpaRepository<ReleasePlan, Long> {
    Page<ReleasePlan> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
