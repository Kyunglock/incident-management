package com.incident.management.git;

import com.incident.management.common.GitCommit;
import com.incident.management.config.GitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 로컬 저장소에서 git CLI(ProcessBuilder)를 직접 실행하는 구현.
 * git.provider 미지정 또는 local 일 때 활성화된다.
 */
@Component
@ConditionalOnProperty(name = "git.provider", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class LocalGitProvider implements GitProvider {

    /** 필드 구분자: unit separator (메시지에 특수문자가 있어도 안전하게 파싱) */
    private static final String SEP = "";

    private final GitProperties gitProperties;

    @Override
    public Set<String> systems() {
        return gitProperties.getRepositories().keySet();
    }

    @Override
    public List<GitCommit> listCommits(String system, int count) {
        String repoPath = repoPath(system);
        if (repoPath == null) {
            return List.of();
        }
        String format = "%H" + SEP + "%an" + SEP + "%ad" + SEP + "%s";
        String raw = runGit(repoPath, "git", "log", "-" + count,
                "--date=format:%Y-%m-%d %H:%M", "--pretty=format:" + format);
        List<GitCommit> commits = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return commits;
        }
        for (String line : raw.split("\n")) {
            String[] parts = line.split(SEP, -1);
            if (parts.length < 4) {
                continue;
            }
            // 로컬 모드는 단일 저장소이므로 project 는 비워둔다(저장 토큰은 hash 만 → 하위호환).
            commits.add(new GitCommit(null, parts[0], parts[1], parts[2], parts[3]));
        }
        return commits;
    }

    @Override
    public String commitMessages(String system, int count) {
        String repoPath = repoPath(system);
        if (repoPath == null) {
            return "";
        }
        return runGit(repoPath, "git", "log", "--oneline", "-" + count);
    }

    @Override
    public String commitDiff(String system, String project, String hash) {
        String repoPath = repoPath(system);
        if (repoPath == null || hash == null || hash.isBlank()) {
            return "";
        }
        return runGit(repoPath, "git", "diff", hash + "~1", hash);
    }

    @Override
    public String rangeDiff(String system, String project, String from, String to) {
        String repoPath = repoPath(system);
        if (repoPath == null) {
            return "";
        }
        if (from == null || from.isBlank()) {
            return runGit(repoPath, "git", "diff", "HEAD~1", "HEAD");
        }
        return runGit(repoPath, "git", "diff", from, to);
    }

    private String repoPath(String system) {
        String repoPath = gitProperties.resolveRepoPath(system);
        if (repoPath == null || repoPath.isBlank()) {
            log.warn("git 저장소 경로가 설정되지 않았습니다. system={}", system);
            return null;
        }
        return repoPath;
    }

    private String runGit(String repoPath, String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new java.io.File(repoPath));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            log.warn("git 명령 실행 실패: {}", String.join(" ", command), e);
            return "";
        }
    }
}
