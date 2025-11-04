package com.planimrt.model;

import com.planimrt.DTOs.BeamDTO;
import com.planimrt.DTOs.DicomFileDTO;
import com.planimrt.DTOs.SegmentDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Entity
@Table(name = "dicom_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DicomFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 500)
    private String filePath;

    @Column(length = 100)
    private String studyInstanceUID;

    @Column(length = 100)
    private String seriesInstanceUID;

    @Column(length = 100)
    private String sopInstanceUID;

    @Column(length = 20)
    private String modality;

    @Column(length = 500)
    private String studyDescription;

    @Column(length = 500)
    private String seriesDescription;

    private LocalDate studyDate;

    @Column(length = 20)
    private String studyTime;

    @Column(length = 200)
    private String manufacturer;

    @Column(length = 200)
    private String institutionName;

    private Integer rows;

    private Integer columns;

    @Column(length = 100)
    private String transferSyntaxUID;

    @Column(columnDefinition = "TEXT")
    private String allMetadata; // JSON serializado de todos los metadatos

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @OneToOne(mappedBy = "dicomFile", cascade = CascadeType.ALL)
    private McsResult mcsResult;

    @OneToMany(mappedBy = "dicomFile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<Beam> beams = new ArrayList<>();

    public void addMetadata(String key, String valueStr) {
    }

    public DicomFileDTO toDTO() {

        List<BeamDTO> beamDTOs = beams.stream().map(beam -> {
            List<SegmentDTO> segmentDTOs = beam.getSegments().stream()
                    .map(seg -> SegmentDTO.builder()
                            .muWeight(seg.getMuWeight())
                            .leafJawPositions(seg.getLeafJawPositions())
                            .build())
                    .collect(Collectors.toList());

            return BeamDTO.builder()
                    .beamName(beam.getBeamName())
                    .beamMU(beam.getBeamMU())
                    .segments(segmentDTOs)
                    .build();
        }).collect(Collectors.toList());

        return DicomFileDTO.builder()
                .patientId(patient != null ? patient.getPatientId() : "UNKNOWN")
                .fileName(fileName)
                .beams(beamDTOs)
                .build();
    }
}