package com.incident.management.dto.response;

import com.incident.management.entity.ReleaseHistory;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReleaseHistoryResponse {
    private Long id;
    private Long releasePlanId;
    private LocalDateTime deployedAt;
    private ReleaseHistory.Status status;
    private String memo;
    private LocalDateTime createdAt;
}
