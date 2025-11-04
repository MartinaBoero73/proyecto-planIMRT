package com.planimrt.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) throws IOException {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    /**
     * Guarda el multipart file en un archivo temporal dentro de uploadDir.
     * Devuelve un id público (UUID) para referenciar el archivo.
     */
    public String store(byte[] content, String originalFilename) throws IOException {
        String ext = FilenameUtils.getExtension(originalFilename);
        String id = UUID.randomUUID().toString();
        String filename = id + (ext.isBlank() ? "" : ("." + ext));
        Path target = uploadDir.resolve(filename);
        Files.write(target, content, StandardOpenOption.CREATE_NEW);
        return filename; // devolvemos el nombre del archivo dentro de uploadDir
    }

    public Path resolve(String storedFilename) {
        return uploadDir.resolve(storedFilename).toAbsolutePath();
    }

    public boolean exists(String storedFilename) {
        return Files.exists(resolve(storedFilename));
    }
}
