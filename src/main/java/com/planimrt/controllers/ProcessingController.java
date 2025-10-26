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

    // Recibe el archivo y lo guarda. Muestra la pag de confirmación.
    @PostMapping("/upload")
    public String handleUpload(@RequestParam("file") MultipartFile file, Model model) throws Exception {
        if (file.isEmpty()) {
            model.addAttribute("error", "Seleccioná un archivo DICOM para subir.");
            return "upload";
        }

        String storedFilename = storageService.store(file.getBytes(), file.getOriginalFilename());
        model.addAttribute("storedFilename", storedFilename);
        model.addAttribute("originalName", file.getOriginalFilename());
        return "confirm";
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

            // Convertir BufferedImage -> base64 para incrustar en <img>
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
}

