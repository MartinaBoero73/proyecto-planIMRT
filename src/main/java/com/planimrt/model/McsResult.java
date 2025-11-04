package com.planimrt.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mcs_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class McsResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dicom_file_id", nullable = false, unique = true)
    private DicomFile dicomFile;

    @Column(nullable = false)
    private Double mcsValue;

    @Column(columnDefinition = "TEXT")
    private String calculationDetails; // JSON con detalles del cálculo

    @Column(columnDefinition = "TEXT")
    private String collimatorPlotPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProcessingStatus status;

    @Column(columnDefinition = "TEXT")
    private String errors;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime calculatedAt;
}