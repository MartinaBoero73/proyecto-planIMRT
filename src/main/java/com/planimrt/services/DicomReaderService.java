package com.planimrt.services;

import com.planimrt.DTOs.BeamDTO;
import com.planimrt.DTOs.DicomFileDTO;
import com.planimrt.DTOs.SegmentDTO;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.*;
import org.dcm4che3.io.DicomInputStream;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DicomReaderService {

    public DicomFileDTO readDicomFileAsDTO(String dicomPath) throws IOException {
        File file = new File(dicomPath);
        return readDicomFileAsDTO(file);
    }

    public DicomFileDTO readDicomFileAsDTO(File file) throws IOException {
        if (!file.exists()) {
            throw new IllegalArgumentException("El archivo no existe: " + file.getAbsolutePath());
        }

        log.debug("Leyendo DICOM como DTO desde archivo: {}", file.getAbsolutePath());

        String fileName = file.getName();
        String patientId = null;
        Map<String, String> metadata = new HashMap<>();
        List<BeamDTO> beamsData = new ArrayList<>();

        try (DicomInputStream dis = new DicomInputStream(new FileInputStream(file))) {
            Attributes attributes = dis.readDataset();

            // Extraer Patient ID
            patientId = getString(attributes, Tag.PatientID);

            // Extraer Transfer Syntax
            String transferSyntax = dis.getTransferSyntax();
            if (transferSyntax != null && !transferSyntax.isEmpty()) {
                metadata.put("TransferSyntaxUID", transferSyntax);
            }

            // Extraser todos los metadatos
            extractAllMetadataToMap(attributes, metadata);

            // Extraer datos de los beams (si es un RT Plan)
            extractBeamsData(attributes, beamsData);

        } catch (Exception e) {
            throw new IOException("Error al leer el archivo DICOM: " + e.getMessage(), e);
        }

        log.info("DicomFileDTO creado: PatientID={}, Beams={}", patientId, beamsData.size());
        return new DicomFileDTO(patientId, fileName, metadata, beamsData);
    }

    /**
     * Extrae datos de los beams de un RT Plan como BeamDTO
     */
    private void extractBeamsData(Attributes attributes, List<BeamDTO> beamsData) {
        log.debug("=== INICIANDO EXTRACCIÓN DE BEAMS ===");

        // Primero, extraer los MU reales desde FractionGroupSequence -> ReferencedBeamSequence
        Map<Integer, Double> beamNumberToMU = extractRealBeamMU(attributes);

        // Probar múltiples tags posibles para beams
        Sequence beamSeq = attributes.getSequence(Tag.BeamSequence);

        if (beamSeq == null) {
            log.warn("Tag.BeamSequence (300A,00B0) no encontrado, intentando con FractionGroupSequence...");

            // Intentar con FractionGroupSequence -> ReferencedBeamSequence
            Sequence fractionGroupSeq = attributes.getSequence(Tag.FractionGroupSequence);
            if (fractionGroupSeq != null && !fractionGroupSeq.isEmpty()) {
                log.debug("FractionGroupSequence encontrada con {} items", fractionGroupSeq.size());
                Attributes firstFraction = fractionGroupSeq.get(0);
                beamSeq = firstFraction.getSequence(Tag.ReferencedBeamSequence);

                if (beamSeq != null) {
                    log.warn("Encontrados beams en ReferencedBeamSequence, pero esto solo tiene referencias. Buscando en IonBeamSequence...");
                }
            }

            // Intentar con IonBeamSequence (para protones/iones)
            beamSeq = attributes.getSequence(Tag.IonBeamSequence);
            if (beamSeq != null) {
                log.info("Encontrada IonBeamSequence (300A,03A2) - RT Ion Plan detectado");
            }
        } else {
            log.debug("BeamSequence (300A,00B0) encontrada con {} beams", beamSeq.size());
        }

        if (beamSeq == null || beamSeq.isEmpty()) {
            log.error("No se encontró ninguna secuencia de beams en el archivo DICOM.");
            log.error("Tags intentados: BeamSequence (300A,00B0), IonBeamSequence (300A,03A2), ReferencedBeamSequence");
            return;
        }

        log.info("Procesando {} beams...", beamSeq.size());

        for (int i = 0; i < beamSeq.size(); i++) {
            Attributes beamItem = beamSeq.get(i);
            log.debug("--- Procesando Beam #{} ---", i + 1);

            BeamDTO beamDTO = new BeamDTO();

            // Extraer BeamNumber para hacer match con los MU reales
            Integer beamNumber = beamItem.getInt(Tag.BeamNumber, -1);
            log.debug("BeamNumber: {}", beamNumber);

            // Extraer datos básicos del beam con logs detallados
            String beamName = beamItem.getString(Tag.BeamName, null);
            if (beamName == null) {
                beamName = "Beam_" + (i + 1);
                log.warn("BeamName no encontrado, usando nombre por defecto: {}", beamName);
            } else {
                log.debug("BeamName: {}", beamName);
            }
            beamDTO.setBeamName(beamName);

            // Obtener el MU real desde el mapa extraído de ReferencedBeamSequence
            Double beamMU = beamNumberToMU.getOrDefault(beamNumber, 0.0);

            //Si no se encontró en ReferencedBeamSequence, intentar obtenerlo del beam directamente
            if (beamMU == 0.0) {
                if (beamItem.contains(Tag.BeamMeterset)) {
                    beamMU = beamItem.getDouble(Tag.BeamMeterset, 0.0);
                    log.debug("BeamMeterset desde BeamSequence (300A,0086): {}", beamMU);
                } else if (beamItem.contains(Tag.FinalCumulativeMetersetWeight)) {
                    beamMU = beamItem.getDouble(Tag.FinalCumulativeMetersetWeight, 0.0);
                    log.debug("FinalCumulativeMetersetWeight (300A,010E): {}", beamMU);
                } else {
                    log.warn("No se encontró MU para beam {}", beamName);
                }
            } else {
                log.debug("BeamMeterset desde ReferencedBeamSequence: {} MU", beamMU);
            }
            beamDTO.setBeamMU(beamMU);

            // Extraer Control Points como Segmentos
            List<SegmentDTO> segments = new ArrayList<>();
            Sequence cps = beamItem.getSequence(Tag.ControlPointSequence);

            if (cps == null) {
                log.warn("ControlPointSequence no encontrada para beam: {}", beamName);
                // Intentar con IonControlPointSequence para planes de iones
                cps = beamItem.getSequence(Tag.IonControlPointSequence);
                if (cps != null) {
                    log.info("IonControlPointSequence encontrada con {} control points", cps.size());
                }
            } else {
                log.debug("ControlPointSequence encontrada con {} control points", cps.size());
            }

            if (cps != null) {
                for (int j = 0; j < cps.size(); j++) {
                    Attributes cp = cps.get(j);
                    log.debug("  Control Point #{}: Index={}", j, cp.getInt(Tag.ControlPointIndex, -1));

                    SegmentDTO segmentDTO = new SegmentDTO();

                    // MU Weight (CumulativeMetersetWeight)
                    Double muWeight = cp.getDouble(Tag.CumulativeMetersetWeight, 0.0);
                    log.debug("    CumulativeMetersetWeight: {}", muWeight);
                    segmentDTO.setMuWeight(muWeight);

                    // Posiciones de colimador (Leaf/Jaw) - intentar múltiples tags
                    double[] leafJawPositions = cp.getDoubles(Tag.LeafJawPositions);

                    if (leafJawPositions == null || leafJawPositions.length == 0) {
                        // Intentar con BeamLimitingDevicePositionSequence
                        Sequence bldSeq = cp.getSequence(Tag.BeamLimitingDevicePositionSequence);
                        if (bldSeq != null && !bldSeq.isEmpty()) {
                            log.debug("    Usando BeamLimitingDevicePositionSequence");
                            List<Double> allPositions = new ArrayList<>();

                            for (Attributes bld : bldSeq) {
                                double[] positions = bld.getDoubles(Tag.LeafJawPositions);
                                if (positions != null && positions.length > 0) {
                                    for (double pos : positions) {
                                        allPositions.add(pos);
                                    }
                                }
                            }

                            if (!allPositions.isEmpty()) {
                                segmentDTO.setLeafJawPositions(allPositions);
                                log.debug("    LeafJawPositions desde BeamLimitingDevicePositionSequence: {} valores", allPositions.size());
                            } else {
                                segmentDTO.setLeafJawPositions(new ArrayList<>());
                                log.debug("    BeamLimitingDevicePositionSequence sin posiciones");
                            }
                        } else {
                            segmentDTO.setLeafJawPositions(new ArrayList<>());
                            log.debug("    LeafJawPositions no encontradas o vacías");
                        }
                    } else {
                        log.debug("    LeafJawPositions encontradas: {} valores", leafJawPositions.length);
                        // Convertir array primitivo a List<Double>
                        List<Double> positionsList = Arrays.stream(leafJawPositions)
                                .boxed()
                                .collect(Collectors.toList());
                        segmentDTO.setLeafJawPositions(positionsList);
                    }

                    segments.add(segmentDTO);
                }
            } else {
                log.error("No se encontraron Control Points para beam: {}", beamName);
            }

            beamDTO.setSegments(segments);
            beamsData.add(beamDTO);

            log.info("Beam '{}' procesado: {} MU, {} segmentos", beamDTO.getBeamName(), beamDTO.getBeamMU(), segments.size());
        }

        log.info("=== EXTRACCIÓN COMPLETADA: {} beams extraídos ===", beamsData.size());
    }

    /**
     * Extrae los valores reales de MU desde FractionGroupSequence -> ReferencedBeamSequence
     */
    private Map<Integer, Double> extractRealBeamMU(Attributes attributes) {
        Map<Integer, Double> beamNumberToMU = new HashMap<>();

        Sequence fractionGroupSeq = attributes.getSequence(Tag.FractionGroupSequence);
        if (fractionGroupSeq == null || fractionGroupSeq.isEmpty()) {
            log.warn("FractionGroupSequence no encontrada");
            return beamNumberToMU;
        }

        log.debug("FractionGroupSequence encontrada con {} fracciones", fractionGroupSeq.size());

        // Recorrer todas las farcciones (generalmente hay 1)
        for (Attributes fractionItem : fractionGroupSeq) {
            Sequence refBeamSeq = fractionItem.getSequence(Tag.ReferencedBeamSequence);

            if (refBeamSeq == null) {
                log.warn("ReferencedBeamSequence no encontrada en FractionGroup");
                continue;
            }

            log.debug("ReferencedBeamSequence encontrada con {} referencias", refBeamSeq.size());

            // Extraer BeamMeterset de cada beam referenciado
            for (Attributes refBeam : refBeamSeq) {
                Integer beamNumber = refBeam.getInt(Tag.ReferencedBeamNumber, -1);
                Double beamMeterset = refBeam.getDouble(Tag.BeamMeterset, 0.0);

                if (beamNumber != -1 && beamMeterset > 0) {
                    beamNumberToMU.put(beamNumber, beamMeterset);
                    log.debug("Beam #{}: {} MU (desde ReferencedBeamSequence)", beamNumber, beamMeterset);
                }
            }
        }

        log.info("Se extrajeron {} valores de MU desde ReferencedBeamSequence", beamNumberToMU.size());
        return beamNumberToMU;
    }

    /**
     * Extrae todos los metadatos a un Map (para DTOs)
     */
    private void extractAllMetadataToMap(Attributes attributes, Map<String, String> metadata) throws Exception {
        attributes.accept((attrs, tag, vr, value) -> {
            try {
                String tagName = getTagName(tag);
                String tagHex = String.format("%08X", tag);

                String valueStr;
                if (vr == VR.SQ) {
                    valueStr = "[Sequence]";
                } else {
                    valueStr = attrs.getString(tag, "");
                }

                if (valueStr != null && !valueStr.isEmpty()) {
                    String key = tagName != null && !tagName.isEmpty() ? tagName : "Tag_" + tagHex;
                    metadata.put(key, valueStr);
                }
            } catch (Exception e) {
                log.warn("Error procesando atributo {}: {}", String.format("%08X", tag), e.getMessage());
            }
            return true;
        }, true);
    }

    private String getTagName(int tag) {
        try {
            return ElementDictionary.keywordOf(tag, null);
        } catch (Exception e) {
            return null;
        }
    }

    private String getString(Attributes attrs, int tag) {
        try {
            String value = attrs.getString(tag, "");
            return value != null ? value.trim() : "";
        } catch (Exception e) {
            log.warn("Error obteniendo string del tag {}: {}", String.format("%08X", tag), e.getMessage());
            return "";
        }
    }
}