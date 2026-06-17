package com.incident.management.git;

/**
 * 선택된 커밋 참조. project + hash 로 어느 저장소의 어느 커밋인지 식별한다.
 * 저장 포맷은 "project@hash" (로컬 모드는 project 가 비어 "hash" 만).
 */
public record GitCommitRef(String project, String hash) {

    private static final String SEP = "@";

    /** "project@hash" 또는 "hash" 토큰을 파싱. @ 가 없으면 project 는 null. */
    public static GitCommitRef parse(String token) {
        if (token == null) {
            return null;
        }
        String t = token.trim();
        if (t.isEmpty()) {
            return null;
        }
        int idx = t.lastIndexOf(SEP);
        if (idx < 0) {
            return new GitCommitRef(null, t);
        }
        String project = t.substring(0, idx);
        String hash = t.substring(idx + 1);
        return new GitCommitRef(project.isEmpty() ? null : project, hash);
    }

    /** "project@hash" 토큰으로 직렬화. project 가 없으면 "hash" 만. */
    public String toToken() {
        return (project == null || project.isBlank()) ? hash : project + SEP + hash;
    }
}
