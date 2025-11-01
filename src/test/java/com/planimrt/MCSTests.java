package com.planimrt;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class MCSTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("TP-CU08-02: Repetibilidad del cálculo - El mismo DICOM debe producir siempre el mismo MCS")
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
                    .andExpect(jsonPath("$.mcs").exists())
                    .andReturn();

            // Convesión a Double
            Number mcsNumber = JsonPath.read(
                    processResult.getResponse().getContentAsString(),
                    "$.mcs");
            double mcs = mcsNumber.doubleValue();

            valoresMCS.add(mcs);
            System.out.println("Iteración " + i + " - MCS: " + mcs);
        }

        double primerMCS = valoresMCS.get(0);
        Set<Double> valoresUnicos = new HashSet<>(valoresMCS);

        System.out.println("\n=== Test Repetibilidad ===");
        System.out.println("MCS constante: " + primerMCS);
        System.out.println("Valores únicos: " + valoresUnicos.size());
        System.out.println("==========================\n");

        assertEquals(1, valoresUnicos.size(),
                "Se encontraron " + valoresUnicos.size() +
                        " valores MCS diferentes: " + valoresUnicos);

        for (int i = 1; i < valoresMCS.size(); i++) {
            assertEquals(primerMCS, valoresMCS.get(i), 0.0001,
                    "El MCS en la iteración " + (i + 1) + " difiere del primer cálculo");
        }
    }

}
