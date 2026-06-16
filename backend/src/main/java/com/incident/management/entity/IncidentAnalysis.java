package com.incident.management.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident_analysis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(columnDefinition = "TEXT")
    private String errorLogs;

    @Column(columnDefinition = "TEXT")
    private String cause;

    @Column(length = 500)
    private String docPath;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
