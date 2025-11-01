package com.planimrt.controllers;

import com.planimrt.model.ProcessingResult;
import com.planimrt.services.FileStorageService;
import com.planimrt.services.ProcessingOrchestrator;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ProcessingController {

    private final FileStorageService storageService;
    private final ProcessingOrchestrator orchestrator;


    public ProcessingController(FileStorageService storageService, ProcessingOrchestrator orchestrator) {
        this.storageService = storageService;
        this.orchestrator = orchestrator;
    }

    @GetMapping("/upload")
    public String uploadForm() {
        return "upload";
    }

    @PostMapping("/upload")
    @ResponseBody // Importante: devuelve JSON en vez de una vista
    public Map<String, String> handleUploadAjax(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new RuntimeException("Seleccioná un archivo DICOM para subir.");
        }

        String storedFilename = storageService.store(file.getBytes(), file.getOriginalFilename());

        Map<String, String> response = new HashMap<>();
        response.put("storedFilename", storedFilename);
        response.put("originalName", file.getOriginalFilename());
        return response;
    }


    // Procesa el archivo
    @PostMapping("/process")
    public String processFile(@RequestParam("storedFilename") String storedFilename, Model model) {
        try {
            Long responsibleUserId  = 5L;
            if (!storageService.exists(storedFilename)) {
                model.addAttribute("error", "Archivo no encontrado en el servidor.");
                return "upload";
            }

            Path path = storageService.resolve(storedFilename);
            ProcessingResult result = orchestrator.processPlan(path.toString(), responsibleUserId);

            model.addAttribute("status", result.getStatus().name());
            model.addAttribute("mcsIndex", result.getMcsIndex());
            model.addAttribute("errors", result.getErrors());
            model.addAttribute("beams", result.getBeams());

            if (result.getCollimatorPlot() != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(result.getCollimatorPlot(), "png", baos);
                String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                model.addAttribute("plotData", "data:image/png;base64," + base64);
            }

            return "result";

        } catch (Exception e) {
            model.addAttribute("error", "Error procesando el archivo: " + e.getMessage());
            return "upload";
        }
    }

    @PostMapping("/api/process")
    @ResponseBody
    public Map<String, Object> processFileApi(@RequestParam("storedFilename") String storedFilename) throws Exception {
        Long responsibleUserId = 5L;

        if (!storageService.exists(storedFilename)) {
            throw new RuntimeException("Archivo no encontrado en el servidor.");
        }

        Path path = storageService.resolve(storedFilename);
        ProcessingResult result = orchestrator.processPlan(path.toString(), responsibleUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", result.getStatus().name());
        response.put("mcs", result.getMcsIndex());
        response.put("errors", result.getErrors());
        response.put("beamCount", result.getBeams() != null ? result.getBeams().size() : 0);

        return response;
    }
}
