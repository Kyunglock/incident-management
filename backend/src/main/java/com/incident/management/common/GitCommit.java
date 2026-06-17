package com.incident.management.common;

/**
 * 커밋 한 건의 메타 정보.
 * - project: 커밋이 속한 저장소 식별자. GitLab 모드는 프로젝트 경로(root/cnedu-front-edunet),
 *   로컬 모드는 system 키(또는 repoPath). system 하나가 여러 저장소에 매핑될 때
 *   어느 저장소의 커밋인지 구분하는 데 사용한다.
 */
public record GitCommit(String project, String hash, String author, String date, String message) {}
