package com.incident.management.repository;

import com.incident.management.entity.ReleaseHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReleaseHistoryRepository extends JpaRepository<ReleaseHistory, Long> {
    List<ReleaseHistory> findByReleasePlanIdOrderByCreatedAtDesc(Long releasePlanId);

    void deleteByReleasePlanId(Long releasePlanId);
}
