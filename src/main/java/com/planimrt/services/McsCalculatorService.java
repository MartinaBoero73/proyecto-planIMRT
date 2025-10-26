package com.planimrt.services;

import com.planimrt.DTOs.DicomFileDTO;
import org.springframework.stereotype.Service;

import com.planimrt.DTOs.BeamDTO;
import com.planimrt.DTOs.SegmentDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Service
@Slf4j
public class McsCalculatorService {

    /**
     * Calcula el Modulation Complexity Score (MCS) de un plan RT, basado en McNiven 2010.
     */
    public double calculateMcs(DicomFileDTO dicomFileDTO) {
        if (dicomFileDTO.getBeams() == null || dicomFileDTO.getBeams().isEmpty()) {
            log.warn("El archivo DICOM no contiene haces.");
            return 0.0;
        }

        double totalPlanMU = dicomFileDTO.getBeams().stream()
                .mapToDouble(b -> b.getBeamMU() != null ? b.getBeamMU() : 0.0)
                .sum();

        double planMCS = 0.0;

        for (BeamDTO beam : dicomFileDTO.getBeams()) {
            double beamMCS = calculateBeamMcs(beam);
            double beamMU = beam.getBeamMU() != null ? beam.getBeamMU() : 0.0;

            if (totalPlanMU > 0) {
                planMCS += beamMCS * (beamMU / totalPlanMU);
            }
        }

        log.info("MCS calculado para plan {}: {}", dicomFileDTO.getFileName(), planMCS);
        return planMCS;
    }

    /**
     * Calcula el MCS de un haz individual.
     */
    private double calculateBeamMcs(BeamDTO beam) {
        List<SegmentDTO> segments = beam.getSegments();
        if (segments == null || segments.isEmpty()) return 0.0;

        double totalMUBeam = beam.getBeamMU() != null ? beam.getBeamMU() : 0.0;
        if (totalMUBeam == 0.0) totalMUBeam = 1.0; // evitar división por cero

        double beamMCS = 0.0;

        for (int i = 0; i < segments.size(); i++) {
            SegmentDTO seg = segments.get(i);
            double muWeight = seg.getMuWeight() != null ? seg.getMuWeight() : 0.0;

            double lsv = calculateLSV(seg, i > 0 ? segments.get(i - 1) : null);
            double aav = calculateAAV(seg);

            beamMCS += (aav * lsv) * (muWeight / totalMUBeam);
        }

        log.debug("Beam {} → MCS = {}", beam.getBeamName(), beamMCS);
        return beamMCS;
    }

    /**
     * Calcula la Leaf Sequence Variability (LSV) entre segmentos conecutivos.
     * Cuanto más similar la forma del campo, mayor LSV (0–1).
     */
    private double calculateLSV(SegmentDTO current, SegmentDTO previous) {
        if (previous == null || previous.getLeafJawPositions() == null || current.getLeafJawPositions() == null)
            return 1.0;

        List<Double> curr = current.getLeafJawPositions();
        List<Double> prev = previous.getLeafJawPositions();

        if (curr.size() != prev.size()) return 1.0;

        double maxDiff = 0.0;
        for (int i = 0; i < curr.size(); i++) {
            maxDiff = Math.max(maxDiff, Math.abs(curr.get(i) - prev.get(i)));
        }

        if (maxDiff == 0) return 1.0;

        double sum = 0.0;
        for (int i = 0; i < curr.size(); i++) {
            double diff = Math.abs(curr.get(i) - prev.get(i));
            sum += (maxDiff - diff) / maxDiff;
        }

        return sum / curr.size();
    }

    /**
     * Calcula la Aperture Area Variability (AAV).
     * Es la proporción del área abierta del campo respecto al área máxima.
     */
    private double calculateAAV(SegmentDTO seg) {
        List<Double> positions = seg.getLeafJawPositions();
        if (positions == null || positions.isEmpty()) return 1.0;

        // En DICOM RTPlan, las posiciones vienen como [L1, L2, ., R1, R2, ...]
        int n = positions.size() / 2;
        double sumArea = 0.0;

        for (int i = 0; i < n; i++) {
            double left = Math.abs(positions.get(i)); // lado izquierdo
            double right = Math.abs(positions.get(i + n)); // lado derecho
            double opening = Math.abs(right - left);
            sumArea += opening;
        }

        double maxArea = n * 10.0; // asume apertura máxima de 10 mm por hoja
        double aav = sumArea / maxArea;
        return Math.min(1.0, Math.max(0.0, aav)); // limitar 0–1
    }
}

