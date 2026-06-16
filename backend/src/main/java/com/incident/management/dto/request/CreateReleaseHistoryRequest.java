package com.incident.management.dto.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 반영 이력 생성 요청. 엑셀 "시스템 반영 작업 요청" 한 행 = 1 SR 과 1:1 매핑된다. */
@Data
public class CreateReleaseHistoryRequest {

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
    private List<CommitDto> commits = new ArrayList<>();

    @Data
    public static class CommitDto {
        private String hash;
        private String author;
        private String date;
        private String message;
    }
}
