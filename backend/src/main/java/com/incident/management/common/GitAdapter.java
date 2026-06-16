package com.incident.management.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GitAdapter {

    /** 필드 구분자: unit separator (메시지에 특수문자가 있어도 안전하게 파싱) */
    private static final String SEP = "";

    /** 커밋 한 건의 메타 정보 */
    public record Commit(String hash, String author, String date, String message) {}

    /** 최근 커밋 목록을 구조화해서 반환 (체크박스 매핑용) */
    public List<Commit> getCommitList(String repoPath, int count) {
        String format = "%H" + SEP + "%an" + SEP + "%ad" + SEP + "%s";
        String raw = runGit(repoPath, "git", "log", "-" + count,
                "--date=format:%Y-%m-%d %H:%M", "--pretty=format:" + format);
        List<Commit> commits = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return commits;
        }
        for (String line : raw.split("\n")) {
            String[] parts = line.split(SEP, -1);
            if (parts.length < 4) {
                continue;
            }
            commits.add(new Commit(parts[0], parts[1], parts[2], parts[3]));
        }
        return commits;
    }

    public String getCommitMessages(String repoPath, int count) {
        return runGit(repoPath, "git", "log", "--oneline", "-" + count);
    }

    public String getDiff(String repoPath, String fromCommit, String toCommit) {
        if (fromCommit == null || fromCommit.isBlank()) {
            return runGit(repoPath, "git", "diff", "HEAD~1", "HEAD");
        }
        return runGit(repoPath, "git", "diff", fromCommit, toCommit);
    }

    public String getDiffStatOnly(String repoPath, String fromCommit, String toCommit) {
        if (fromCommit == null || fromCommit.isBlank()) {
            return runGit(repoPath, "git", "diff", "--stat", "HEAD~1", "HEAD");
        }
        return runGit(repoPath, "git", "diff", "--stat", fromCommit, toCommit);
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
