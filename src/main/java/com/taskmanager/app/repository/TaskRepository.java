package com.taskmanager.app.repository;

import com.taskmanager.app.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Spring Data JPA provides these methods automatically:
    // - save(Task task)           -> Create/Update task
    // - findById(Long id)         -> Find task by ID
    // - findAll()                 -> Get all tasks
    // - deleteById(Long id)       -> Delete task by ID
    // - count()                   -> Count total tasks
    // - existsById(Long id)       -> Check if task exists

    // Custom query methods using method naming convention
    List<Task> findByStatus(String status);

    List<Task> findByTitleContaining(String keyword);

    List<Task> findByStatusAndTitleContaining(String status, String keyword);

    List<Task> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Custom queries using @Query annotation
    @Query("SELECT t FROM Task t WHERE t.status = :status ORDER BY t.createdAt DESC")
    List<Task> findTasksByStatusOrderByCreatedAt(@Param("status") String status);

    @Query("SELECT t FROM Task t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Task> searchTasks(@Param("keyword") String keyword);

    // Native SQL query (for complex operations)
    @Query(value = "SELECT * FROM tasks WHERE status = ?1 AND created_at >= ?2", nativeQuery = true)
    List<Task> findRecentTasksByStatus(String status, LocalDateTime since);

    // Count queries
    @Query("SELECT COUNT(t) FROM Task t WHERE t.status = :status")
    long countByStatus(@Param("status") String status);

    // Check if task exists with specific title
    boolean existsByTitle(String title);

    // Delete by status
    void deleteByStatus(String status);
}