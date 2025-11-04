package com.planimrt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class DicomUploadTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("TP-CU01-01: Carga de archivo DICOM  -	Verificar que el sistema cargue sin error un archivo DICOM válido")
    void testCargaDicomValido() throws Exception {
        MockMultipartFile archivoDicom = new MockMultipartFile(
                "file",
                "valid_rtplan.dcm",
                "application/dicom",
                getClass().getResourceAsStream("/dicom/valid_rtplan.dcm")
        );

        mockMvc.perform(multipart("/upload")
                        .file(archivoDicom))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalName").value("valid_rtplan.dcm"))
                .andExpect(jsonPath("$.storedFilename").exists());
    }

    @Test
    @DisplayName("TP-CU01-02: Intento de carga con archivo no DICOM - Verificar que el sistema rechace archivos con extensión incorrecta.")
    void testCargaArchivoNoValido() throws Exception {
        MockMultipartFile archivoNoValido = new MockMultipartFile(
                "file",
                "sesion1.txt",
                "text/plain",
                getClass().getResourceAsStream("/dicom_txt/sesion1.txt")
        );

        mockMvc.perform(multipart("/upload").file(archivoNoValido))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("El archivo no es un DICOM válido."));
    }

}

//    // TP-CU01-03: carga de archivo DICOM corrupto
//    @Test
//    void testCargaDicomDañado() throws Exception {
//        MockMultipartFile archivoCorrupto = new MockMultipartFile(
//                "file", "dañado.dcm",
//                "application/dicom",
//                "datos-incompletos".getBytes()
//        );
//
//        mockMvc.perform(multipart("/upload")
//                        .file(archivoCorrupto))
//                .andExpect(status().isBadRequest())
//                .andExpect(content().string(containsString("Error al leer el archivo")));
//    }

