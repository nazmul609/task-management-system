package com.taskmanager.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_attachments")
public class FileAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_filename", nullable = false, length = 255)
    @NotBlank(message = "Original filename is required")
    @Size(max = 255, message = "Filename cannot exceed 255 characters")
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 255)
    @NotBlank(message = "Stored filename is required")
    private String storedFilename;

    @Column(name = "file_path", nullable = false, length = 500)
    @NotBlank(message = "File path is required")
    private String filePath;

    @Column(name = "content_type", nullable = false, length = 100)
    @NotBlank(message = "Content type is required")
    private String contentType;

    @Column(name = "file_size", nullable = false)
    @Min(value = 1, message = "File size must be greater than 0")
    private Long fileSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // =====================================================
    // RELATIONSHIPS
    // =====================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    // =====================================================
    // ADDITIONAL METADATA
    // =====================================================

    @Column(name = "description", length = 500)
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Column(name = "is_image")
    private Boolean isImage = false;

    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;

    @Column(name = "download_count", nullable = false)
    private Long downloadCount = 0L;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    public FileAttachment() {}

    public FileAttachment(String originalFilename, String storedFilename, String filePath,
                          String contentType, Long fileSize, Task task, User uploadedBy) {
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.filePath = filePath;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.task = task;
        this.uploadedBy = uploadedBy;
        this.isImage = isImageFile(contentType);
    }

    // =====================================================
    // JPA LIFECYCLE METHODS
    // =====================================================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (downloadCount == null) downloadCount = 0L;
        if (isActive == null) isActive = true;
        if (isImage == null) isImage = isImageFile(contentType);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    public String getFileExtension() {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "";
    }

    public String getFileSizeFormatted() {
        if (fileSize == null) return "0 B";

        double size = fileSize.doubleValue();
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", size, units[unitIndex]);
    }

    public boolean hasThumbnail() {
        return thumbnailPath != null && !thumbnailPath.trim().isEmpty();
    }

    public static boolean isImageFile(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    public boolean isDocument() {
        if (contentType == null) return false;
        return contentType.equals("application/pdf") ||
                contentType.equals("application/msword") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                contentType.equals("text/plain");
    }

    public boolean isArchive() {
        if (contentType == null) return false;
        return contentType.equals("application/zip") ||
                contentType.equals("application/x-rar-compressed") ||
                contentType.equals("application/x-7z-compressed");
    }

    // =====================================================
    // GETTERS AND SETTERS
    // =====================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsImage() {
        return isImage;
    }

    public void setIsImage(Boolean isImage) {
        this.isImage = isImage;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public Long getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    // =====================================================
    // toString (excluding binary data and avoiding circular references)
    // =====================================================

    @Override
    public String toString() {
        return "FileAttachment{" +
                "id=" + id +
                ", originalFilename='" + originalFilename + '\'' +
                ", storedFilename='" + storedFilename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", fileSize=" + fileSize +
                ", isImage=" + isImage +
                ", downloadCount=" + downloadCount +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                '}';
    }
}