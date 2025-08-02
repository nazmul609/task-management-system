
package com.taskmanager.app.service;

import com.taskmanager.app.model.Task;
import com.taskmanager.app.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service// This tells Spring: "This is a service class, manage it for me!"
public class TaskService{

//    private List<Task> tasks = new ArrayList<>(); // In-memory list
//    private Long idCounter = 1L; // Manual ID generation


    private final TaskRepository taskRepository;// Database access

    // Constructor injection of repository
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository; // Database handles ID generation
    }

    // Get all tasks
    public List<Task> getAllTasks() {
        return taskRepository.findAll(); // JPA built-in method
    }

    // Create new task
    public Task createTask(Task task) {
        // Validation: Check if task with same title already exists
        if (taskRepository.existsByTitle(task.getTitle())) {
            throw new IllegalArgumentException("Task with title '" + task.getTitle() + "' already exists");
        }

        return taskRepository.save(task); // JPA built-in method
    }

    // Get task by ID
    public Task getTaskById(Long id) {
        Optional<Task> task = taskRepository.findById(id); // JPA built-in method
        return task.orElse(null); // Return null if not found
    }

    // Alternative: Get task by ID with better error handling
    public Optional<Task> findTaskById(Long id) {
        return taskRepository.findById(id);
    }

    // Update existing task
    public Task updateTask(Long id, Task updatedTask) {
        Optional<Task> existingTaskOpt = taskRepository.findById(id);

        if (existingTaskOpt.isPresent()) {
            Task existingTask = existingTaskOpt.get();

            // Update fields
            existingTask.setTitle(updatedTask.getTitle());
            existingTask.setDescription(updatedTask.getDescription());
            existingTask.setStatus(updatedTask.getStatus());
            // updatedAt will be set automatically by @PreUpdate

            return taskRepository.save(existingTask); // Save updates
        }

        return null; // Task not found
    }

    // Delete task
    public boolean deleteTask(Long id) {
        if (taskRepository.existsById(id)) { // Check if exists
            taskRepository.deleteById(id); // JPA built-in method
            return true;
        }
        return false; // Task not found
    }

    // Get tasks by status
    public List<Task> getTasksByStatus(String status) {
        return taskRepository.findByStatus(status); // Custom method
    }

    // Search tasks by keyword (in title or description)
    public List<Task> searchTasks(String keyword) {
        return taskRepository.searchTasks(keyword); // Custom @Query method
    }

    // Get tasks by status ordered by creation date
    public List<Task> getTasksByStatusOrderedByDate(String status) {
        return taskRepository.findTasksByStatusOrderByCreatedAt(status); // Custom @Query method
    }

    // Get tasks created between dates
    public List<Task> getTasksCreatedBetween(LocalDateTime start, LocalDateTime end) {
        return taskRepository.findByCreatedAtBetween(start, end); // Method naming convention
    }

    // Get task statistics
    public TaskStats getTaskStatistics() {
        long totalTasks = taskRepository.count();
        long todoTasks = taskRepository.countByStatus("TODO");
        long inProgressTasks = taskRepository.countByStatus("IN_PROGRESS");
        long doneTasks = taskRepository.countByStatus("DONE");

        return new TaskStats(totalTasks, todoTasks, inProgressTasks, doneTasks);
    }

    // Inner class for statistics
    public static class TaskStats {
        private final long total;
        private final long todo;
        private final long inProgress;
        private final long done;

        public TaskStats(long total, long todo, long inProgress, long done) {
            this.total = total;
            this.todo = todo;
            this.inProgress = inProgress;
            this.done = done;
        }

        // Getters
        public long getTotal() { return total; }
        public long getTodo() { return todo; }
        public long getInProgress() { return inProgress; }
        public long getDone() { return done; }
    }
}