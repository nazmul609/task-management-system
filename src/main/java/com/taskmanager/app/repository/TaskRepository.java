package com.taskmanager.app.repository;

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
public interface TaskRepository extends JpaRepository<Task, Long> {

    // =====================================================
    // ORIGINAL METHODS (from Day 4)
    // =====================================================

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

    // =====================================================
    // NEW: USER RELATIONSHIP QUERIES
    // =====================================================

    // Find tasks by user
    List<Task> findByUser(User user);

    // Find tasks by user ID
    List<Task> findByUserId(Long userId);

    // Find tasks by user ID and status
    List<Task> findByUserIdAndStatus(Long userId, String status);

    // Find tasks by username
    @Query("SELECT t FROM Task t WHERE t.user.username = :username")
    List<Task> findByUsername(@Param("username") String username);

    // Find unassigned tasks (tasks without user)
    @Query("SELECT t FROM Task t WHERE t.user IS NULL")
    List<Task> findUnassignedTasks();

    // Find assigned tasks (tasks with user)
    @Query("SELECT t FROM Task t WHERE t.user IS NOT NULL")
    List<Task> findAssignedTasks();

    // Find tasks by user and status ordered by created date
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND t.status = :status ORDER BY t.createdAt DESC")
    List<Task> findByUserIdAndStatusOrderByCreatedAt(@Param("userId") Long userId, @Param("status") String status);

    // Search tasks for specific user
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND " +
            "(LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Task> searchTasksByUser(@Param("userId") Long userId, @Param("keyword") String keyword);

    // Count tasks by user
    @Query("SELECT COUNT(t) FROM Task t WHERE t.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    // Count tasks by user and status
    @Query("SELECT COUNT(t) FROM Task t WHERE t.user.id = :userId AND t.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    // Find overdue tasks for a user (assuming we'll add due_date later)
    // @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND t.dueDate < CURRENT_TIMESTAMP AND t.status != 'DONE'")
    // List<Task> findOverdueTasksByUser(@Param("userId") Long userId);

    // =====================================================
    // USER STATISTICS QUERIES
    // =====================================================

    // Get task statistics by user
    @Query("SELECT " +
            "u.id as userId, " +
            "u.username, " +
            "COUNT(t.id) as totalTasks, " +
            "SUM(CASE WHEN t.status = 'TODO' THEN 1 ELSE 0 END) as todoTasks, " +
            "SUM(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as inProgressTasks, " +
            "SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END) as doneTasks " +
            "FROM User u LEFT JOIN u.tasks t " +
            "WHERE u.active = true " +
            "GROUP BY u.id, u.username " +
            "ORDER BY totalTasks DESC")
    List<Object[]> getTaskStatisticsByUser();

    // Find users with most tasks
    @Query("SELECT t.user FROM Task t " +
            "WHERE t.user IS NOT NULL " +
            "GROUP BY t.user " +
            "ORDER BY COUNT(t.id) DESC")
    List<User> findUsersWithMostTasks();

    // Find users with most completed tasks
    @Query("SELECT t.user FROM Task t " +
            "WHERE t.user IS NOT NULL AND t.status = 'DONE' " +
            "GROUP BY t.user " +
            "ORDER BY COUNT(t.id) DESC")
    List<User> findUsersWithMostCompletedTasks();

    // Count unassigned tasks
    @Query("SELECT COUNT(t) FROM Task t WHERE t.user IS NULL")
    long countUnassignedTasks();

    // Count assigned tasks
    @Query("SELECT COUNT(t) FROM Task t WHERE t.user IS NOT NULL")
    long countAssignedTasks();

    // =====================================================
    // ADVANCED QUERIES
    // =====================================================

    // Find tasks with user details (JOIN FETCH to avoid N+1 problem)
    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.user WHERE t.id = :taskId")
    Optional<Task> findTaskWithUser(@Param("taskId") Long taskId);

    // Find all tasks with their users loaded (efficient for displaying task lists with user info)
    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.user ORDER BY t.createdAt DESC")
    List<Task> findAllTasksWithUsers();

    // Find recent tasks by user with limit
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId ORDER BY t.createdAt DESC")
    List<Task> findRecentTasksByUser(@Param("userId") Long userId);

    // Check if user has any tasks
    boolean existsByUserId(Long userId);

    // Find tasks created by user between dates
    List<Task> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
}