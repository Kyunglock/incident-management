package com.incident.management.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

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
    private Boolean frontendChanged;
    private Boolean backendChanged;
    private String note;
    private Boolean finalConfirmed;
    private String gitSystem;
    private String gitCommitHash;
    private String gitCommitMessage;
    /** 이 SR(반영 이력)에 장애가 등록되어 있는지 여부 */
    private Boolean incidentRegistered;
    private LocalDateTime createdAt;
}
