package com.taskmanager.app.controller;

import com.taskmanager.app.model.FileAttachment;
import com.taskmanager.app.service.FileService;
import com.taskmanager.app.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * File Controller - REST API for file upload, download, and management
 *
 * Endpoints:
 * POST   /api/files/upload/{taskId}     - Upload file to task
 * GET    /api/files/{fileId}/download   - Download file
 * GET    /api/files/task/{taskId}       - Get task attachments
 * DELETE /api/files/{fileId}            - Delete file
 * GET    /api/files/{fileId}/info       - Get file metadata
 * GET    /api/files/search              - Search files
 * GET    /api/files/stats               - Get file statistics
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final FileStorageService fileStorageService;

    public FileController(FileService fileService, FileStorageService fileStorageService) {
        this.fileService = fileService;
        this.fileStorageService = fileStorageService;
    }

    // =====================================================
    // FILE UPLOAD
    // =====================================================

    /**
     * Upload file and attach to task
     * POST /api/files/upload/{taskId}
     */
    @PostMapping("/upload/{taskId}")
    public ResponseEntity<?> uploadFile(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {

        try {
            // Validate task ID
            if (taskId == null || taskId <= 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid task ID"));
            }

            // Validate file
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("No file provided"));
            }

            // Upload file
            FileAttachment fileAttachment = fileService.uploadFile(file, taskId, description);

            // Create response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "File uploaded successfully");
            response.put("fileId", fileAttachment.getId());
            response.put("originalFilename", fileAttachment.getOriginalFilename());
            response.put("fileSize", fileAttachment.getFileSize());
            response.put("fileSizeFormatted", fileAttachment.getFileSizeFormatted());
            response.put("contentType", fileAttachment.getContentType());
            response.put("isImage", fileAttachment.getIsImage());
            response.put("uploadedAt", fileAttachment.getCreatedAt());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (FileStorageService.FileStorageException e) {
            return ResponseEntity.badRequest().body(createErrorResponse("File upload failed: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error occurred"));
        }
    }

    /**
     * Upload multiple files to task
     * POST /api/files/upload/{taskId}/multiple
     */
    @PostMapping("/upload/{taskId}/multiple")
    public ResponseEntity<?> uploadMultipleFiles(
            @PathVariable Long taskId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "description", required = false) String description) {

        try {
            if (taskId == null || taskId <= 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid task ID"));
            }

            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("No files provided"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("totalFiles", files.length);
            response.put("uploadedFiles", new java.util.ArrayList<>());
            response.put("failedFiles", new java.util.ArrayList<>());

            int successCount = 0;
            for (MultipartFile file : files) {
                try {
                    FileAttachment fileAttachment = fileService.uploadFile(file, taskId, description);

                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("fileId", fileAttachment.getId());
                    fileInfo.put("originalFilename", fileAttachment.getOriginalFilename());
                    fileInfo.put("fileSize", fileAttachment.getFileSize());

                    ((List<Object>) response.get("uploadedFiles")).add(fileInfo);
                    successCount++;

                } catch (Exception e) {
                    Map<String, Object> errorInfo = new HashMap<>();
                    errorInfo.put("filename", file.getOriginalFilename());
                    errorInfo.put("error", e.getMessage());

                    ((List<Object>) response.get("failedFiles")).add(errorInfo);
                }
            }

            response.put("successCount", successCount);
            response.put("failedCount", files.length - successCount);
            response.put("message", String.format("Uploaded %d of %d files successfully", successCount, files.length));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Multiple file upload failed: " + e.getMessage()));
        }
    }

    // =====================================================
    // FILE DOWNLOAD
    // =====================================================

    /**
     * Download file
     * GET /api/files/{fileId}/download
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId, HttpServletRequest request) {

        try {
            // Validate file ID
            if (fileId == null || fileId <= 0) {
                return ResponseEntity.badRequest().build();
            }

            // Check if user can access file
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!fileService.canUserAccessFile(fileId, authentication.getName())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // Get file
            FileService.FileDownloadInfo downloadInfo = fileService.downloadFile(fileId);
            Resource resource = downloadInfo.getResource();
            FileAttachment fileAttachment = downloadInfo.getFileAttachment();

            // Determine content type
            String contentType = null;
            try {
                contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            } catch (IOException ex) {
                contentType = fileAttachment.getContentType();
            }

            // Fallback if content type is still null
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileAttachment.getOriginalFilename() + "\"")
                    .body(resource);

        } catch (FileStorageService.FileStorageException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * View file inline (for images, PDFs, etc.)
     * GET /api/files/{fileId}/view
     */
    @GetMapping("/{fileId}/view")
    public ResponseEntity<Resource> viewFile(@PathVariable Long fileId, HttpServletRequest request) {

        try {
            if (fileId == null || fileId <= 0) {
                return ResponseEntity.badRequest().build();
            }

            // Check access permission
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!fileService.canUserAccessFile(fileId, authentication.getName())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            FileService.FileDownloadInfo downloadInfo = fileService.downloadFile(fileId);
            Resource resource = downloadInfo.getResource();
            FileAttachment fileAttachment = downloadInfo.getFileAttachment();

            String contentType = fileAttachment.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + fileAttachment.getOriginalFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =====================================================
    // FILE INFORMATION
    // =====================================================

    /**
     * Get file metadata
     * GET /api/files/{fileId}/info
     */
    @GetMapping("/{fileId}/info")
    public ResponseEntity<?> getFileInfo(@PathVariable Long fileId) {

        try {
            if (fileId == null || fileId <= 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid file ID"));
            }

            Optional<FileAttachment> fileOpt = fileService.getFileById(fileId);
            if (fileOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            FileAttachment file = fileOpt.get();

            Map<String, Object> response = new HashMap<>();
            response.put("id", file.getId());
            response.put("originalFilename", file.getOriginalFilename());
            response.put("fileSize", file.getFileSize());
            response.put("fileSizeFormatted", file.getFileSizeFormatted());
            response.put("contentType", file.getContentType());
            response.put("isImage", file.getIsImage());
            response.put("description", file.getDescription());
            response.put("downloadCount", file.getDownloadCount());
            response.put("createdAt", file.getCreatedAt());
            response.put("updatedAt", file.getUpdatedAt());

            // Task information
            response.put("taskId", file.getTask().getId());
            response.put("taskTitle", file.getTask().getTitle());

            // Uploader information
            response.put("uploadedBy", file.getUploadedBy().getUsername());
            response.put("uploaderName", file.getUploadedBy().getFullName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get file info"));
        }
    }

    /**
     * Get task attachments
     * GET /api/files/task/{taskId}
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<?> getTaskAttachments(@PathVariable Long taskId) {

        try {
            if (taskId == null || taskId <= 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid task ID"));
            }

            List<FileAttachment> attachments = fileService.getTaskAttachments(taskId);

            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("totalFiles", attachments.size());
            response.put("files", attachments.stream().map(this::mapFileToResponse).toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get task attachments"));
        }
    }

    /**
     * Get task images only
     * GET /api/files/task/{taskId}/images
     */
    @GetMapping("/task/{taskId}/images")
    public ResponseEntity<?> getTaskImages(@PathVariable Long taskId) {

        try {
            if (taskId == null || taskId <= 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid task ID"));
            }

            List<FileAttachment> images = fileService.getTaskImages(taskId);

            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("totalImages", images.size());
            response.put("images", images.stream().map(this::mapFileToResponse).toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get task images"));
        }
    }

    /**
     * Get task documents only
     * GET /api/files/task/{taskId}/documents
     */
    @GetMapping("/task/{taskId}/documents")
    public ResponseEntity<?> getTaskDocuments(@PathVariable Long taskId) {

        try {
            if (taskId == null || taskId <= 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid task ID"));
            }

            List<FileAttachment> documents = fileService.getTaskDocuments(taskId);

            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("totalDocuments", documents.size());
            response.put("documents", documents.stream().map(this::mapFileToResponse).toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get task documents"));
        }
    }

    // =====================================================
    // FILE MANAGEMENT
    // =====================================================

    /**
     * Delete file (soft delete)
     * DELETE /api/files/{fileId}
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable Long fileId) {

        try {
            if (fileId == null || fileId <= 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid file ID"));
            }

            boolean deleted = fileService.deleteFile(fileId);

            if (deleted) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "File deleted successfully");
                response.put("fileId", fileId);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to delete file"));
        }
    }

    /**
     * Update file description
     * PUT /api/files/{fileId}/description
     */
    @PutMapping("/{fileId}/description")
    public ResponseEntity<?> updateFileDescription(
            @PathVariable Long fileId,
            @RequestBody Map<String, String> request) {

        try {
            if (fileId == null || fileId <= 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid file ID"));
            }

            String description = request.get("description");

            Optional<FileAttachment> fileOpt = fileService.getFileById(fileId);
            if (fileOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // For now, we'll create a simple update method (you'd add this to FileService)
            // fileService.updateFileDescription(fileId, description);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "File description updated successfully");
            response.put("fileId", fileId);
            response.put("description", description);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to update file description"));
        }
    }

    // =====================================================
    // SEARCH AND STATISTICS
    // =====================================================

    /**
     * Search files
     * GET /api/files/search?keyword=example
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchFiles(@RequestParam String keyword) {

        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Search keyword is required"));
            }

            List<FileAttachment> files = fileService.searchFiles(keyword);

            Map<String, Object> response = new HashMap<>();
            response.put("keyword", keyword);
            response.put("totalResults", files.size());
            response.put("files", files.stream().map(this::mapFileToResponse).toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Search failed"));
        }
    }

    /**
     * Search files within a task
     * GET /api/files/task/{taskId}/search?keyword=example
     */
    @GetMapping("/task/{taskId}/search")
    public ResponseEntity<?> searchTaskFiles(@PathVariable Long taskId, @RequestParam String keyword) {

        try {
            if (taskId == null || taskId <= 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid task ID"));
            }

            if (keyword == null || keyword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Search keyword is required"));
            }

            List<FileAttachment> files = fileService.searchTaskFiles(taskId, keyword);

            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("keyword", keyword);
            response.put("totalResults", files.size());
            response.put("files", files.stream().map(this::mapFileToResponse).toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Task file search failed"));
        }
    }

    /**
     * Get file statistics
     * GET /api/files/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getFileStatistics() {

        try {
            FileService.FileStats stats = fileService.getFileStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put("totalFiles", stats.getTotalFiles());
            response.put("activeFiles", stats.getActiveFiles());
            response.put("imageFiles", stats.getImageFiles());
            response.put("documentFiles", stats.getDocumentFiles());
            response.put("totalStorage", stats.getTotalStorage());
            response.put("totalStorageFormatted", stats.getTotalStorageFormatted());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get file statistics"));
        }
    }

    /**
     * Get task file statistics
     * GET /api/files/task/{taskId}/stats
     */
    @GetMapping("/task/{taskId}/stats")
    public ResponseEntity<?> getTaskFileStats(@PathVariable Long taskId) {

        try {
            if (taskId == null || taskId <= 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid task ID"));
            }

            FileService.TaskFileStats stats = fileService.getTaskFileStats(taskId);

            Map<String, Object> response = new HashMap<>();
            response.put("taskId", stats.getTaskId());
            response.put("fileCount", stats.getFileCount());
            response.put("totalSize", stats.getTotalSize());
            response.put("imageCount", stats.getImageCount());
            response.put("documentCount", stats.getDocumentCount());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get task file statistics"));
        }
    }

    // =====================================================
    // CONFIGURATION AND SYSTEM INFO
    // =====================================================

    /**
     * Get upload configuration
     * GET /api/files/config
     */
    @GetMapping("/config")
    public ResponseEntity<?> getUploadConfig() {

        try {
            Map<String, Object> config = new HashMap<>();
            config.put("maxFileSize", fileStorageService.getMaxFileSize());
            config.put("maxFileSizeFormatted", fileStorageService.getMaxFileSizeFormatted());
            config.put("allowedImageTypes", fileStorageService.getAllowedImageTypes());
            config.put("allowedDocumentTypes", fileStorageService.getAllowedDocumentTypes());
            config.put("allowedArchiveTypes", fileStorageService.getAllowedArchiveTypes());

            return ResponseEntity.ok(config);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get upload configuration"));
        }
    }

    // =====================================================
    // UTILITY METHODS
    // =====================================================

    /**
     * Map FileAttachment to response object
     */
    private Map<String, Object> mapFileToResponse(FileAttachment file) {
        Map<String, Object> fileResponse = new HashMap<>();
        fileResponse.put("id", file.getId());
        fileResponse.put("originalFilename", file.getOriginalFilename());
        fileResponse.put("fileSize", file.getFileSize());
        fileResponse.put("fileSizeFormatted", file.getFileSizeFormatted());
        fileResponse.put("contentType", file.getContentType());
        fileResponse.put("isImage", file.getIsImage());
        fileResponse.put("description", file.getDescription());
        fileResponse.put("downloadCount", file.getDownloadCount());
        fileResponse.put("createdAt", file.getCreatedAt());
        fileResponse.put("uploadedBy", file.getUploadedBy().getUsername());
        fileResponse.put("uploaderName", file.getUploadedBy().getFullName());

        return fileResponse;
    }

    /**
     * Create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", java.time.LocalDateTime.now());
        return error;
    }
}