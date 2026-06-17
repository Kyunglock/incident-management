package com.incident.management.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReleasePlanResponse {
    private Long id;
    private String title;
    private String summary;
    private String docPath;
    private String llmOutput;
    private LocalDateTime createdAt;
}
