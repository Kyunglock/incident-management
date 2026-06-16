package com.incident.management.dto.response;

import com.incident.management.entity.CommitRef;
import com.incident.management.entity.ReleaseHistory;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReleaseHistoryResponse {
    private Long id;
    private Long releasePlanId;
    private LocalDateTime deployedAt;
    private ReleaseHistory.Status status;
    private String memo;
    private List<String> srNumbers;
    private List<CommitRef> commits;
    private LocalDateTime createdAt;
}
