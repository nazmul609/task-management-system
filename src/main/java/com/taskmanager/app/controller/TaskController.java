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

    // GET /api/tasks - Get all tasks
    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        List<Task> tasks = taskService.getAllTasks();

        // Even if list is empty, return 200 OK with empty array
        return ResponseEntity.ok(tasks);
    }

    // POST /api/tasks - Create a new task
    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody Task task) {
        // Validate input before processing
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().build(); // 400 BAD REQUEST
        }

        Task createdTask = taskService.createTask(task);

        // Return 201 CREATED with the created task
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    // GET /api/tasks/{id} - Get task by ID
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        // Validate ID
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build(); // 400 BAD REQUEST
        }

        Task task = taskService.getTaskById(id);

        if (task == null) {
            return ResponseEntity.notFound().build(); // 404 NOT FOUND
        }

        return ResponseEntity.ok(task); // 200 OK
    }

    // PUT /api/tasks/{id} - Update existing task
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @Valid @RequestBody Task task) {
        // Validate ID
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().build();
        }

        // Check if task exists
        Task existingTask = taskService.getTaskById(id);
        if (existingTask == null) {
            return ResponseEntity.notFound().build(); // 404 NOT FOUND
        }

        Task updatedTask = taskService.updateTask(id, task);

        if (updatedTask == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(updatedTask); // 200 OK
    }

    // DELETE /api/tasks/{id} - Delete a task
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTask(@PathVariable Long id) {
        // Validate ID
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body("Invalid task ID");
        }

        boolean deleted = taskService.deleteTask(id);

        if (deleted) {
            return ResponseEntity.ok("Task deleted successfully"); // 200 OK
        } else {
            return ResponseEntity.notFound().build(); // 404 NOT FOUND
        }
    }

    // GET /api/tasks/status/{status} - Get tasks by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Task>> getTasksByStatus(@PathVariable String status) {
        // Validate status
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

    // GET /api/tasks/stats - Get task statistics
    @GetMapping("/stats")
    public ResponseEntity<TaskService.TaskStats> getTaskStatistics() {
        TaskService.TaskStats stats = taskService.getTaskStatistics();
        return ResponseEntity.ok(stats);
    }
}