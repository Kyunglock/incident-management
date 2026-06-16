package com.incident.management.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @JoinColumn(name = "release_plan_id", nullable = false)
    private ReleasePlan releasePlan;

    private LocalDateTime deployedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(columnDefinition = "TEXT")
    private String memo;

    /** 매핑된 SR 번호 목록 (자유 입력 문자열) */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "release_history_sr", joinColumns = @JoinColumn(name = "release_history_id"))
    @Column(name = "sr_number")
    @Builder.Default
    private List<String> srNumbers = new ArrayList<>();

    /** 매핑된 git 커밋 목록 */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "release_history_commit", joinColumns = @JoinColumn(name = "release_history_id"))
    @Builder.Default
    private List<CommitRef> commits = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum Status {
        PENDING, DEPLOYED, ROLLED_BACK
    }
}
