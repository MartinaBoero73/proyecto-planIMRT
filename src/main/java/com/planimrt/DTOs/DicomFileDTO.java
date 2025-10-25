package com.planimrt.DTOs;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DicomFileDTO {
    private String patientId;
    private String fileName;
    private Map<String, String> metadata;
    private List<BeamDTO> beams;
}
