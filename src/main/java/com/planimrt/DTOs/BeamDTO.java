package com.planimrt.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BeamDTO {
    private String beamName;
    private Double beamMU;
    private List<SegmentDTO> segments;
}
