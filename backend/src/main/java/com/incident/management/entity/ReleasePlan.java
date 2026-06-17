package com.incident.management.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "release_plan")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleasePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String title;

    /** 시트 내 SR 작업내용을 LLM으로 한 줄 요약한 값 (목록 표시용). */
    @Column(length = 500)
    private String summary;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 500)
    private String excelPath;

    @Column(length = 500)
    private String docPath;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String rawInput;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String llmOutput;
}
