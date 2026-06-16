package com.incident.management.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 반영 이력 생성 요청 (SR/커밋 매핑 포함) */
@Data
public class CreateReleaseHistoryRequest {

    private LocalDateTime deployedAt;
    private String memo;
    private List<String> srNumbers = new ArrayList<>();
    private List<CommitDto> commits = new ArrayList<>();

    @Data
    public static class CommitDto {
        private String hash;
        private String author;
        private String date;
        private String message;
    }
}
