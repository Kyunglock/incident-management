package com.incident.management.controller;

import com.incident.management.dto.request.CreateReleaseHistoryRequest;
import com.incident.management.dto.response.ReleaseHistoryResponse;
import com.incident.management.service.ReleaseHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReleaseHistoryController {

    private final ReleaseHistoryService releaseHistoryService;

    @PostMapping("/api/release-plans/{planId}/histories")
    public ResponseEntity<ReleaseHistoryResponse> create(
            @PathVariable Long planId,
            @RequestBody CreateReleaseHistoryRequest request) {
        return ResponseEntity.ok(releaseHistoryService.create(planId, request));
    }

    @GetMapping("/api/release-plans/{planId}/histories")
    public ResponseEntity<List<ReleaseHistoryResponse>> getByPlan(@PathVariable Long planId) {
        return ResponseEntity.ok(releaseHistoryService.getByPlan(planId));
    }

    @GetMapping("/api/release-histories/{id}")
    public ResponseEntity<ReleaseHistoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(releaseHistoryService.getById(id));
    }

    @PatchMapping("/api/release-histories/{id}/final-confirm")
    public ResponseEntity<ReleaseHistoryResponse> updateFinalConfirmed(
            @PathVariable Long id,
            @RequestParam boolean finalConfirmed) {
        return ResponseEntity.ok(releaseHistoryService.updateFinalConfirmed(id, finalConfirmed));
    }

    @PatchMapping("/api/release-histories/{id}/sr-number")
    public ResponseEntity<ReleaseHistoryResponse> updateSrNumber(
            @PathVariable Long id,
            @RequestParam(required = false) String srNumber) {
        return ResponseEntity.ok(releaseHistoryService.updateSrNumber(id, srNumber));
    }
}
