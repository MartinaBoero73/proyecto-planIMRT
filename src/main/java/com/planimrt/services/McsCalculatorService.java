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

        log.debug("Total MU del plan: {}", totalPlanMU);

        if (totalPlanMU == 0.0) {
            log.warn("Total MU del plan es 0, no se puede calcular MCS");
            return 0.0;
        }

        double planMCS = 0.0;

        for (BeamDTO beam : dicomFileDTO.getBeams()) {
            double beamMCS = calculateBeamMcs(beam);
            double beamMU = beam.getBeamMU() != null ? beam.getBeamMU() : 0.0;

            if (totalPlanMU > 0) {
                double weightedMCS = beamMCS * (beamMU / totalPlanMU);
                planMCS += weightedMCS;
                log.debug("Beam {}: MCS={}, MU={}, Weighted MCS={}",
                        beam.getBeamName(), beamMCS, beamMU, weightedMCS);
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
        if (segments == null || segments.isEmpty()) {
            log.warn("Beam {} no tiene segmentos", beam.getBeamName());
            return 0.0;
        }

        double totalMUBeam = beam.getBeamMU() != null ? beam.getBeamMU() : 0.0;
        if (totalMUBeam == 0.0) {
            log.warn("Beam {} tiene 0 MU", beam.getBeamName());
            return 0.0;
        }

        double beamMCS = 0.0;
        int validSegments = 0;

        for (int i = 0; i < segments.size(); i++) {
            SegmentDTO currentSeg = segments.get(i);
            double currentMuWeight = currentSeg.getMuWeight() != null ? currentSeg.getMuWeight() : 0.0;

            // Calcular MU incremental (diferencia entre control points consecutivos)
            double incrementalMU;
            if (i == 0) {
                incrementalMU = currentMuWeight; // Primer segmento
            } else {
                SegmentDTO previousSeg = segments.get(i - 1);
                double previousMuWeight = previousSeg.getMuWeight() != null ? previousSeg.getMuWeight() : 0.0;
                incrementalMU = currentMuWeight - previousMuWeight;
            }

            // Solo procesar si hay MU incremental positiv
            if (incrementalMU > 0) {
                double lsv = calculateLSV(currentSeg, i > 0 ? segments.get(i - 1) : null);
                double aav = calculateAAV(currentSeg);

                //MCS para este segmento ponderado por su MU incremental
                double segmentContribution = (aav * lsv) * (incrementalMU / totalMUBeam);
                beamMCS += segmentContribution;
                validSegments++;

                log.trace("Segment {}/{}: AAV={}, LSV={}, IncrMU={}, Contribution={}",
                        i, segments.size(), aav, lsv, incrementalMU, segmentContribution);
            }
        }

        log.debug("Beam {} → MCS = {} (de {} segmentos válidos)",
                beam.getBeamName(), beamMCS, validSegments);
        return beamMCS;
    }

    /**
     * Calcula la Leaf Sequence Variability (LSV) entre segmentos conecutivos.
     * Cuanto más similar la forma del campo, mayor LSV (0–1).
     */
    private double calculateLSV(SegmentDTO current, SegmentDTO previous) {
        if (previous == null) {
            return 1.0; // Primer segmento
        }

        List<Double> curr = current.getLeafJawPositions();
        List<Double> prev = previous.getLeafJawPositions();

        if (curr == null || prev == null || curr.isEmpty() || prev.isEmpty()) {
            log.trace("LSV: posiciones vacías o nulas, retornando 1.0");
            return 1.0;
        }

        if (curr.size() != prev.size()) {
            log.warn("LSV: tamaños diferentes ({} vs {}), retornando 1.0", curr.size(), prev.size());
            return 1.0;
        }

        // Encontrar la máxima diferencia entre posiciones
        double maxDiff = 0.0;
        for (int i = 0; i < curr.size(); i++) {
            double diff = Math.abs(curr.get(i) - prev.get(i));
            maxDiff = Math.max(maxDiff, diff);
        }

        // Si no hay diferencia, campos idénticos → LSV = 1
        if (maxDiff == 0) {
            return 1.0;
        }

        // Calcular LSV según McNiven: promedio de similitud normalizada
        double sum = 0.0;
        for (int i = 0; i < curr.size(); i++) {
            double diff = Math.abs(curr.get(i) - prev.get(i));
            sum += (maxDiff - diff) / maxDiff;
        }

        double lsv = sum / curr.size();
        log.trace("LSV calculado: {} (maxDiff={})", lsv, maxDiff);
        return lsv;
    }

    /**
     * Calcula la Aperture Area Variability (AAV).
     * Es la proporción del área abierta del campo respecto al área máxima posible.
     */
    private double calculateAAV(SegmentDTO seg) {
        List<Double> positions = seg.getLeafJawPositions();
        if (positions == null || positions.isEmpty()) {
            log.trace("AAV: sin posiciones, retornando 1.0");
            return 1.0;
        }

        // En DICOM RTPlan, las posiciones vienen como [L1, L2, ..., Ln, R1, R2, ..., Rn]
        // Donde L = lado izquierdo (negativo), R = lado derecho (positivo)
        int totalLeaves = positions.size();

        // Si el número es impar, hay un problema en los datos
        if (totalLeaves % 2 != 0) {
            log.warn("AAV: número impar de posiciones ({}), usando todas como están", totalLeaves);
            // Intentar calcular igualmente
        }

        int n = totalLeaves / 2; // Número de pares de hojas
        double sumArea = 0.0;
        double maxPossibleArea = 0.0;

        for (int i = 0; i < n; i++) {
            double left = positions.get(i);        // Posición izquierda (negativa)
            double right = positions.get(i + n);   // Posición derecha (positiva)

            // Apertura = distancia entre left y right
            double opening = Math.abs(right - left);
            sumArea += opening;

            // Asume que la apertura máxima es 400mm (20cm a cada lado)
            maxPossibleArea += 400.0;
        }

        if (maxPossibleArea == 0) {
            log.trace("AAV: área máxima es 0, retornando 1.0");
            return 1.0;
        }

        double aav = sumArea / maxPossibleArea;

        // Limitar entre 0 y 1
        aav = Math.min(1.0, Math.max(0.0, aav));

        log.trace("AAV calculado: {} (área={}, max={})", aav, sumArea, maxPossibleArea);
        return aav;
    }
}