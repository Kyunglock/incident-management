package com.incident.management.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incident.management.common.GitCommit;
import com.incident.management.config.GitProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * GitLab REST API(/api/v4) 연동 구현. git.provider=gitlab 일 때 활성화된다.
 * system 하나가 여러 프로젝트에 매핑되면 커밋 목록을 병합해 반환한다.
 */
@Component
@ConditionalOnProperty(name = "git.provider", havingValue = "gitlab")
@Slf4j
public class GitLabApiProvider implements GitProvider {

    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WebClient webClient;
    private final GitProperties gitProperties;
    private final ObjectMapper objectMapper;

    public GitLabApiProvider(WebClient webClient, GitProperties gitProperties, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.gitProperties = gitProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Set<String> systems() {
        return gitProperties.getGitlab().getSystems().keySet();
    }

    @Override
    public List<GitCommit> listCommits(String system, int count) {
        List<String> projects = gitProperties.getGitlab().resolveProjects(system);
        if (projects.isEmpty()) {
            log.warn("GitLab system 에 매핑된 프로젝트가 없습니다. system={}", system);
            return List.of();
        }
        String branch = gitProperties.getGitlab().getBranch();
        List<Dated> collected = new ArrayList<>();
        for (String project : projects) {
            String token = tokenFor(project);
            if (token == null) {
                continue;
            }
            String url = apiBase(project) + "/repository/commits?per_page=" + count
                    + (branch != null && !branch.isBlank() ? "&ref_name=" + branch : "");
            try {
                JsonNode arr = getJson(url, token);
                int n = (arr != null && arr.isArray()) ? arr.size() : -1;
                log.info("GitLab 커밋 조회 - project={} branch={} 건수={} url={}", project, branch, n, url);
                if (arr == null || !arr.isArray()) {
                    continue;
                }
                for (JsonNode node : arr) {
                    String committedDate = node.path("committed_date").asText("");
                    collected.add(new Dated(parseInstant(committedDate),
                            new GitCommit(
                                    project,
                                    node.path("id").asText(""),
                                    node.path("author_name").asText(""),
                                    formatDate(committedDate),
                                    node.path("title").asText(""))));
                }
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                log.warn("GitLab 커밋 조회 실패(HTTP). project={} status={} body={} url={}",
                        project, e.getStatusCode(), e.getResponseBodyAsString(), url);
            } catch (Exception e) {
                log.warn("GitLab 커밋 조회 실패. project={} url={}", project, url, e);
            }
        }
        // 프로젝트마다 count 개씩 가져온 뒤 최신순으로 병합한다.
        // (여기서 다시 count 로 자르면 특정 저장소가 통째로 누락될 수 있어 자르지 않는다.)
        return collected.stream()
                .sorted(Comparator.comparing((Dated d) -> d.instant).reversed())
                .map(d -> d.commit)
                .toList();
    }

    @Override
    public String commitMessages(String system, int count) {
        StringBuilder sb = new StringBuilder();
        for (GitCommit c : listCommits(system, count)) {
            String shortHash = c.hash().length() >= 8 ? c.hash().substring(0, 8) : c.hash();
            sb.append(shortHash).append(' ').append(c.message()).append('\n');
        }
        return sb.toString();
    }

    @Override
    public String commitDiff(String system, String project, String hash) {
        if (hash == null || hash.isBlank()) {
            return "";
        }
        String proj = resolveProject(system, project);
        if (proj == null) {
            return "";
        }
        String token = tokenFor(proj);
        if (token == null) {
            return "";
        }
        try {
            JsonNode arr = getJson(apiBase(proj) + "/repository/commits/"
                    + URLEncoder.encode(hash, StandardCharsets.UTF_8) + "/diff", token);
            return joinDiffs(arr);
        } catch (Exception e) {
            log.warn("GitLab 커밋 diff 조회 실패. project={} hash={}", proj, hash, e);
            return "";
        }
    }

    @Override
    public String rangeDiff(String system, String project, String from, String to) {
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            return "";
        }
        String proj = resolveProject(system, project);
        if (proj == null) {
            return "";
        }
        String token = tokenFor(proj);
        if (token == null) {
            return "";
        }
        try {
            String url = apiBase(proj) + "/repository/compare?from="
                    + URLEncoder.encode(from, StandardCharsets.UTF_8)
                    + "&to=" + URLEncoder.encode(to, StandardCharsets.UTF_8);
            JsonNode root = getJson(url, token);
            return root == null ? "" : joinDiffs(root.path("diffs"));
        } catch (Exception e) {
            log.warn("GitLab 범위 diff 조회 실패. project={} {}..{}", proj, from, to, e);
            return "";
        }
    }

    // --- 내부 유틸 ---

    /** 프로젝트가 비면 system 의 첫 프로젝트로 폴백. */
    private String resolveProject(String system, String project) {
        if (project != null && !project.isBlank()) {
            return project;
        }
        List<String> projects = gitProperties.getGitlab().resolveProjects(system);
        return projects.isEmpty() ? null : projects.get(0);
    }

    /** 프로젝트별 토큰을 조회. 없으면 warn 후 null (해당 프로젝트는 건너뜀). */
    private String tokenFor(String project) {
        String token = gitProperties.getGitlab().resolveToken(project);
        if (token == null || token.isBlank()) {
            log.warn("GitLab 토큰이 설정되지 않아 프로젝트를 건너뜁니다. project={}", project);
            return null;
        }
        return token;
    }

    private String apiBase(String project) {
        String base = gitProperties.getGitlab().getBaseUrl();
        if (base != null && base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/api/v4/projects/" + URLEncoder.encode(project, StandardCharsets.UTF_8);
    }

    private JsonNode getJson(String url, String token) throws Exception {
        String body = webClient.get()
                .uri(URI.create(url))
                .header("PRIVATE-TOKEN", token)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return body == null ? null : objectMapper.readTree(body);
    }

    /** GitLab diff 배열({old_path,new_path,diff})을 unified diff 문자열로 합친다. */
    private String joinDiffs(JsonNode diffs) {
        if (diffs == null || !diffs.isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode d : diffs) {
            String newPath = d.path("new_path").asText("");
            String oldPath = d.path("old_path").asText("");
            sb.append("diff --git a/").append(oldPath).append(" b/").append(newPath).append('\n');
            sb.append(d.path("diff").asText("")).append('\n');
        }
        return sb.toString();
    }

    private OffsetDateTime parseInstant(String iso) {
        try {
            return OffsetDateTime.parse(iso);
        } catch (Exception e) {
            return OffsetDateTime.MIN;
        }
    }

    private String formatDate(String iso) {
        try {
            return OffsetDateTime.parse(iso).format(DISPLAY_DATE);
        } catch (Exception e) {
            return iso;
        }
    }

    /** 정렬용: 커밋 + 시각 */
    private record Dated(OffsetDateTime instant, GitCommit commit) {}
}
