package com.incident.management.repository;

import com.incident.management.entity.ReleasePlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReleasePlanRepository extends JpaRepository<ReleasePlan, Long> {
    Page<ReleasePlan> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    /** 같은 날짜(제목)의 반영 계획서가 이미 존재하는지 확인한다. */
    boolean existsByTitle(String title);
}
