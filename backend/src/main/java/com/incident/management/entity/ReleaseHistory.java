package com.incident.management.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 반영 이력. 엑셀 "시스템 반영 작업 요청" 한 행(row) = 1 SR 과 1:1로 매핑된다.
 */
@Entity
@Table(name = "release_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_plan_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ReleasePlan releasePlan;

    /** SR 번호 (레드마인 이슈 번호). 이 번호로 레드마인 연동해 아래 정보를 채운다. */
    private String srNumber;

    /** 서비스 (레드마인 연동) */
    private String service;

    /** 작업내용 (레드마인 연동) */
    @Column(columnDefinition = "TEXT")
    private String workContent;

    /** 요청자 (레드마인 연동) */
    private String requester;

    /** 작업자 (레드마인 연동) */
    private String worker;

    /** TEST URL - 검수 */
    private String testUrlVerify;

    /** TEST URL - 운영 */
    private String testUrlProd;

    /** TEST 상세 */
    @Column(columnDefinition = "TEXT")
    private String testDetail;

    /** Frontend 작업 여부 */
    private Boolean frontendChanged;

    /** Backend 작업 여부 */
    private Boolean backendChanged;

    /** 비고 */
    private String note;

    /** 최종확인 */
    @Builder.Default
    private Boolean finalConfirmed = false;

    /** 연동된 git 시스템(저장소) 키. 미지정 시 기본 저장소 사용 */
    private String gitSystem;

    /** 연동된 git 커밋 해시 */
    private String gitCommitHash;

    /** 연동된 git 커밋 메시지 (표시용) */
    private String gitCommitMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
