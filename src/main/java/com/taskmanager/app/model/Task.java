package com.taskmanager.app.model;

import com.taskmanager.app.validation.ValidTaskStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity // This tells JPA this is a database table
@Table(name = "tasks") // Optional: specify table name
public class Task {

    @Id // Primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment
    private Long id;

    @Column(name = "title", nullable = false, length = 100) // Database column mapping
    @NotBlank(message = "Title is required and cannot be empty")
    @Size(min = 1, max = 100, message = "Title must be between 1 and 100 characters")
    private String title;

    @Column(name = "description", length = 500) // Can be null
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    @NotBlank(message = "Status is required")
    @ValidTaskStatus
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // =====================================================
    // NEW: RELATIONSHIP - Many Tasks belong to One User
    // =====================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true) // Foreign key column
    private User user;

    // Default constructor (required by JPA)
    public Task() {
    }

    // Constructor for manual creation
    public Task(String title, String description, String status) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor with user
    public Task(String title, String description, String status, User user) {
        this(title, description, status);
        this.user = user;
    }

    // JPA lifecycle methods
    @PrePersist // Called before saving to database
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate // Called before updating in database
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

    // Check if task is assigned to a user
    public boolean isAssigned() {
        return user != null;
    }

    // Get user's full name (safe method that handles null user)
    public String getAssignedToName() {
        return user != null ? user.getFullName() : "Unassigned";
    }

    // Get username (safe method)
    public String getAssignedToUsername() {
        return user != null ? user.getUsername() : null;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    // NEW: User relationship getter and setter
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
    // =====================================================
// NEW: FILE ATTACHMENT RELATIONSHIP
// =====================================================

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FileAttachment> fileAttachments = new ArrayList<>();

// =====================================================
// FILE ATTACHMENT HELPER METHODS
// =====================================================

    // Add file attachment to task
    public void addFileAttachment(FileAttachment fileAttachment) {
        fileAttachments.add(fileAttachment);
        fileAttachment.setTask(this);
    }

    // Remove file attachment from task
    public void removeFileAttachment(FileAttachment fileAttachment) {
        fileAttachments.remove(fileAttachment);
        fileAttachment.setTask(null);
    }

    // Get active file attachments count
    public int getActiveAttachmentsCount() {
        return (int) fileAttachments.stream()
                .filter(FileAttachment::getIsActive)
                .count();
    }

    // Get image attachments count
    public int getImageAttachmentsCount() {
        return (int) fileAttachments.stream()
                .filter(attachment -> attachment.getIsActive() && attachment.getIsImage())
                .count();
    }

    // Get document attachments count
    public int getDocumentAttachmentsCount() {
        return (int) fileAttachments.stream()
                .filter(attachment -> attachment.getIsActive() && !attachment.getIsImage())
                .count();
    }

    // Check if task has attachments
    public boolean hasAttachments() {
        return fileAttachments.stream().anyMatch(FileAttachment::getIsActive);
    }

    // Get total attachment size
    public long getTotalAttachmentSize() {
        return fileAttachments.stream()
                .filter(FileAttachment::getIsActive)
                .mapToLong(FileAttachment::getFileSize)
                .sum();
    }

// =====================================================
// GETTER AND SETTER FOR FILE ATTACHMENTS
// =====================================================

    public List<FileAttachment> getFileAttachments() {
        return fileAttachments;
    }

    public void setFileAttachments(List<FileAttachment> fileAttachments) {
        this.fileAttachments = fileAttachments;
    }

    // toString for debugging (avoiding circular reference with user)
    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", assignedTo=" + getAssignedToUsername() +
                '}';
    }
}