package com.incident.management.dto.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 반영 이력 생성 요청. SR 번호 1건과 1:1 매핑된다.
 * 서비스/작업내용/요청자 등 상세 정보는 SR 번호로 레드마인 연동해 채운다.
 */
@Data
public class CreateReleaseHistoryRequest {

    private String srNumber;
    private List<CommitDto> commits = new ArrayList<>();

    @Data
    public static class CommitDto {
        private String hash;
        private String author;
        private String date;
        private String message;
    }
}
