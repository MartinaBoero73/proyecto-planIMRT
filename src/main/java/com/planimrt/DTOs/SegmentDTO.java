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
public class SegmentDTO {
    private Double muWeight;
    private List<Double> leafJawPositions;
}
