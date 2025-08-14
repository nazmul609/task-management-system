package com.taskmanager.app.service;

import com.taskmanager.app.model.User;
import com.taskmanager.app.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional // All methods are transactional by default
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor injection
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =====================================================
    // BASIC CRUD OPERATIONS
    // =====================================================

    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Get only active users
    public List<User> getActiveUsers() {
        return userRepository.findByActive(true);
    }

    // Get user by ID
    public User getUserById(Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.orElse(null);
    }

    // Get user by ID with Optional (better error handling)
    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    // Get user by username (for login)
    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Get user by username (for JWT authentication) - Throws exception if not found
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }

    // Get user by email
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Create new user
    public User createUser(User user) {
        // Validation: Check if username already exists
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username '" + user.getUsername() + "' is already taken");
        }

        // Validation: Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email '" + user.getEmail() + "' is already registered");
        }

        // Set default values
        if (user.getActive() == null) {
            user.setActive(true);
        }


// Encode password before saving
        if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    // Update existing user
    public User updateUser(Long id, User updatedUser) {
        Optional<User> existingUserOpt = userRepository.findById(id);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            // Check if new username is already taken by another user
            if (!existingUser.getUsername().equals(updatedUser.getUsername()) &&
                    userRepository.existsByUsername(updatedUser.getUsername())) {
                throw new IllegalArgumentException("Username '" + updatedUser.getUsername() + "' is already taken");
            }

            // Check if new email is already taken by another user
            if (!existingUser.getEmail().equals(updatedUser.getEmail()) &&
                    userRepository.existsByEmail(updatedUser.getEmail())) {
                throw new IllegalArgumentException("Email '" + updatedUser.getEmail() + "' is already registered");
            }

            // Update fields
            existingUser.setUsername(updatedUser.getUsername());
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setFirstName(updatedUser.getFirstName());
            existingUser.setLastName(updatedUser.getLastName());

            // Only update password if provided
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().trim().isEmpty()) {
                existingUser.setPassword(updatedUser.getPassword());
            }

            // Update active status if provided
            if (updatedUser.getActive() != null) {
                existingUser.setActive(updatedUser.getActive());
            }

            // updatedAt will be set automatically by @PreUpdate
            return userRepository.save(existingUser);
        }

        return null; // User not found
    }

    // Soft delete user (set active = false)
    public boolean deactivateUser(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setActive(false);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    // Hard delete user (permanent deletion)
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Reactivate user
    public boolean reactivateUser(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setActive(true);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    // =====================================================
    // SEARCH AND FILTER OPERATIONS
    // =====================================================

    // Search users by keyword
    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getActiveUsers();
        }
        return userRepository.searchUsers(keyword.trim());
    }

    // Search users by name
    public List<User> searchUsersByName(String name) {
        return userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(name, name);
    }

    // Search users by full name
    public List<User> searchUsersByFullName(String fullName) {
        return userRepository.findByFullNameContaining(fullName);
    }

    // Get users created between dates
    public List<User> getUsersCreatedBetween(LocalDateTime start, LocalDateTime end) {
        return userRepository.findByCreatedAtBetween(start, end);
    }

    // =====================================================
    // STATISTICS AND ANALYTICS
    // =====================================================

    // Get user statistics
    public UserStats getUserStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countActiveUsers();
        long inactiveUsers = totalUsers - activeUsers;

        return new UserStats(totalUsers, activeUsers, inactiveUsers);
    }

    // Get users with their tasks
    public List<User> getUsersWithTasks() {
        return userRepository.findActiveUsersWithTasks();
    }

    // Get top users by completed tasks
    public List<User> getTopUsersByCompletedTasks() {
        return userRepository.findTopUsersByCompletedTasks();
    }

    // Get users with no tasks
    public List<User> getUsersWithNoTasks() {
        return userRepository.findUsersWithNoTasks();
    }

    // =====================================================
    // VALIDATION HELPERS
    // =====================================================

    // Check if username is available
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    // Check if email is available
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    // Validate user exists and is active
    public boolean isUserActiveById(Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        return userOpt.isPresent() && userOpt.get().getActive();
    }

    // =====================================================
    // INNER CLASS FOR STATISTICS
    // =====================================================

    public static class UserStats {
        private final long total;
        private final long active;
        private final long inactive;

        public UserStats(long total, long active, long inactive) {
            this.total = total;
            this.active = active;
            this.inactive = inactive;
        }

        // Getters
        public long getTotal() { return total; }
        public long getActive() { return active; }
        public long getInactive() { return inactive; }

        // Calculate percentage
        public double getActivePercentage() {
            return total > 0 ? (double) active / total * 100 : 0;
        }
    }
}

