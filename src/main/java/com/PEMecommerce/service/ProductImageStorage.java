package com.PEMecommerce.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class ProductImageStorage {

    private static final int MAX_BYTES = 2 * 1024 * 1024;

    private final Path directory;
    private final String publicBaseUrl;

    public ProductImageStorage(@Value("${app.uploads.public-base-url}") String publicBaseUrl) throws IOException {
        this.directory = Path.of("uploads").toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        Files.createDirectories(this.directory);
    }

    public Path getDirectory() {
        return directory;
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ImageUploadException("An image file is required");
        }
        byte[] bytes = file.getBytes();
        if (bytes.length > MAX_BYTES) {
            throw new ImageUploadException("Image must be 2 MB or smaller");
        }
        String extension = extensionFor(bytes);
        if (extension == null) {
            throw new ImageUploadException("Only JPEG, PNG, GIF, and WebP images are allowed");
        }
        String filename = UUID.randomUUID() + extension;
        Path target = directory.resolve(filename).normalize();
        if (!target.startsWith(directory)) {
            throw new ImageUploadException("Could not store the image");
        }
        Files.write(target, bytes);
        return publicBaseUrl + "/" + filename;
    }

    private String extensionFor(byte[] header) {
        if (header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47) {
            return ".png";
        }
        if (header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }
        if (header.length >= 6
                && header[0] == 'G'
                && header[1] == 'I'
                && header[2] == 'F') {
            return ".gif";
        }
        if (header.length >= 12
                && header[0] == 'R'
                && header[1] == 'I'
                && header[2] == 'F'
                && header[3] == 'F'
                && header[8] == 'W'
                && header[9] == 'E'
                && header[10] == 'B'
                && header[11] == 'P') {
            return ".webp";
        }
        return null;
    }
}
