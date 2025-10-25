package com.planimrt.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "dicom_segments")
public class Segment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double muWeight;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "segment_positions", joinColumns = @JoinColumn(name = "segment_id"))
    @Column(name = "position_value")
    private List<Double> leafJawPositions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beam_id")
    private Beam beam;
}
