package com.incident.management.service;

import com.incident.management.entity.ReleaseHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 레드마인 연동. SR 번호(이슈 번호)로 이슈 정보를 조회해
 * 반영 이력의 서비스/작업내용/요청자/작업자/TEST URL 등 상세 정보를 채운다.
 *
 * TODO: 실제 레드마인 REST API 연동 구현 (base-url, api-key 설정 후
 *       GET /issues/{srNumber}.json 호출 → 필드 매핑).
 */
@Service
@Slf4j
public class RedmineService {

    /** SR 번호로 레드마인 이슈 정보를 조회해 반영 이력을 보강한다. */
    public void enrich(ReleaseHistory history) {
        String srNumber = history.getSrNumber();
        if (srNumber == null || srNumber.isBlank()) {
            return;
        }
        // TODO: 레드마인 API 연동 전까지는 보강하지 않는다.
        log.debug("레드마인 연동 미구현 - SR {} 상세 정보는 추후 연동 시 채워집니다.", srNumber);
    }
}
