package com.taskmanager.app.controller;

import com.taskmanager.app.model.User;
import com.taskmanager.app.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // =====================================================
    // BASIC CRUD OPERATIONS
    // =====================================================

    // GET /api/users - Get all users
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // GET /api/users/active - Get only active users
    @GetMapping("/active")
    public ResponseEntity<List<User>> getActiveUsers() {
        List<User> users = userService.getActiveUsers();
        return ResponseEntity.ok(users);
    }

    // GET /api/users/{id} - Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        // Validate ID
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }

        User user = userService.getUserById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(user);
    }

    // POST /api/users - Create new user
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            // This will be handled by GlobalExceptionHandler
            throw e;
        }
    }

    // PUT /api/users/{id} - Update existing user
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
        // Validate ID
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }

        try {
            User updatedUser = userService.updateUser(id, user);
            if (updatedUser == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            // This will be handled by GlobalExceptionHandler
            throw e;
        }
    }

    // DELETE /api/users/{id} - Hard delete user
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        // Validate ID
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body("Invalid user ID");
        }

        boolean deleted = userService.deleteUser(id);
        if (deleted) {
            return ResponseEntity.ok("User deleted successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // PUT /api/users/{id}/deactivate - Soft delete (deactivate user)
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<String> deactivateUser(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body("Invalid user ID");
        }

        boolean deactivated = userService.deactivateUser(id);
        if (deactivated) {
            return ResponseEntity.ok("User deactivated successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // PUT /api/users/{id}/reactivate - Reactivate user
    @PutMapping("/{id}/reactivate")
    public ResponseEntity<String> reactivateUser(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body("Invalid user ID");
        }

        boolean reactivated = userService.reactivateUser(id);
        if (reactivated) {
            return ResponseEntity.ok("User reactivated successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // =====================================================
    // SEARCH AND FILTER OPERATIONS
    // =====================================================

    // GET /api/users/search?keyword=john - Search users
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<User> users = userService.searchUsers(keyword);
        return ResponseEntity.ok(users);
    }

    // GET /api/users/search/name?name=john - Search by name
    @GetMapping("/search/name")
    public ResponseEntity<List<User>> searchUsersByName(@RequestParam String name) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<User> users = userService.searchUsersByName(name);
        return ResponseEntity.ok(users);
    }

    // GET /api/users/username/{username} - Get user by username
    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Optional<User> userOpt = userService.findUserByUsername(username);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(userOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /api/users/email/{email} - Get user by email
    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Optional<User> userOpt = userService.findUserByEmail(email);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(userOpt.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // =====================================================
    // STATISTICS AND ANALYTICS
    // =====================================================

    // GET /api/users/stats - Get user statistics
    @GetMapping("/stats")
    public ResponseEntity<UserService.UserStats> getUserStatistics() {
        UserService.UserStats stats = userService.getUserStatistics();
        return ResponseEntity.ok(stats);
    }

    // GET /api/users/with-tasks - Get users with their tasks
    @GetMapping("/with-tasks")
    public ResponseEntity<List<User>> getUsersWithTasks() {
        List<User> users = userService.getUsersWithTasks();
        return ResponseEntity.ok(users);
    }

    // GET /api/users/top-performers - Get top users by completed tasks
    @GetMapping("/top-performers")
    public ResponseEntity<List<User>> getTopUsersByCompletedTasks() {
        List<User> users = userService.getTopUsersByCompletedTasks();
        return ResponseEntity.ok(users);
    }

    // GET /api/users/no-tasks - Get users with no tasks
    @GetMapping("/no-tasks")
    public ResponseEntity<List<User>> getUsersWithNoTasks() {
        List<User> users = userService.getUsersWithNoTasks();
        return ResponseEntity.ok(users);
    }

    // =====================================================
    // VALIDATION ENDPOINTS
    // =====================================================

    // GET /api/users/check/username/{username} - Check if username is available
    @GetMapping("/check/username/{username}")
    public ResponseEntity<Boolean> checkUsernameAvailability(@PathVariable String username) {
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        boolean available = userService.isUsernameAvailable(username);
        return ResponseEntity.ok(available);
    }

    // GET /api/users/check/email/{email} - Check if email is available
    @GetMapping("/check/email/{email}")
    public ResponseEntity<Boolean> checkEmailAvailability(@PathVariable String email) {
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        boolean available = userService.isEmailAvailable(email);
        return ResponseEntity.ok(available);
    }
}