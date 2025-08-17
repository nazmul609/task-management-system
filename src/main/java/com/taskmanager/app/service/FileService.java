package com.taskmanager.app.service;

import com.taskmanager.app.model.FileAttachment;
import com.taskmanager.app.model.Task;
import com.taskmanager.app.model.User;
import com.taskmanager.app.repository.FileAttachmentRepository;
import com.taskmanager.app.repository.TaskRepository;
import com.taskmanager.app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * File Service - Main business logic for file management
 * Integrates FileStorageService with database operations
 */
@Service
@Transactional
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    private final FileAttachmentRepository fileAttachmentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public FileService(FileAttachmentRepository fileAttachmentRepository,
                       TaskRepository taskRepository,
                       UserRepository userRepository,
                       FileStorageService fileStorageService) {
        this.fileAttachmentRepository = fileAttachmentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    // =====================================================
    // CORE FILE OPERATIONS
    // =====================================================

    /**
     * Upload file and attach to task
     */
    public FileAttachment uploadFile(MultipartFile file, Long taskId, String description)
            throws FileStorageService.FileStorageException {

        // Validate task exists
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found with ID: " + taskId));

        // Get current user
        User currentUser = getCurrentUser();

        // Store file physically
        FileStorageService.StoredFileInfo storedFileInfo = fileStorageService.storeFile(file);

        // Create database record
        FileAttachment fileAttachment = new FileAttachment(
                storedFileInfo.getOriginalFilename(),
                storedFileInfo.getStoredFilename(),
                storedFileInfo.getFilePath(),
                storedFileInfo.getContentType(),
                storedFileInfo.getFileSize(),
                task,
                currentUser
        );

        if (description != null && !description.trim().isEmpty()) {
            fileAttachment.setDescription(description.trim());
        }

        FileAttachment savedAttachment = fileAttachmentRepository.save(fileAttachment);

        logger.info("File uploaded successfully: {} for task {} by user {}",
                storedFileInfo.getOriginalFilename(), taskId, currentUser.getUsername());

        return savedAttachment;
    }

    /**
     * Download file
     */
    public FileDownloadInfo downloadFile(Long fileId) throws FileStorageService.FileStorageException {
        // Get file attachment with details
        FileAttachment fileAttachment = fileAttachmentRepository.findByIdWithDetails(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found with ID: " + fileId));

        if (!fileAttachment.getIsActive()) {
            throw new IllegalArgumentException("File has been deleted");
        }

        // Load file resource
        Path filePath = fileStorageService.loadFile(fileAttachment.getStoredFilename());

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileStorageService.FileStorageException("File not readable: " + fileAttachment.getOriginalFilename());
            }

            // Increment download count
            fileAttachment.incrementDownloadCount();
            fileAttachmentRepository.save(fileAttachment);

            return new FileDownloadInfo(resource, fileAttachment);
        } catch (MalformedURLException e) {
            throw new FileStorageService.FileStorageException("File not found: " + fileAttachment.getOriginalFilename(), e);
        }
    }

    /**
     * Delete file (soft delete)
     */
    public boolean deleteFile(Long fileId) {
        Optional<FileAttachment> fileAttachmentOpt = fileAttachmentRepository.findById(fileId);

        if (fileAttachmentOpt.isPresent()) {
            FileAttachment fileAttachment = fileAttachmentOpt.get();

            // Soft delete - mark as inactive
            fileAttachment.setIsActive(false);
            fileAttachmentRepository.save(fileAttachment);

            logger.info("File soft deleted: {} (ID: {})",
                    fileAttachment.getOriginalFilename(), fileId);
            return true;
        }

        return false;
    }

    /**
     * Permanently delete file (hard delete)
     */
    public boolean permanentlyDeleteFile(Long fileId) {
        Optional<FileAttachment> fileAttachmentOpt = fileAttachmentRepository.findById(fileId);

        if (fileAttachmentOpt.isPresent()) {
            FileAttachment fileAttachment = fileAttachmentOpt.get();

            // Delete physical file
            boolean physicalDeleted = fileStorageService.deleteFile(fileAttachment.getFilePath());

            if (physicalDeleted) {
                // Delete database record
                fileAttachmentRepository.delete(fileAttachment);
                logger.info("File permanently deleted: {} (ID: {})",
                        fileAttachment.getOriginalFilename(), fileId);
                return true;
            } else {
                logger.warn("Failed to delete physical file, keeping database record: {}",
                        fileAttachment.getFilePath());
            }
        }

        return false;
    }

    // =====================================================
    // QUERY OPERATIONS
    // =====================================================

    /**
     * Get all attachments for a task
     */
    public List<FileAttachment> getTaskAttachments(Long taskId) {
        return fileAttachmentRepository.findByTaskIdWithUploader(taskId);
    }

    /**
     * Get attachment by ID
     */
    public Optional<FileAttachment> getFileById(Long fileId) {
        return fileAttachmentRepository.findByIdWithDetails(fileId);
    }

    /**
     * Get image attachments for a task
     */
    public List<FileAttachment> getTaskImages(Long taskId) {
        return fileAttachmentRepository.findImagesByTaskId(taskId);
    }

    /**
     * Get document attachments for a task
     */
    public List<FileAttachment> getTaskDocuments(Long taskId) {
        return fileAttachmentRepository.findDocumentsByTaskId(taskId);
    }

    /**
     * Search attachments by filename
     */
    public List<FileAttachment> searchFiles(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return fileAttachmentRepository.findByIsActive(true);
        }
        return fileAttachmentRepository.searchByFilename(keyword.trim());
    }

    /**
     * Search attachments within a task
     */
    public List<FileAttachment> searchTaskFiles(Long taskId, String keyword) {
        return fileAttachmentRepository.searchTaskAttachments(taskId, keyword);
    }

    /**
     * Get files uploaded by user
     */
    public List<FileAttachment> getUserFiles(Long userId) {
        return fileAttachmentRepository.findByUploadedById(userId);
    }

    // =====================================================
    // STATISTICS AND ANALYTICS
    // =====================================================

    /**
     * Get file statistics
     */
    public FileStats getFileStatistics() {
        long totalFiles = fileAttachmentRepository.count();
        long activeFiles = fileAttachmentRepository.findByIsActive(true).size();
        long imageFiles = fileAttachmentRepository.countImages();
        long documentFiles = fileAttachmentRepository.countDocuments();
        long totalStorage = fileAttachmentRepository.getTotalStorageUsed();

        return new FileStats(totalFiles, activeFiles, imageFiles, documentFiles, totalStorage);
    }

    /**
     * Get task file statistics
     */
    public TaskFileStats getTaskFileStats(Long taskId) {
        long fileCount = fileAttachmentRepository.countByTaskId(taskId);
        long totalSize = fileAttachmentRepository.getStorageUsedByTask(taskId);
        List<FileAttachment> images = fileAttachmentRepository.findImagesByTaskId(taskId);
        List<FileAttachment> documents = fileAttachmentRepository.findDocumentsByTaskId(taskId);

        return new TaskFileStats(taskId, fileCount, totalSize, images.size(), documents.size());
    }

    /**
     * Get user file statistics
     */
    public UserFileStats getUserFileStats(Long userId) {
        long fileCount = fileAttachmentRepository.countByUserId(userId);
        long totalSize = fileAttachmentRepository.getStorageUsedByUser(userId);

        return new UserFileStats(userId, fileCount, totalSize);
    }

    // =====================================================
    // UTILITY METHODS
    // =====================================================

    /**
     * Check if user can access file
     */
    public boolean canUserAccessFile(Long fileId, String username) {
        Optional<FileAttachment> fileOpt = fileAttachmentRepository.findById(fileId);

        if (fileOpt.isPresent()) {
            FileAttachment file = fileOpt.get();
            // User can access if they uploaded it or it's active
            return file.getIsActive() &&
                    (file.getUploadedBy().getUsername().equals(username) ||
                            hasTaskAccess(file.getTask().getId(), username));
        }

        return false;
    }

    /**
     * Check if user has access to task (simplified - you might want more complex logic)
     */
    private boolean hasTaskAccess(Long taskId, String username) {
        // For now, assume all authenticated users can access all tasks
        // In a real application, you'd implement proper authorization logic
        return true;
    }

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + username));
    }

    /**
     * Validate file attachment exists and is active
     */
    public boolean isValidAttachment(Long fileId) {
        return fileAttachmentRepository.findById(fileId)
                .map(FileAttachment::getIsActive)
                .orElse(false);
    }

    // =====================================================
    // CLEANUP OPERATIONS
    // =====================================================

    /**
     * Clean up old inactive files
     */
    @Transactional
    public int cleanupOldFiles(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<FileAttachment> oldFiles = fileAttachmentRepository.findOldInactiveAttachments(cutoffDate);

        int deletedCount = 0;
        for (FileAttachment file : oldFiles) {
            if (fileStorageService.deleteFile(file.getFilePath())) {
                fileAttachmentRepository.delete(file);
                deletedCount++;
            }
        }

        logger.info("Cleaned up {} old files older than {} days", deletedCount, daysOld);
        return deletedCount;
    }

    // =====================================================
    // INNER CLASSES FOR STATISTICS
    // =====================================================

    public static class FileStats {
        private final long totalFiles;
        private final long activeFiles;
        private final long imageFiles;
        private final long documentFiles;
        private final long totalStorage;

        public FileStats(long totalFiles, long activeFiles, long imageFiles, long documentFiles, long totalStorage) {
            this.totalFiles = totalFiles;
            this.activeFiles = activeFiles;
            this.imageFiles = imageFiles;
            this.documentFiles = documentFiles;
            this.totalStorage = totalStorage;
        }

        // Getters
        public long getTotalFiles() { return totalFiles; }
        public long getActiveFiles() { return activeFiles; }
        public long getImageFiles() { return imageFiles; }
        public long getDocumentFiles() { return documentFiles; }
        public long getTotalStorage() { return totalStorage; }

        public String getTotalStorageFormatted() {
            return formatFileSize(totalStorage);
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
    }

    public static class TaskFileStats {
        private final Long taskId;
        private final long fileCount;
        private final long totalSize;
        private final long imageCount;
        private final long documentCount;

        public TaskFileStats(Long taskId, long fileCount, long totalSize, long imageCount, long documentCount) {
            this.taskId = taskId;
            this.fileCount = fileCount;
            this.totalSize = totalSize;
            this.imageCount = imageCount;
            this.documentCount = documentCount;
        }

        // Getters
        public Long getTaskId() { return taskId; }
        public long getFileCount() { return fileCount; }
        public long getTotalSize() { return totalSize; }
        public long getImageCount() { return imageCount; }
        public long getDocumentCount() { return documentCount; }
    }

    public static class UserFileStats {
        private final Long userId;
        private final long fileCount;
        private final long totalSize;

        public UserFileStats(Long userId, long fileCount, long totalSize) {
            this.userId = userId;
            this.fileCount = fileCount;
            this.totalSize = totalSize;
        }

        // Getters
        public Long getUserId() { return userId; }
        public long getFileCount() { return fileCount; }
        public long getTotalSize() { return totalSize; }
    }

    /**
     * File download information
     */
    public static class FileDownloadInfo {
        private final Resource resource;
        private final FileAttachment fileAttachment;

        public FileDownloadInfo(Resource resource, FileAttachment fileAttachment) {
            this.resource = resource;
            this.fileAttachment = fileAttachment;
        }

        public Resource getResource() { return resource; }
        public FileAttachment getFileAttachment() { return fileAttachment; }
    }
}