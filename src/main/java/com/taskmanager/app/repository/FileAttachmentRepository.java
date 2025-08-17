package com.taskmanager.app.repository;

import com.taskmanager.app.model.FileAttachment;
import com.taskmanager.app.model.Task;
import com.taskmanager.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {

    // =====================================================
    // BASIC QUERIES
    // =====================================================

    // Find attachments by task
    List<FileAttachment> findByTask(Task task);

    // Find attachments by task ID
    List<FileAttachment> findByTaskId(Long taskId);

    // Find attachments by task ID and active status
    List<FileAttachment> findByTaskIdAndIsActive(Long taskId, Boolean isActive);

    // Find attachments by user who uploaded
    List<FileAttachment> findByUploadedBy(User user);

    // Find attachments by user ID
    List<FileAttachment> findByUploadedById(Long userId);

    // Find active attachments
    List<FileAttachment> findByIsActive(Boolean isActive);

    // =====================================================
    // CONTENT TYPE QUERIES
    // =====================================================

    // Find attachments by content type
    List<FileAttachment> findByContentType(String contentType);

    // Find image attachments
    List<FileAttachment> findByIsImage(Boolean isImage);

    // Find attachments by content type pattern
    @Query("SELECT f FROM FileAttachment f WHERE f.contentType LIKE :pattern AND f.isActive = true")
    List<FileAttachment> findByContentTypePattern(@Param("pattern") String pattern);

    // Find image attachments for a task
    @Query("SELECT f FROM FileAttachment f WHERE f.task.id = :taskId AND f.isImage = true AND f.isActive = true")
    List<FileAttachment> findImagesByTaskId(@Param("taskId") Long taskId);

    // Find document attachments for a task
    @Query("SELECT f FROM FileAttachment f WHERE f.task.id = :taskId AND f.isImage = false AND f.isActive = true")
    List<FileAttachment> findDocumentsByTaskId(@Param("taskId") Long taskId);

    // =====================================================
    // SEARCH QUERIES
    // =====================================================

    // Search attachments by filename
    @Query("SELECT f FROM FileAttachment f WHERE " +
            "LOWER(f.originalFilename) LIKE LOWER(CONCAT('%', :keyword, '%')) AND f.isActive = true")
    List<FileAttachment> searchByFilename(@Param("keyword") String keyword);

    // Search attachments by description
    @Query("SELECT f FROM FileAttachment f WHERE " +
            "LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%')) AND f.isActive = true")
    List<FileAttachment> searchByDescription(@Param("keyword") String keyword);

    // Search attachments for a specific task
    @Query("SELECT f FROM FileAttachment f WHERE f.task.id = :taskId AND " +
            "(LOWER(f.originalFilename) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND f.isActive = true")
    List<FileAttachment> searchTaskAttachments(@Param("taskId") Long taskId, @Param("keyword") String keyword);

    // =====================================================
    // SIZE AND DATE QUERIES
    // =====================================================

    // Find large files (over specified size)
    @Query("SELECT f FROM FileAttachment f WHERE f.fileSize > :sizeInBytes AND f.isActive = true")
    List<FileAttachment> findLargeFiles(@Param("sizeInBytes") Long sizeInBytes);

    // Find files uploaded between dates
    List<FileAttachment> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Find recent attachments for a task
    @Query("SELECT f FROM FileAttachment f WHERE f.task.id = :taskId AND f.isActive = true ORDER BY f.createdAt DESC")
    List<FileAttachment> findRecentByTaskId(@Param("taskId") Long taskId);

    // Find most downloaded files
    @Query("SELECT f FROM FileAttachment f WHERE f.isActive = true ORDER BY f.downloadCount DESC")
    List<FileAttachment> findMostDownloaded();

    // =====================================================
    // STATISTICS QUERIES
    // =====================================================

    // Count attachments by task
    @Query("SELECT COUNT(f) FROM FileAttachment f WHERE f.task.id = :taskId AND f.isActive = true")
    long countByTaskId(@Param("taskId") Long taskId);

    // Count attachments by user
    @Query("SELECT COUNT(f) FROM FileAttachment f WHERE f.uploadedBy.id = :userId AND f.isActive = true")
    long countByUserId(@Param("userId") Long userId);

    // Count image attachments
    @Query("SELECT COUNT(f) FROM FileAttachment f WHERE f.isImage = true AND f.isActive = true")
    long countImages();

    // Count document attachments
    @Query("SELECT COUNT(f) FROM FileAttachment f WHERE f.isImage = false AND f.isActive = true")
    long countDocuments();

    // Get total storage used
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileAttachment f WHERE f.isActive = true")
    long getTotalStorageUsed();

    // Get storage used by task
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileAttachment f WHERE f.task.id = :taskId AND f.isActive = true")
    long getStorageUsedByTask(@Param("taskId") Long taskId);

    // Get storage used by user
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileAttachment f WHERE f.uploadedBy.id = :userId AND f.isActive = true")
    long getStorageUsedByUser(@Param("userId") Long userId);

    // =====================================================
    // VALIDATION QUERIES
    // =====================================================

    // Check if stored filename exists
    boolean existsByStoredFilename(String storedFilename);

    // Check if file path exists
    boolean existsByFilePath(String filePath);

    // Find attachment by stored filename
    Optional<FileAttachment> findByStoredFilename(String storedFilename);

    // Find attachment by file path
    Optional<FileAttachment> findByFilePath(String filePath);

    // =====================================================
    // ADVANCED QUERIES
    // =====================================================

    // Find attachments with thumbnails
    @Query("SELECT f FROM FileAttachment f WHERE f.thumbnailPath IS NOT NULL AND f.isActive = true")
    List<FileAttachment> findWithThumbnails();

    // Find attachments without thumbnails (images only)
    @Query("SELECT f FROM FileAttachment f WHERE f.isImage = true AND f.thumbnailPath IS NULL AND f.isActive = true")
    List<FileAttachment> findImagesWithoutThumbnails();

    // Get file statistics by content type
    @Query("SELECT f.contentType, COUNT(f), SUM(f.fileSize) FROM FileAttachment f " +
            "WHERE f.isActive = true GROUP BY f.contentType ORDER BY COUNT(f) DESC")
    List<Object[]> getFileStatisticsByContentType();

    // Get upload statistics by user
    @Query("SELECT u.username, COUNT(f), SUM(f.fileSize) FROM FileAttachment f " +
            "JOIN f.uploadedBy u WHERE f.isActive = true " +
            "GROUP BY u.id, u.username ORDER BY COUNT(f) DESC")
    List<Object[]> getUploadStatisticsByUser();

    // Find duplicate files (same size and name)
    @Query("SELECT f FROM FileAttachment f WHERE f.isActive = true AND " +
            "EXISTS (SELECT f2 FROM FileAttachment f2 WHERE f2.id != f.id AND " +
            "f2.originalFilename = f.originalFilename AND f2.fileSize = f.fileSize AND f2.isActive = true)")
    List<FileAttachment> findPotentialDuplicates();

    // =====================================================
    // CLEANUP QUERIES
    // =====================================================

    // Find inactive attachments
    List<FileAttachment> findByIsActiveFalse();

    // Find old inactive attachments for cleanup
    @Query("SELECT f FROM FileAttachment f WHERE f.isActive = false AND f.updatedAt < :cutoffDate")
    List<FileAttachment> findOldInactiveAttachments(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Soft delete all attachments for a task
    @Query("UPDATE FileAttachment f SET f.isActive = false, f.updatedAt = CURRENT_TIMESTAMP WHERE f.task.id = :taskId")
    int softDeleteByTaskId(@Param("taskId") Long taskId);

    // =====================================================
    // EFFICIENT LOADING
    // =====================================================

    // Load attachment with task and user details
    @Query("SELECT f FROM FileAttachment f " +
            "LEFT JOIN FETCH f.task " +
            "LEFT JOIN FETCH f.uploadedBy " +
            "WHERE f.id = :id AND f.isActive = true")
    Optional<FileAttachment> findByIdWithDetails(@Param("id") Long id);

    // Load all attachments for a task with user details
    @Query("SELECT f FROM FileAttachment f " +
            "LEFT JOIN FETCH f.uploadedBy " +
            "WHERE f.task.id = :taskId AND f.isActive = true " +
            "ORDER BY f.createdAt DESC")
    List<FileAttachment> findByTaskIdWithUploader(@Param("taskId") Long taskId);
}