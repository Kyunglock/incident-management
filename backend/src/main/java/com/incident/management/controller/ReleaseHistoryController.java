package com.incident.management.controller;

import com.incident.management.dto.response.ReleaseHistoryResponse;
import com.incident.management.entity.ReleaseHistory;
import com.incident.management.service.ReleaseHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReleaseHistoryController {

    private final ReleaseHistoryService releaseHistoryService;

    @PostMapping("/api/release-plans/{planId}/histories")
    public ResponseEntity<ReleaseHistoryResponse> create(
            @PathVariable Long planId,
            @RequestParam(required = false) LocalDateTime deployedAt,
            @RequestParam(required = false) String memo) {
        return ResponseEntity.ok(releaseHistoryService.create(planId, deployedAt, memo));
    }

    @GetMapping("/api/release-plans/{planId}/histories")
    public ResponseEntity<List<ReleaseHistoryResponse>> getByPlan(@PathVariable Long planId) {
        return ResponseEntity.ok(releaseHistoryService.getByPlan(planId));
    }

    @GetMapping("/api/release-histories/{id}")
    public ResponseEntity<ReleaseHistoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(releaseHistoryService.getById(id));
    }

    @PatchMapping("/api/release-histories/{id}/status")
    public ResponseEntity<ReleaseHistoryResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam ReleaseHistory.Status status) {
        return ResponseEntity.ok(releaseHistoryService.updateStatus(id, status));
    }
}
