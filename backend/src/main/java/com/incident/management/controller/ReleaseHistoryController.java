package com.incident.management.controller;

import com.incident.management.dto.request.CreateReleaseHistoryRequest;
import com.incident.management.dto.response.ReleaseHistoryResponse;
import com.incident.management.dto.response.ReleaseHistorySummaryResponse;
import com.incident.management.dto.response.SideEffectReportResponse;
import com.incident.management.service.ReleaseHistoryService;
import com.incident.management.service.SideEffectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    /** SR 선택용 전체 요약 목록 (장애 등록 드롭다운 등) */
    @GetMapping("/api/release-histories")
    public ResponseEntity<List<ReleaseHistorySummaryResponse>> getAllSummaries() {
        return ResponseEntity.ok(releaseHistoryService.getAllSummaries());
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

    /** SR 에 git 커밋 연동 (commitHashes: 콤마 구분 다중 해시, 비면 연동 해제) */
    @PatchMapping("/api/release-histories/{id}/git-commit")
    public ResponseEntity<ReleaseHistoryResponse> updateGitCommit(
            @PathVariable Long id,
            @RequestParam(required = false) String system,
            @RequestParam(required = false) String commitHashes) {
        return ResponseEntity.ok(
                releaseHistoryService.updateGitCommit(id, system, commitHashes));
    }

    /** 연동된 git 커밋 기준 사이드이펙트 검토 (결과 원문 + docPath 반환) */
    @PostMapping("/api/release-histories/{id}/side-effect")
    public ResponseEntity<Map<String, String>> analyzeSideEffect(@PathVariable Long id) {
        SideEffectService.AnalysisResult result = releaseHistoryService.analyzeSideEffect(id);
        return ResponseEntity.ok(Map.of("content", result.content()));
    }

    /** SR 에 연동된 사이드이펙트 검토 결과(최신) 조회 — 다시 보기용 */
    @GetMapping("/api/release-histories/{id}/side-effect")
    public ResponseEntity<SideEffectReportResponse> getSideEffectReport(@PathVariable Long id) {
        return ResponseEntity.ok(releaseHistoryService.getSideEffectReport(id));
    }

    /** 작업내용 기반 테스트케이스 LLM 자동 생성 */
    @PostMapping("/api/release-histories/{id}/test-cases")
    public ResponseEntity<ReleaseHistoryResponse> generateTestCases(@PathVariable Long id) {
        return ResponseEntity.ok(releaseHistoryService.generateTestCases(id));
    }
}
