package com.planimrt.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "dicom_beams")
public class Beam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String beamName;

    private Double beamMU;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dicom_file_id")
    private DicomFile dicomFile;

    @OneToMany(mappedBy = "beam", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<Segment> segments = new ArrayList<>();
}
