package com.planimrt.services;

import com.planimrt.DTOs.DicomFileDTO;
import com.planimrt.model.DicomFile;
import com.planimrt.model.ProcessingResult;
import com.planimrt.model.ProcessingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessingOrchestrator {

    private final DicomFileService dicomFileService;
    private final DicomReaderService dicomReaderService;
    private final McsCalculatorService mcsCalculator;
    private final CollimatorPlotterService plotter;

    /**
     * Procesa un plan DICOM desde una ruta de archivo
     */
    public ProcessingResult processPlan(String dicomPath, Long responsibleUserId) {
        log.info("Iniciando procesamiento del archivo: {}", dicomPath);

        try {
            // 0. Valida que el archivo existe
            validateFilePath(dicomPath);

            // CAMBIO: Leer DICOM completo con beams ANTES de guardarlo
            DicomFileDTO dicomFileDTO = dicomReaderService.readDicomFileAsDTO(dicomPath);
            log.info("DICOM leído: PatientID={}, Beams={}",
                    dicomFileDTO.getPatientId(),
                    dicomFileDTO.getBeams() != null ? dicomFileDTO.getBeams().size() : 0);

            // Guardar en BD (solo metadatos básicos)
            DicomFile dicomFile = dicomFileService.processFromPath(dicomPath, responsibleUserId);
            log.info("Archivo DICOM procesado y guardado. ID: {}, PatientID: {}, Archivo: {}",
                    dicomFile.getId(),
                    dicomFile.getPatient().getPatientId(),
                    dicomFile.getFileName());

            // 2. Obtiene metadatos como Map
            Map<String, String> metadata = dicomFileService.getMetadataAsMap(dicomFile);

            // 3. Detecta inconsistencias en los metadatos
            List<String> errors = detectInconsistencies(dicomFile, metadata);

            if (!errors.isEmpty()) {
                log.warn("Se detectaron {} inconsistencias en el archivo", errors.size());
                errors.forEach(error -> log.warn("  - {}", error));
            }

            // CAMBIO: Usar el DTO con beams (no el de BD)
            double mcs = 0.0;
            try {
                mcs = mcsCalculator.calculateMcs(dicomFileDTO);
                log.info("MCS calculado: {}", mcs);
            } catch (Exception e) {
                log.error("Error calculando MCS", e);
                errors.add("No se pudo calcular MCS: " + e.getMessage());
            }

            // 5. Generar plot del colimador
            BufferedImage plot = null;
            try {
                List<List<Double>> collimatorData = extractCollimatorData(metadata);
                List<Double> positions = extractPositions(metadata);

                if (!collimatorData.isEmpty() && !positions.isEmpty()) {
                    plot = plotter.generatePlot(collimatorData, positions);
                    log.info("Plot del colimador generado exitosamente");
                } else {
                    log.warn("No se encontraron datos suficientes para generar el plot");
                }
            } catch (Exception e) {
                log.error("Error generando plot del colimador", e);
                errors.add("No se pudo generar el plot del colimador: " + e.getMessage());
            }

            // 6. Determina el estado del procesamiento
            ProcessingStatus status = determineStatus(errors, mcs, plot);
            log.info("Procesamiento completado con estado: {}", status);

            return ProcessingResult.builder()
                    .dicomFile(dicomFile)
                    .mcsIndex(mcs)
                    .collimatorPlot(plot)
                    .status(status)
                    .errors(errors)
                    .beams(dicomFileDTO.getBeams())
                    .build();

        } catch (Exception e) {
            log.error("Error crítico procesando plan DICOM: {}", dicomPath, e);
            return ProcessingResult.builder()
                    .status(ProcessingStatus.FAILED)
                    .errors(List.of("Error crítico: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Valida que el archivo existe y es accesible
     */
    private void validateFilePath(String dicomPath) throws IllegalArgumentException {
        if (dicomPath == null || dicomPath.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta del archivo no puede estar vacía");
        }

        Path path = Paths.get(dicomPath);

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("El archivo no existe: " + dicomPath);
        }

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("La ruta no corresponde a un archivo regular: " + dicomPath);
        }

        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("El archivo no tiene permisos de lectura: " + dicomPath);
        }

        String fileName = path.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".dcm")) {
            log.warn("El archivo no tiene extensión .dcm: {}", fileName);
        }
    }

    /**
     * Detecta inconsistencias en los metadatos DICOM
     */
    private List<String> detectInconsistencies(DicomFile dicomFile, Map<String, String> metadata) {
        List<String> errors = new ArrayList<>();

        if (dicomFile.getPatient() == null) {
            errors.add("Paciente no asignado al archivo DICOM");
            return errors;
        }

        if (dicomFile.getPatient().getPatientId() == null || dicomFile.getPatient().getPatientId().isEmpty()) {
            errors.add("Patient ID no encontrado");
        }

        if (metadata == null || metadata.isEmpty()) {
            errors.add("No se encontraron metadatos DICOM");
            return errors;
        }

        String modality = dicomFile.getModality();
        if (modality == null || modality.isEmpty()) {
            errors.add("Modalidad DICOM no encontrada");
        } else if (!modality.equals("RTPLAN") && !modality.equals("CT") && !modality.equals("MR") && !modality.equals("RTSTRUCT")) {
            errors.add("Modalidad no soportada para planificación: " + modality);
        }

        if (dicomFile.getStudyInstanceUID() == null || dicomFile.getStudyInstanceUID().isEmpty()) {
            errors.add("Study Instance UID no encontrado");
        }

        if (dicomFile.getSeriesInstanceUID() == null || dicomFile.getSeriesInstanceUID().isEmpty()) {
            errors.add("Series Instance UID no encontrado");
        }

        if (dicomFile.getSopInstanceUID() == null || dicomFile.getSopInstanceUID().isEmpty()) {
            errors.add("SOP Instance UID no encontrado");
        }

        if (dicomFile.getPatient().getName() == null || dicomFile.getPatient().getName().isEmpty()) {
            errors.add("Nombre del paciente no encontrado");
        }
        if ("RTPLAN".equals(modality)) {
            validateRTPlanSpecificFields(metadata, errors);
        }

        return errors;
    }

    /**
     * Validaciones específicas para archivos RTPLAN
     */
    private void validateRTPlanSpecificFields(Map<String, String> metadata, List<String> errors) {
        String beamSequence = metadata.get("BeamSequence");
        if (beamSequence == null || !beamSequence.equals("[Sequence]")) {
            errors.add("Beam Sequence no encontrada (requerida para RTPLAN)");
        }

        String fractionGroupSequence = metadata.get("FractionGroupSequence");
        if (fractionGroupSequence == null || !fractionGroupSequence.equals("[Sequence]")) {
            errors.add("Fraction Group Sequence no encontrada (requerida para RTPLAN)");
        }
    }

    /**
     * Extrae datos del colimador desde los metadatos DICOM
     */
    private List<List<Double>> extractCollimatorData(Map<String, String> metadata) {
        List<List<Double>> data = new ArrayList<>();

        // TODO: Implementar extracción real de datos del colimador desde DICOM

        String beamSequence = metadata.get("BeamSequence");
        if (beamSequence != null && beamSequence.equals("[Sequence]")) {
            log.debug("BeamSequence detectada, usando datos de ejemplo para el plot");
            data.add(List.of(1.0, 2.0, 3.0, 4.0));
            data.add(List.of(1.5, 2.5, 3.5, 4.5));
        }

        return data;
    }

    /**
     * Extrae posiciones desde los metadatos DICOM
     */
    private List<Double> extractPositions(Map<String, String> metadata) {
        List<Double> positions = new ArrayList<>();

        // TODO: Implementar extracción real de posiciones desde DICOM

        String controlPointSequence = metadata.get("ControlPointSequence");
        if (controlPointSequence != null && controlPointSequence.equals("[Sequence]")) {
            // Datos de ejemplo
            log.debug("ControlPointSequence detectada, usando posiciones de ejemplo");
            positions.add(0.0);
            positions.add(45.0);
            positions.add(90.0);
            positions.add(135.0);
        }

        return positions;
    }

    /**
     * Determina el estado final del procesamiento
     */
    private ProcessingStatus determineStatus(List<String> errors, double mcs, BufferedImage plot) {
        if (errors.isEmpty()) {
            return ProcessingStatus.SUCCESS;
        }

        // Verificar si son errores críticos
        boolean hasCriticalErrors = errors.stream()
                .anyMatch(error ->
                        error.contains("Patient ID no encontrado") ||
                                error.contains("Paciente no asignado") ||
                                error.contains("No se encontraron metadatos") ||
                                error.contains("Error crítico")
                );

        if (hasCriticalErrors) {
            return ProcessingStatus.FAILED;
        }

        if (mcs > 0 || plot != null) {
            return ProcessingStatus.PARTIAL;
        }

        return ProcessingStatus.FAILED;
    }
}