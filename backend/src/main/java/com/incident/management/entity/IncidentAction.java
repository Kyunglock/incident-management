package com.incident.management.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident_actions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(columnDefinition = "TEXT")
    private String actionDescription;

    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    private String performedBy;

    private LocalDateTime performedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum ActionType {
        ANALYSIS, FIX, DEPLOY, VERIFY
    }
}
