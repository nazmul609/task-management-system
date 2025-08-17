package com.taskmanager.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

/**
 * File Storage Service
 * Handles local file storage operations with security and organization
 */
@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    // =====================================================
    // CONFIGURATION PROPERTIES
    // =====================================================

    @Value("${app.file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.file.max-file-size:10485760}") // 10MB default
    private long maxFileSize;

    @Value("${app.file.create-thumbnails:true}")
    private boolean createThumbnails;

    // Base upload directory path
    private Path uploadPath;

    // =====================================================
    // ALLOWED FILE TYPES AND SECURITY
    // =====================================================

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            "text/csv"
    );

    private static final Set<String> ALLOWED_ARCHIVE_TYPES = Set.of(
            "application/zip",
            "application/x-rar-compressed",
            "application/x-7z-compressed"
    );

    private static final Set<String> DANGEROUS_EXTENSIONS = Set.of(
            ".exe", ".bat", ".cmd", ".com", ".pif", ".scr", ".vbs", ".js", ".jar", ".app"
    );

    // =====================================================
    // INITIALIZATION
    // =====================================================

    @PostConstruct
    public void init() {
        try {
            this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(this.uploadPath);

            // Create subdirectories for organization
            Files.createDirectories(this.uploadPath.resolve("images"));
            Files.createDirectories(this.uploadPath.resolve("documents"));
            Files.createDirectories(this.uploadPath.resolve("archives"));
            Files.createDirectories(this.uploadPath.resolve("thumbnails"));

            logger.info("File storage initialized at: {}", this.uploadPath);
        } catch (IOException e) {
            logger.error("Failed to initialize file storage directory", e);
            throw new RuntimeException("Could not initialize file storage", e);
        }
    }

    // =====================================================
    // CORE FILE OPERATIONS
    // =====================================================

    /**
     * Store uploaded file
     */
    public StoredFileInfo storeFile(MultipartFile file) throws FileStorageException {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String storedFilename = generateUniqueFilename(originalFilename);
        String contentType = file.getContentType();

        try {
            // Determine subdirectory based on file type
            String subdirectory = determineSubdirectory(contentType);
            Path targetDir = this.uploadPath.resolve(subdirectory);
            Path targetPath = targetDir.resolve(storedFilename);

            // Ensure the target path is within the upload directory (security check)
            if (!targetPath.normalize().startsWith(this.uploadPath.normalize())) {
                throw new FileStorageException("Invalid file path: " + storedFilename);
            }

            // Copy file to target location
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            logger.info("File stored successfully: {} -> {}", originalFilename, targetPath);

            return new StoredFileInfo(
                    originalFilename,
                    storedFilename,
                    targetPath.toString(),
                    contentType,
                    file.getSize(),
                    subdirectory
            );

        } catch (IOException e) {
            logger.error("Failed to store file: {}", originalFilename, e);
            throw new FileStorageException("Failed to store file: " + originalFilename, e);
        }
    }

    /**
     * Load file as resource
     */
    public Path loadFile(String filename) throws FileStorageException {
        try {
            Path filePath = this.uploadPath.resolve(filename).normalize();

            // Security check: ensure file is within upload directory
            if (!filePath.startsWith(this.uploadPath.normalize())) {
                throw new FileStorageException("Invalid file path: " + filename);
            }

            if (!Files.exists(filePath)) {
                throw new FileStorageException("File not found: " + filename);
            }

            return filePath;
        } catch (Exception e) {
            throw new FileStorageException("Failed to load file: " + filename, e);
        }
    }

    /**
     * Delete file
     */
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            boolean deleted = Files.deleteIfExists(path);

            if (deleted) {
                logger.info("File deleted successfully: {}", filePath);
            } else {
                logger.warn("File not found for deletion: {}", filePath);
            }

            return deleted;
        } catch (IOException e) {
            logger.error("Failed to delete file: {}", filePath, e);
            return false;
        }
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.exists(path);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get file size
     */
    public long getFileSize(String filePath) throws FileStorageException {
        try {
            Path path = Paths.get(filePath);
            return Files.size(path);
        } catch (IOException e) {
            throw new FileStorageException("Failed to get file size: " + filePath, e);
        }
    }

    // =====================================================
    // FILE VALIDATION
    // =====================================================

    private void validateFile(MultipartFile file) throws FileStorageException {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("File is empty or null");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new FileStorageException("Filename is empty");
        }

        // Check file size
        if (file.getSize() > maxFileSize) {
            throw new FileStorageException("File size exceeds maximum allowed size of " +
                    formatFileSize(maxFileSize));
        }

        // Check for dangerous file extensions
        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        if (DANGEROUS_EXTENSIONS.contains(fileExtension)) {
            throw new FileStorageException("File type not allowed: " + fileExtension);
        }

        // Validate content type
        String contentType = file.getContentType();
        if (!isAllowedContentType(contentType)) {
            throw new FileStorageException("File type not supported: " + contentType);
        }

        // Additional filename validation
        if (originalFilename.contains("..")) {
            throw new FileStorageException("Filename contains invalid path sequence");
        }
    }

    private boolean isAllowedContentType(String contentType) {
        if (contentType == null) return false;

        return ALLOWED_IMAGE_TYPES.contains(contentType) ||
                ALLOWED_DOCUMENT_TYPES.contains(contentType) ||
                ALLOWED_ARCHIVE_TYPES.contains(contentType);
    }

    // =====================================================
    // UTILITY METHODS
    // =====================================================

    private String generateUniqueFilename(String originalFilename) {
        String fileExtension = getFileExtension(originalFilename);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        return String.format("%s_%s%s", timestamp, uuid, fileExtension);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String determineSubdirectory(String contentType) {
        if (ALLOWED_IMAGE_TYPES.contains(contentType)) {
            return "images";
        } else if (ALLOWED_ARCHIVE_TYPES.contains(contentType)) {
            return "archives";
        } else {
            return "documents";
        }
    }

    private String formatFileSize(long bytes) {
        double size = bytes;
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", size, units[unitIndex]);
    }

    // =====================================================
    // GETTERS FOR CONFIGURATION
    // =====================================================

    public String getUploadDir() {
        return uploadDir;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public String getMaxFileSizeFormatted() {
        return formatFileSize(maxFileSize);
    }

    public Path getUploadPath() {
        return uploadPath;
    }

    public Set<String> getAllowedImageTypes() {
        return new HashSet<>(ALLOWED_IMAGE_TYPES);
    }

    public Set<String> getAllowedDocumentTypes() {
        return new HashSet<>(ALLOWED_DOCUMENT_TYPES);
    }

    public Set<String> getAllowedArchiveTypes() {
        return new HashSet<>(ALLOWED_ARCHIVE_TYPES);
    }

    // =====================================================
    // INNER CLASSES
    // =====================================================

    /**
     * Information about a stored file
     */
    public static class StoredFileInfo {
        private final String originalFilename;
        private final String storedFilename;
        private final String filePath;
        private final String contentType;
        private final long fileSize;
        private final String subdirectory;

        public StoredFileInfo(String originalFilename, String storedFilename, String filePath,
                              String contentType, long fileSize, String subdirectory) {
            this.originalFilename = originalFilename;
            this.storedFilename = storedFilename;
            this.filePath = filePath;
            this.contentType = contentType;
            this.fileSize = fileSize;
            this.subdirectory = subdirectory;
        }

        // Getters
        public String getOriginalFilename() { return originalFilename; }
        public String getStoredFilename() { return storedFilename; }
        public String getFilePath() { return filePath; }
        public String getContentType() { return contentType; }
        public long getFileSize() { return fileSize; }
        public String getSubdirectory() { return subdirectory; }

        @Override
        public String toString() {
            return "StoredFileInfo{" +
                    "originalFilename='" + originalFilename + '\'' +
                    ", storedFilename='" + storedFilename + '\'' +
                    ", contentType='" + contentType + '\'' +
                    ", fileSize=" + fileSize +
                    ", subdirectory='" + subdirectory + '\'' +
                    '}';
        }
    }

    /**
     * Custom exception for file storage operations
     */
    public static class FileStorageException extends Exception {
        public FileStorageException(String message) {
            super(message);
        }

        public FileStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}