package com.evoting.evotingsystem.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp"
    );

    private final Path baseDir;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.baseDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public String storeProfilePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only PNG/JPG/WEBP images are allowed.");
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) {
            ext = original.substring(dot).toLowerCase();
        }
        if (ext.isBlank()) {
            ext = contentType.equals("image/png") ? ".png" : ".jpg";
        }

        Path targetDir = baseDir.resolve("profile");
        try {
            Files.createDirectories(targetDir);
            String filename = UUID.randomUUID() + ext;
            Path target = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/profile/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store profile photo.", e);
        }
    }

    public String storeCandidatePhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only PNG/JPG/WEBP images are allowed.");
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) {
            ext = original.substring(dot).toLowerCase();
        }
        if (ext.isBlank()) {
            ext = contentType.equals("image/png") ? ".png" : ".jpg";
        }

        Path targetDir = baseDir.resolve("candidates");
        try {
            Files.createDirectories(targetDir);
            String filename = UUID.randomUUID() + ext;
            Path target = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/candidates/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store candidate photo.", e);
        }
    }
}

