package com.incident.management.service;

import com.incident.management.common.GitAdapter;
import com.incident.management.config.GitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitService {

    private final GitAdapter gitAdapter;
    private final GitProperties gitProperties;

    /** system 키로 저장소를 분기해 최근 커밋 목록을 반환 */
    public List<GitAdapter.Commit> getCommits(String system, int count) {
        String repoPath = gitProperties.resolveRepoPath(system);
        if (repoPath == null || repoPath.isBlank()) {
            log.warn("git 저장소 경로가 설정되지 않았습니다. system={}", system);
            return List.of();
        }
        return gitAdapter.getCommitList(repoPath, count);
    }

    /** 설정된 시스템 목록 (프론트 드롭다운용) */
    public java.util.Set<String> getSystems() {
        return gitProperties.getRepositories().keySet();
    }
}
