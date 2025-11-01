package com.planimrt;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class MCSTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("TP-CU08-01: Cálculo de MCS con archivo válido")
    void testCalculoMCSConArchivoValido() throws Exception {
        MockMultipartFile archivoDicom = new MockMultipartFile(
                "file",
                "valid_rtplan.dcm",
                "application/dicom",
                getClass().getResourceAsStream("/dicom/valid_rtplan.dcm")
        );

        long uploadStart = System.currentTimeMillis();
        MvcResult uploadResult = mockMvc.perform(multipart("/upload")
                        .file(archivoDicom))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storedFilename").exists())
                .andReturn();
        long uploadTime = System.currentTimeMillis() - uploadStart;

        String storedFilename = JsonPath.read(
                uploadResult.getResponse().getContentAsString(),
                "$.storedFilename");

        System.out.println("✓ Archivo subido: " + storedFilename);

        // Medicion de tiempo
        long processStart = System.currentTimeMillis();
        MvcResult processResult = mockMvc.perform(post("/api/process")
                        .param("storedFilename", storedFilename))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        long processTime = System.currentTimeMillis() - processStart;
        long totalTime = uploadTime + processTime;

        // Extraer y verificar respuesta
        String jsonResponse = processResult.getResponse().getContentAsString();

        System.out.println("\n=== RESPUESTA DEL PROCESAMIENTO ===");
        System.out.println(jsonResponse);
        System.out.println("===================================\n");

        String status = JsonPath.read(jsonResponse, "$.status");

        // Si falló, mostrar errores y abortar
        if ("FAILED".equals(status)) {
            try {
                Object errors = JsonPath.read(jsonResponse, "$.errors");
                System.err.println("\nPROCESAMIENTO FALLÓ");
                System.err.println("Errores: " + errors);

                try {
                    String exception = JsonPath.read(jsonResponse, "$.exception");
                    System.err.println("Tipo de excepción: " + exception);
                } catch (Exception e) {
                    // No hay campo exception
                }

                fail("El procesamiento falló con errores: " + errors);
            } catch (Exception e) {
                fail("El procesamiento falló pero no se pudieron extraer los errores. Response: " + jsonResponse);
            }
        }

        Number mcsNumber = JsonPath.read(jsonResponse, "$.mcs");
        double mcs = mcsNumber.doubleValue();

        // Reporte
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║    TP-CU08-01: Resultados del Test    ║");
        System.out.println("╠════════════════════════════════════════╣");
        System.out.println("║ Archivo: valid_rtplan.dcm              ║");
        System.out.println("║ MCS calculado: " + String.format("%-23s", mcs) + "║");
        System.out.println("║ Status: " + String.format("%-30s", status) + "║");
        System.out.println("╠════════════════════════════════════════╣");
        System.out.println("║ Tiempo de carga: " + String.format("%-20s", uploadTime + " ms") + "║");
        System.out.println("║ Tiempo de procesamiento: " + String.format("%-13s", processTime + " ms") + "║");
        System.out.println("║ Tiempo total: " + String.format("%-23s", totalTime + " ms") + "║");
        System.out.println("╚════════════════════════════════════════╝\n");

        // Aserciones
        assertAll("Verificaciones TP-CU08-01",
                // Verificar que el MCS es válido
                () -> assertNotNull(mcs, "El MCS no debería ser null"),
                () -> assertTrue(mcs >= 0, "El MCS debe ser no negativo"),
                () -> assertTrue(Double.isFinite(mcs), "El MCS debe ser un número finito"),

                // Verificar status
                () -> assertEquals("SUCCESS", status, "El procesamiento debe ser exitoso"),

                // Verificar tiempo de procesamiento (< 5 segundos)
                () -> assertTrue(totalTime < 5000,
                        String.format("El procesamiento completo debe tomar < 5s. Tiempo real: %.2f s",
                                totalTime / 1000.0)),

                // Verificar tiempo de procesamiento del cálculo específicamente
                () -> assertTrue(processTime < 5000,
                        String.format("El cálculo de MCS debe tomar < 5s. Tiempo real: %.2f s",
                                processTime / 1000.0)),

                // Verificar rango del MCS
                () -> assertTrue(mcs < 1.0, "El MCS debe ser menor a 1.0"),
                () -> assertTrue(mcs > 0.0, "El MCS debe ser mayor a 0"),

                // Verificar que el MCS de este archivo es el esperado
                () -> assertEquals(0.000227, mcs, 0.000001, "El MCS debe ser aproximadamente 0.000227")
        );
    }

    @Test
    @DisplayName("TP-RL01: Repetibilidad del cálculo - El mismo DICOM debe producir siempre el mismo MCS")
    void testRepetibilidadCalculo() throws Exception {
        MockMultipartFile archivoDicom = new MockMultipartFile(
                "file",
                "valid_rtplan.dcm",
                "application/dicom",
                getClass().getResourceAsStream("/dicom/valid_rtplan.dcm")
        );

        final int NUM_ITERACIONES = 5;
        List<Double> valoresMCS = new ArrayList<>();

        for (int i = 1; i <= NUM_ITERACIONES; i++) {
            System.out.println("\n--- Iteración " + i + " ---");

            MvcResult uploadResult = mockMvc.perform(multipart("/upload")
                            .file(archivoDicom))
                    .andExpect(status().isOk())
                    .andReturn();

            String storedFilename = JsonPath.read(
                    uploadResult.getResponse().getContentAsString(),
                    "$.storedFilename");

            // Procesar vía API
            MvcResult processResult = mockMvc.perform(post("/api/process")
                            .param("storedFilename", storedFilename))
                    .andExpect(status().isOk())
                    .andReturn();

            String jsonResponse = processResult.getResponse().getContentAsString();
            String status = JsonPath.read(jsonResponse, "$.status");

            // Si falló en esta iteración, mostrar el error
            if ("FAILED".equals(status)) {
                try {
                    Object errors = JsonPath.read(jsonResponse, "$.errors");
                    System.err.println("Iteración " + i + " falló");
                    System.err.println("Errores: " + errors);
                    System.err.println("Response completa: " + jsonResponse);
                    fail("Iteración " + i + " falló con errores: " + errors);
                } catch (Exception e) {
                    fail("Iteración " + i + " falló. Response: " + jsonResponse);
                }
            }

            // Conversión a Double
            Number mcsNumber = JsonPath.read(jsonResponse, "$.mcs");
            double mcs = mcsNumber.doubleValue();

            valoresMCS.add(mcs);
            System.out.println("✓ Iteración " + i + " - MCS: " + mcs);
        }

        double primerMCS = valoresMCS.get(0);
        Set<Double> valoresUnicos = new HashSet<>(valoresMCS);

        System.out.println("\n╔═══════════════════════════════╗");
        System.out.println("║   Test TP-RL01: Resultados   ║");
        System.out.println("╠═══════════════════════════════╣");
        System.out.println("║ MCS constante: " + String.format("%-14s", primerMCS) + "║");
        System.out.println("║ Valores únicos: " + String.format("%-13s", valoresUnicos.size()) + "║");
        System.out.println("║ Iteraciones: " + String.format("%-16s", NUM_ITERACIONES) + "║");
        System.out.println("╚═══════════════════════════════╝\n");

        assertAll("Verificaciones TP-RL01",
                () -> assertEquals(1, valoresUnicos.size(),
                        "Se encontraron " + valoresUnicos.size() +
                                " valores MCS diferentes: " + valoresUnicos),

                () -> {
                    for (int i = 1; i < valoresMCS.size(); i++) {
                        assertEquals(primerMCS, valoresMCS.get(i), 0.0001,
                                "El MCS en la iteración " + (i + 1) + " difiere del primer cálculo");
                    }
                }
        );
    }
}