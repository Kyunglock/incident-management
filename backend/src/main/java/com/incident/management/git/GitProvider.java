package com.incident.management.git;

import com.incident.management.common.GitCommit;

import java.util.List;
import java.util.Set;

/**
 * git 연동 추상화. 구현체는 설정(git.provider)에 따라 하나만 활성화된다.
 * - LocalGitProvider: 로컬 저장소에서 git CLI 실행
 * - GitLabApiProvider: GitLab REST API 호출
 */
public interface GitProvider {

    /** 설정된 system(저장소 묶음) 키 목록 (프론트 드롭다운용) */
    Set<String> systems();

    /** system 의 최근 커밋 목록. system 이 여러 프로젝트에 매핑되면 병합해 반환. */
    List<GitCommit> listCommits(String system, int count);

    /** 작업계획서 생성용 oneline 커밋 메시지 모음 */
    String commitMessages(String system, int count);

    /** 단일 커밋의 부모 대비 변경분(diff) */
    String commitDiff(String system, String project, String hash);

    /** from..to 범위 diff */
    String rangeDiff(String system, String project, String from, String to);
}
