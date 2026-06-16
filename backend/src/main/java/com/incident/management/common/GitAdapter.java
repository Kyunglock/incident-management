package com.incident.management.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
@Slf4j
public class GitAdapter {

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
