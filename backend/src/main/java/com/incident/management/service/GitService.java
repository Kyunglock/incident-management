package com.incident.management.service;

import com.incident.management.common.GitCommit;
import com.incident.management.git.GitProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitService {

    private final GitProvider gitProvider;

    /** system 키로 저장소(들)를 분기해 최근 커밋 목록을 반환 */
    public List<GitCommit> getCommits(String system, int count) {
        return gitProvider.listCommits(system, count);
    }

    /** 설정된 시스템 목록 (프론트 드롭다운용) */
    public Set<String> getSystems() {
        return gitProvider.systems();
    }
}
