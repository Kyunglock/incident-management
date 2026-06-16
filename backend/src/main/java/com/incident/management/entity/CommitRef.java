package com.incident.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/** 반영 이력에 매핑된 git 커밋 정보 (@ElementCollection 으로 저장) */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommitRef {

    @Column(name = "commit_hash", length = 40)
    private String hash;

    @Column(name = "commit_author")
    private String author;

    @Column(name = "commit_date")
    private String date;

    @Column(name = "commit_message", columnDefinition = "TEXT")
    private String message;
}
