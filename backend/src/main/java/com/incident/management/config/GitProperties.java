package com.incident.management.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * git 저장소 경로 설정.
 * - default-repo: system 미지정 시 사용할 기본 저장소 경로
 * - repositories: 시스템(애플리케이션)별 저장소 경로 매핑
 *   예) git.repositories.member=/repos/member-service
 */
@Component
@ConfigurationProperties(prefix = "git")
@Data
public class GitProperties {

    private String defaultRepo;
    private Map<String, String> repositories = new HashMap<>();

    /** system 키로 저장소 경로를 분기. 없으면 기본 저장소로 폴백. */
    public String resolveRepoPath(String system) {
        if (system != null && repositories.containsKey(system)) {
            return repositories.get(system);
        }
        return defaultRepo;
    }
}
