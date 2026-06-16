package com.incident.management.controller;

import com.incident.management.common.GitAdapter;
import com.incident.management.service.GitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/git")
@RequiredArgsConstructor
public class GitController {

    private final GitService gitService;

    /** 최근 커밋 목록 조회 (체크박스 매핑용). 저장소는 system 키로 백엔드에서 분기 */
    @GetMapping("/commits")
    public ResponseEntity<List<GitAdapter.Commit>> getCommits(
            @RequestParam(required = false) String system,
            @RequestParam(defaultValue = "30") int count) {
        return ResponseEntity.ok(gitService.getCommits(system, count));
    }

    /** 설정된 시스템(저장소) 키 목록 */
    @GetMapping("/systems")
    public ResponseEntity<Set<String>> getSystems() {
        return ResponseEntity.ok(gitService.getSystems());
    }
}
