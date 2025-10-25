package com.planimrt.model;

import lombok.Builder;
import lombok.Data;

import java.awt.image.BufferedImage;
import java.util.List;

@Data
@Builder
public class ProcessingResult {
    private DicomFile dicomFile;
    private Double mcsIndex;
    private BufferedImage collimatorPlot;
    private ProcessingStatus status;
    private List<String> errors;
}
