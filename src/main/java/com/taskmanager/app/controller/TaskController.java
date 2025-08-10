package com.taskmanager.app.controller;

import com.taskmanager.app.model.Task;
import com.taskmanager.app.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // =====================================================
    // ORIGINAL ENDPOINTS (from Day 4) - ENHANCED
    // =====================================================

    // GET /api/tasks - Get all tasks (now includes user data)
    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        List<Task> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    // POST /api/tasks - Create a new task
    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody Task task) {
        // Validate input before processing
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Task createdTask = taskService.createTask(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    // GET /api/tasks/{id} - Get task by ID (now includes user data)
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }

        Task task = taskService.getTaskById(id);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(task);
    }

    // PUT /api/tasks/{id} - Update existing task (now supports user assignment)
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @Valid @RequestBody Task task) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }

        Task existingTask = taskService.getTaskById(id);
        if (existingTask == null) {
            return ResponseEntity.notFound().build();
        }

        Task updatedTask = taskService.updateTask(id, task);
        if (updatedTask == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updatedTask);
    }

    // DELETE /api/tasks/{id} - Delete a task
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTask(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body("Invalid task ID");
        }

        boolean deleted = taskService.deleteTask(id);
        if (deleted) {
            return ResponseEntity.ok("Task deleted successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /api/tasks/status/{status} - Get tasks by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Task>> getTasksByStatus(@PathVariable String status) {
        if (status == null || status.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Task> tasks = taskService.getTasksByStatus(status);
        return ResponseEntity.ok(tasks);
    }

    // GET /api/tasks/search?keyword=spring - Search tasks
    @GetMapping("/search")
    public ResponseEntity<List<Task>> searchTasks(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Task> tasks = taskService.searchTasks(keyword);
        return ResponseEntity.ok(tasks);
    }

    // GET /api/tasks/stats - Get task statistics (enhanced with assignment info)
    @GetMapping("/stats")
    public ResponseEntity<TaskService.TaskStats> getTaskStatistics() {
        TaskService.TaskStats stats = taskService.getTaskStatistics();
        return ResponseEntity.ok(stats);
    }

    // =====================================================
    // NEW: USER-SPECIFIC ENDPOINTS
    // =====================================================

    // POST /api/tasks/assign/{userId} - Create task and assign to user
    @PostMapping("/assign/{userId}")
    public ResponseEntity<Task> createTaskForUser(@Valid @RequestBody Task task, @PathVariable Long userId) {
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Task createdTask = taskService.createTaskForUser(task, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
        } catch (IllegalArgumentException e) {
            throw e; // Will be handled by GlobalExceptionHandler
        }
    }

    // PUT /api/tasks/{taskId}/assign/{userId} - Assign existing task to user
    @PutMapping("/{taskId}/assign/{userId}")
    public ResponseEntity<Task> assignTaskToUser(@PathVariable Long taskId, @PathVariable Long userId) {
        if (taskId == null || taskId <= 0 || userId == null || userId <= 0) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Task assignedTask = taskService.assignTaskToUser(taskId, userId);
            return ResponseEntity.ok(assignedTask);
        } catch (IllegalArgumentException e) {
            throw e; // Will be handled by GlobalExceptionHandler
        }
    }

    // PUT /api/tasks/{taskId}/unassign - Unassign task from user
    @PutMapping("/{taskId}/unassign")
    public ResponseEntity<Task> unassignTask(@PathVariable Long taskId) {
        if (taskId == null || taskId <= 0) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Task unassignedTask = taskService.unassignTask(taskId);
            return ResponseEntity.ok(unassignedTask);
        } catch (IllegalArgumentException e) {
            throw e; // Will be handled by GlobalExceptionHandler
        }
    }

    // GET /api/tasks/user/{userId} - Get all tasks for specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Task>> getTasksByUserId(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().build();
        }

        List<Task> tasks = taskService.getTasksByUserId(userId);
        return ResponseEntity.ok(tasks);
    }

    // GET /api/tasks/user/{userId}/status/{status} - Get user's tasks by status
    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<Task>> getTasksByUserIdAndStatus(
            @PathVariable Long userId, @PathVariable String status) {

        if (userId == null || userId <= 0 || status == null || status.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Task> tasks = taskService.getTasksByUserIdAndStatus(userId, status);
        return ResponseEntity.ok(tasks);
    }

    // GET /api/tasks/username/{username} - Get tasks by username
    @GetMapping("/username/{username}")
    public ResponseEntity<List<Task>> getTasksByUsername(@PathVariable String username) {
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Task> tasks = taskService.getTasksByUsername(username);
        return ResponseEntity.ok(tasks);
    }

    // GET /api/tasks/unassigned - Get unassigned tasks
    @GetMapping("/unassigned")
    public ResponseEntity<List<Task>> getUnassignedTasks() {
        List<Task> tasks = taskService.getUnassignedTasks();
        return ResponseEntity.ok(tasks);
    }

    // GET /api/tasks/assigned - Get assigned tasks
    @GetMapping("/assigned")
    public ResponseEntity<List<Task>> getAssignedTasks() {
        List<Task> tasks = taskService.getAssignedTasks();
        return ResponseEntity.ok(tasks);
    }

    // GET /api/tasks/user/{userId}/search?keyword=spring - Search tasks for specific user
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<List<Task>> searchTasksByUser(
            @PathVariable Long userId, @RequestParam String keyword) {

        if (userId == null || userId <= 0 || keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Task> tasks = taskService.searchTasksByUser(userId, keyword);
        return ResponseEntity.ok(tasks);
    }

    // GET /api/tasks/user/{userId}/recent - Get recent tasks for user
    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<List<Task>> getRecentTasksByUser(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().build();
        }

        List<Task> tasks = taskService.getRecentTasksByUser(userId);
        return ResponseEntity.ok(tasks);
    }

    // GET /api/tasks/user/{userId}/stats - Get task statistics for specific user
    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<TaskService.UserTaskStats> getUserTaskStats(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().build();
        }

        TaskService.UserTaskStats stats = taskService.getUserTaskStats(userId);
        return ResponseEntity.ok(stats);
    }

    // GET /api/tasks/user/{userId}/has-tasks - Check if user has any tasks
    @GetMapping("/user/{userId}/has-tasks")
    public ResponseEntity<Boolean> userHasTasks(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().build();
        }

        boolean hasTasks = taskService.userHasTasks(userId);
        return ResponseEntity.ok(hasTasks);
    }
}