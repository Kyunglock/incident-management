package com.incident.management.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReleaseHistoryResponse {
    private Long id;
    private Long releasePlanId;
    private String srNumber;
    private String service;
    private String workContent;
    private String requester;
    private String worker;
    private String testUrlVerify;
    private String testUrlProd;
    private String testDetail;
    private String testCase;
    private Boolean frontendChanged;
    private Boolean backendChanged;
    private String note;
    private Boolean finalConfirmed;
    private String gitSystem;
    /** 연동된 git 커밋 해시 목록 (여러 개 선택 가능) */
    private List<String> gitCommitHashes;
    /** 이 SR(반영 이력)에 장애가 등록되어 있는지 여부 */
    private Boolean incidentRegistered;
    /** 이 SR 에 사이드이펙트 검토 결과가 있는지 여부 */
    private Boolean hasSideEffectReport;
    private LocalDateTime createdAt;
}
