package com.planimrt.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "history", indexes = {
        @Index(name = "idx_history_user", columnList = "user_id"),
        @Index(name = "idx_history_timestamp", columnList = "timestamp"),
        @Index(name = "idx_history_operation", columnList = "operation_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class History {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 50)
    private OperationType operationType;

    @Column(length = 100)
    private String entityType; // Patient, DicomFile, McsResult, etc.

    private Long entityId;

    @Column(columnDefinition = "TEXT")
    private String operationDetails;

    @Column(columnDefinition = "TEXT")
    private String changes;
}