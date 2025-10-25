package com.planimrt.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planimrt.model.DicomFile;
import com.planimrt.model.Patient;
import com.planimrt.DTOs.DicomFileDTO;
import com.planimrt.repository.DicomFileRepository;
import com.planimrt.repository.PatientRepository;
import com.planimrt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DicomFileService {

    private final DicomFileRepository dicomFileRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final DicomReaderService dicomReaderService;
    private final ObjectMapper objectMapper;

    private static final String UPLOAD_DIR = "uploads/dicom/";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Procesa un archivo DICOM desde una ruta existente
     */
    @Transactional
    public DicomFile processFromPath(String dicomPath, Long responsibleUserId) throws IOException {
        log.info("Procesando archivo DICOM desde ruta: {}", dicomPath);

        // 1. Leer metadatos
        DicomFileDTO dicomDTO = dicomReaderService.readDicomFileAsDTO(dicomPath);

        // 2. Buscar o crear paciente
        Patient patient = findOrCreatePatient(dicomDTO.getPatientId(), dicomDTO.getMetadata(), responsibleUserId);

        // 3. Crear entidad
        DicomFile dicomFile = mapToEntity(dicomDTO, patient, dicomPath);

        // 4. Guardar en BD
        return dicomFileRepository.save(dicomFile);
    }


    /**
     * Obtiene metadatos como Map desde el JSON almacenado
     */
    public Map<String, String> getMetadataAsMap(DicomFile dicomFile) {
        try {
            if (dicomFile.getAllMetadata() != null && !dicomFile.getAllMetadata().isEmpty()) {
                return objectMapper.readValue(dicomFile.getAllMetadata(), Map.class);
            }
        } catch (JsonProcessingException e) {
            log.error("Error parseando metadatos JSON del DicomFile {}", dicomFile.getId(), e);
        }
        return new HashMap<>();
    }

    /**
     * Obtiene un metadato específico por clave
     */
    public String getMetadataValue(DicomFile dicomFile, String key) {
        Map<String, String> metadata = getMetadataAsMap(dicomFile);
        return metadata.get(key);
    }


    /**
     * Busca un paciente existente o crea uno nuevo
     */
    private Patient findOrCreatePatient(String patientId, Map<String, String> metadata, Long responsibleUserId) {
        Optional<Patient> existingPatient = patientRepository.findByPatientId(patientId);

        if (existingPatient.isPresent()) {
            log.debug("Paciente existente encontrado: {}", patientId);
            return existingPatient.get();
        }

        // Crear nuevo paciente
        log.info("Creando nuevo paciente: {}", patientId);
        Patient newPatient = new Patient();
        newPatient.setPatientId(patientId);

        // TODO: implementar busqueda por id de usuario responsable
        newPatient.setResponsibleUser(userRepository.findById(73L).orElse(null));


        newPatient.setName(metadata.getOrDefault("PatientName", "Unknown"));

        // Parsear fecha de nacimiento si existe
        String birthDateStr = metadata.get("PatientBirthDate");
        if (birthDateStr != null && !birthDateStr.isEmpty()) {
            try {
                newPatient.setBirthDate(LocalDate.parse(birthDateStr, DATE_FORMATTER));
            } catch (Exception e) {
                log.warn("No se pudo parsear fecha de nacimiento: {}", birthDateStr);
            }
        }

        newPatient.setSex(metadata.get("PatientSex"));

        return patientRepository.save(newPatient);
    }

    /**
     * Mapea DTO a entidad DicomFile
     */
    private DicomFile mapToEntity(DicomFileDTO dto, Patient patient, String filePath) {
        DicomFile dicomFile = new DicomFile();

        dicomFile.setPatient(patient);
        dicomFile.setFileName(dto.getFileName());
        dicomFile.setFilePath(filePath);

        Map<String, String> metadata = dto.getMetadata();

        // Extraer campos importantes
        dicomFile.setStudyInstanceUID(metadata.get("StudyInstanceUID"));
        dicomFile.setSeriesInstanceUID(metadata.get("SeriesInstanceUID"));
        dicomFile.setSopInstanceUID(metadata.get("SOPInstanceUID"));
        dicomFile.setModality(metadata.get("Modality"));
        dicomFile.setStudyDescription(metadata.get("StudyDescription"));
        dicomFile.setSeriesDescription(metadata.get("SeriesDescription"));

        // Parsear fecha de estudio
        String studyDateStr = metadata.get("StudyDate");
        if (studyDateStr != null && !studyDateStr.isEmpty()) {
            try {
                dicomFile.setStudyDate(LocalDate.parse(studyDateStr, DATE_FORMATTER));
            } catch (Exception e) {
                log.warn("No se pudo parsear study date: {}", studyDateStr);
            }
        }

        dicomFile.setStudyTime(metadata.get("StudyTime"));
        dicomFile.setManufacturer(metadata.get("Manufacturer"));
        dicomFile.setInstitutionName(metadata.get("InstitutionName"));

        // Parsear dimensiones
        try {
            String rowsStr = metadata.get("Rows");
            if (rowsStr != null) dicomFile.setRows(Integer.parseInt(rowsStr));

            String colsStr = metadata.get("Columns");
            if (colsStr != null) dicomFile.setColumns(Integer.parseInt(colsStr));
        } catch (NumberFormatException e) {
            log.warn("Error parseando dimensiones de imagen");
        }

        dicomFile.setTransferSyntaxUID(metadata.get("TransferSyntaxUID"));

        // Serializar todos los metadatos como JSON
        try {
            dicomFile.setAllMetadata(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            log.error("Error serializando metadatos a JSON", e);
            dicomFile.setAllMetadata("{}");
        }

        return dicomFile;
    }
}
