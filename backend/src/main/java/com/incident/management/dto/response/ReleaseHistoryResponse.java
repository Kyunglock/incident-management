package com.incident.management.dto.response;

import com.incident.management.entity.CommitRef;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReleaseHistoryResponse {
    private Long id;
    private Long releasePlanId;
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
    private List<CommitRef> commits;
    private LocalDateTime createdAt;
}
