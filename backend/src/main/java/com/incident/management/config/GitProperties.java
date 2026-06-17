package com.incident.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * git 연동 설정.
 * - provider: local | gitlab (전역 전환, 기본 local)
 * [local 모드]
 * - default-repo: system 미지정 시 사용할 기본 저장소 경로
 * - repositories: 시스템(애플리케이션)별 로컬 저장소 경로 매핑
 *   예) git.repositories.member=/repos/member-service
 * [gitlab 모드]
 * - gitlab.base-url / token / branch
 * - gitlab.systems: system 키 → GitLab 프로젝트 경로 목록 (1:N)
 *   예) git.gitlab.systems.에듀넷[0]=root/cnedu-front-edunet
 */
@Component
@ConfigurationProperties(prefix = "git")
@Data
public class GitProperties {

    /** local | gitlab */
    private String provider = "local";

    private String defaultRepo;
    private Map<String, String> repositories = new HashMap<>();

    private Gitlab gitlab = new Gitlab();

    /** [local] system 키로 저장소 경로를 분기. 없으면 기본 저장소로 폴백. */
    public String resolveRepoPath(String system) {
        if (system != null && repositories.containsKey(system)) {
            return repositories.get(system);
        }
        return defaultRepo;
    }

    @Data
    public static class Gitlab {
        private String baseUrl;
        private String token;
        private String branch = "main";
        /** system 키 → 프로젝트 경로 목록 */
        private Map<String, List<String>> systems = new HashMap<>();

        /** system 의 프로젝트 경로 목록. 없으면 빈 목록. */
        public List<String> resolveProjects(String system) {
            if (system != null && systems.containsKey(system)) {
                return systems.get(system);
            }
            return new ArrayList<>();
        }
    }
}
