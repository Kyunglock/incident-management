package com.incident.management.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "document")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String type;

    /** 참조 대상 종류 (RELEASE_PLAN | RELEASE_HISTORY | INCIDENT_ANALYSIS 등). refId 의 의미를 구분. */
    @Column(length = 30)
    private String refType;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 500)
    private String filePath;

    /** LLM 분석 원문(JSON 등). 화면에서 다시 보기 위해 보관. */
    @Column(columnDefinition = "TEXT")
    private String content;

    private Long refId;
}
