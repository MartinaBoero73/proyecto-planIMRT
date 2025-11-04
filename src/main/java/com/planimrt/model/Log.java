package com.planimrt.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "logs", indexes = {
        @Index(name = "idx_log_user", columnList = "user_id"),
        @Index(name = "idx_log_timestamp", columnList = "timestamp"),
        @Index(name = "idx_log_event_type", columnList = "event_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Log {

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
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 20)
    private String severity; // INFO, WARNING, ERROR
}