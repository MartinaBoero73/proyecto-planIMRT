package com.planimrt.services;

import com.planimrt.DTOs.DicomFileDTO;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomInputStream;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Service
@Slf4j
public class DicomReaderService {
    /**
     * Lee un archivo DICOM y devuelve un DTO (sin guardar en BD)
     */
    public DicomFileDTO readDicomFileAsDTO(String dicomPath) throws IOException {
        File file = new File(dicomPath);
        return readDicomFileAsDTO(file);
    }

    /**
     * Lee un archivo DICOM desde un File y devuelve un DTO
     */
    public DicomFileDTO readDicomFileAsDTO(File file) throws IOException {
        if (!file.exists()) {
            throw new IllegalArgumentException("El archivo no existe: " + file.getAbsolutePath());
        }

        log.debug("Leyendo DICOM como DTO desde archivo: {}", file.getAbsolutePath());

        String fileName = file.getName();
        String patientId = null;
        java.util.Map<String, String> metadata = new java.util.HashMap<>();

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

        } catch (Exception e) {
            throw new IOException("Error al leer el archivo DICOM: " + e.getMessage(), e);
        }

        return new DicomFileDTO(patientId, fileName, metadata, null);
    }

    /**
     * Extrae todos los metadatos a un Map (para DTos)
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


    /**
     * Obtiene el nombre de un tag DICOM usando el diccionario de DCM4CHE
     */
    private String getTagName(int tag) {
        try {
            // Usar ElementDictionary de DCM4CHE para obtener el nombre del tag
            return org.dcm4che3.data.ElementDictionary.keywordOf(tag, null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtiene un valor String de un atributo DICOM de forma segura
     */
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