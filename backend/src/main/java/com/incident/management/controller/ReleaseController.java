package com.incident.management.controller;

import com.incident.management.dto.response.PageResponse;
import com.incident.management.dto.response.ReleasePlanImportResponse;
import com.incident.management.dto.response.ReleasePlanResponse;
import com.incident.management.service.ReleasePlanService;
import com.incident.management.service.SideEffectService;
import com.incident.management.service.VulnCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/release-plans")
@RequiredArgsConstructor
public class ReleaseController {

    private final ReleasePlanService releasePlanService;
    private final SideEffectService sideEffectService;
    private final VulnCheckService vulnCheckService;

    @PostMapping
    public ResponseEntity<ReleasePlanResponse> generatePlan(
            @RequestParam MultipartFile excelFile,
            @RequestParam(defaultValue = "false") boolean useGit,
            @RequestParam(required = false) String system,
            @RequestParam(required = false) String commitFrom,
            @RequestParam(required = false) String commitTo,
            @RequestParam(required = false) String releaseTitle) {
        ReleasePlanResponse response = releasePlanService.generatePlan(
                excelFile, useGit, system, commitFrom, commitTo, releaseTitle);
        return ResponseEntity.ok(response);
    }

    /**
     * 다중 시트 엑셀 업로드 → 시트(날짜)별 작업 계획서 + SR 단위 반영 이력 일괄 생성.
     * 이미 같은 날짜(2026-MM-DD)가 존재하면 해당 시트는 무시한다.
     */
    @PostMapping("/import")
    public ResponseEntity<ReleasePlanImportResponse> importPlans(
            @RequestParam MultipartFile excelFile,
            @RequestParam(defaultValue = "true") boolean summarize) {
        return ResponseEntity.ok(releasePlanService.importFromExcel(excelFile, summarize));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ReleasePlanResponse>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(releasePlanService.getAll(keyword, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReleasePlanResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(releasePlanService.getById(id));
    }

    /** 반영이력 기반 작업계획서 '작업내용' 텍스트 생성 (한글파일 붙여넣기용) */
    @PostMapping("/{id}/work-plan")
    public ResponseEntity<Map<String, String>> generateWorkPlan(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("content", releasePlanService.generateWorkPlanContent(id)));
    }

    /** 작업 계획서와 하위(반영 이력/장애/장애 분석)를 함께 삭제한다. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        releasePlanService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/side-effect")
    public ResponseEntity<Map<String, String>> analyzeSideEffect(
            @PathVariable Long id,
            @RequestParam String system,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String commitFrom,
            @RequestParam(required = false) String commitTo) {
        SideEffectService.AnalysisResult result =
                sideEffectService.analyze(system, project, commitFrom, commitTo, "RELEASE_PLAN", id);
        return ResponseEntity.ok(Map.of("content", result.content()));
    }

    @PostMapping("/{id}/vuln-check")
    public ResponseEntity<Map<String, String>> analyzeVuln(
            @PathVariable Long id,
            @RequestParam String system,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) String commitFrom,
            @RequestParam(required = false) String commitTo) {
        String docPath = vulnCheckService.analyze(system, project, commitFrom, commitTo, id);
        return ResponseEntity.ok(Map.of("docPath", docPath));
    }
}
