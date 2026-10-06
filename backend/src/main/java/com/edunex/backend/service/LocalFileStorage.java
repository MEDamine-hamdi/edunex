package com.edunex.backend.service;

import com.edunex.backend.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Development storage on the local disk. Replace with an object storage implementation (R2, Cloudinary) for production. */
@Slf4j
@Component
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(@Value("${app.storage.path:./storage}") String path) {
        this.root = Paths.get(path).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String name = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1);
        String safeName = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safeName.isBlank()) {
            safeName = "file";
        }
        String key = UUID.randomUUID() + "/" + safeName;
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not store file", e);
        }
        return key;
    }

    @Override
    public Resource load(String key) {
        Path path = resolve(key);
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("File not found");
        }
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String key) {
        try {
            Path path = resolve(key);
            Files.deleteIfExists(path);
            Path dir = path.getParent();
            if (dir != null && !dir.equals(root)) {
                Files.deleteIfExists(dir);
            }
        } catch (IOException e) {
            log.warn("Could not delete stored file {}", key, e);
        }
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid file key");
        }
        return path;
    }
}
