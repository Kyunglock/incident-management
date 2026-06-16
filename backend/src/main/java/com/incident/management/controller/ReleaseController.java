package com.incident.management.controller;

import com.incident.management.dto.response.PageResponse;
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
            @RequestParam(required = false) String repoPath,
            @RequestParam(required = false) String commitFrom,
            @RequestParam(required = false) String commitTo,
            @RequestParam(required = false) String releaseTitle) {
        ReleasePlanResponse response = releasePlanService.generatePlan(
                excelFile, useGit, repoPath, commitFrom, commitTo, releaseTitle);
        return ResponseEntity.ok(response);
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

    @PostMapping("/{id}/side-effect")
    public ResponseEntity<Map<String, String>> analyzeSideEffect(
            @PathVariable Long id,
            @RequestParam String repoPath,
            @RequestParam(required = false) String commitFrom,
            @RequestParam(required = false) String commitTo) {
        String docPath = sideEffectService.analyze(repoPath, commitFrom, commitTo, id);
        return ResponseEntity.ok(Map.of("docPath", docPath));
    }

    @PostMapping("/{id}/vuln-check")
    public ResponseEntity<Map<String, String>> analyzeVuln(
            @PathVariable Long id,
            @RequestParam String repoPath,
            @RequestParam(required = false) String commitFrom,
            @RequestParam(required = false) String commitTo) {
        String docPath = vulnCheckService.analyze(repoPath, commitFrom, commitTo, id);
        return ResponseEntity.ok(Map.of("docPath", docPath));
    }
}
