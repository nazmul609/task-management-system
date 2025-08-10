package com.taskmanager.app.repository;

import com.taskmanager.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // =====================================================
    // BUILT-IN METHODS FROM JpaRepository:
    // - save(User user)           -> Create/Update user
    // - findById(Long id)         -> Find user by ID
    // - findAll()                 -> Get all users
    // - deleteById(Long id)       -> Delete user by ID
    // - count()                   -> Count total users
    // - existsById(Long id)       -> Check if user exists
    // =====================================================

    // =====================================================
    // CUSTOM QUERY METHODS - Method Naming Convention
    // =====================================================

    // Find user by username (for login)
    Optional<User> findByUsername(String username);

    // Find user by email (for registration validation)
    Optional<User> findByEmail(String email);

    // Find users by active status
    List<User> findByActive(Boolean active);

    // Find users by first or last name
    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName);

    // Find users created between dates
    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Check if username exists (for validation)
    boolean existsByUsername(String username);

    // Check if email exists (for validation)
    boolean existsByEmail(String email);

    // =====================================================
    // CUSTOM QUERIES WITH @Query ANNOTATION
    // =====================================================

    // Find active users ordered by creation date
    @Query("SELECT u FROM User u WHERE u.active = true ORDER BY u.createdAt DESC")
    List<User> findActiveUsersOrderByCreatedAt();

    // Search users by keyword (searches in username, firstName, lastName, email)
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchUsers(@Param("keyword") String keyword);

    // Find users with their task count
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.tasks WHERE u.active = true")
    List<User> findActiveUsersWithTasks();

    // Count active users
    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    long countActiveUsers();

    // Find users by full name (combined search)
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :fullName, '%'))")
    List<User> findByFullNameContaining(@Param("fullName") String fullName);

    // =====================================================
    // STATISTICS QUERIES
    // =====================================================

    // Get users with task statistics
    @Query("SELECT u.id as userId, u.username, u.firstName, u.lastName, " +
            "COUNT(t.id) as totalTasks, " +
            "SUM(CASE WHEN t.status = 'TODO' THEN 1 ELSE 0 END) as todoTasks, " +
            "SUM(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as inProgressTasks, " +
            "SUM(CASE WHEN t.status = 'DONE' THEN 1 ELSE 0 END) as doneTasks " +
            "FROM User u LEFT JOIN u.tasks t " +
            "WHERE u.active = true " +
            "GROUP BY u.id, u.username, u.firstName, u.lastName " +
            "ORDER BY totalTasks DESC")
    List<Object[]> getUserTaskStatistics();

    // Find top users by task completion
    @Query("SELECT u FROM User u LEFT JOIN u.tasks t " +
            "WHERE u.active = true AND t.status = 'DONE' " +
            "GROUP BY u.id " +
            "ORDER BY COUNT(t.id) DESC")
    List<User> findTopUsersByCompletedTasks();

    // =====================================================
    // NATIVE SQL QUERIES (for complex database-specific operations)
    // =====================================================

    // Find users registered in the last N days
    @Query(value = "SELECT * FROM users WHERE active = true AND created_at >= CURRENT_DATE - INTERVAL ':days days'",
            nativeQuery = true)
    List<User> findUsersRegisteredInLastDays(@Param("days") int days);

    // Find users with no tasks
    @Query(value = "SELECT u.* FROM users u " +
            "LEFT JOIN tasks t ON u.id = t.user_id " +
            "WHERE u.active = true AND t.id IS NULL",
            nativeQuery = true)
    List<User> findUsersWithNoTasks();
}