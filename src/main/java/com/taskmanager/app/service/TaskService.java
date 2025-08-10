package com.taskmanager.app.service;

import com.taskmanager.app.model.Task;
import com.taskmanager.app.model.User;
import com.taskmanager.app.repository.TaskRepository;
import com.taskmanager.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository; // NEW: Add user repository

    // Updated constructor with UserRepository injection
    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    // =====================================================
    // ORIGINAL METHODS (from Day 4) - ENHANCED
    // =====================================================

    // Get all tasks
    public List<Task> getAllTasks() {
        return taskRepository.findAllTasksWithUsers(); // Enhanced: Load users to avoid N+1 problem
    }

    // Create new task (enhanced with user assignment)
    public Task createTask(Task task) {
        // Validation: Check if task with same title already exists
        if (taskRepository.existsByTitle(task.getTitle())) {
            throw new IllegalArgumentException("Task with title '" + task.getTitle() + "' already exists");
        }

        return taskRepository.save(task);
    }

    // Create new task and assign to user
    public Task createTaskForUser(Task task, Long userId) {
        // Validation: Check if task with same title already exists
        if (taskRepository.existsByTitle(task.getTitle())) {
            throw new IllegalArgumentException("Task with title '" + task.getTitle() + "' already exists");
        }

        // Find and assign user
        if (userId != null) {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent() && userOpt.get().getActive()) {
                task.setUser(userOpt.get());
            } else {
                throw new IllegalArgumentException("User with ID " + userId + " not found or inactive");
            }
        }

        return taskRepository.save(task);
    }

    // Get task by ID (enhanced with user data)
    public Task getTaskById(Long id) {
        Optional<Task> task = taskRepository.findTaskWithUser(id); // Enhanced: Load user data
        return task.orElse(null);
    }

    // Alternative: Get task by ID with better error handling
    public Optional<Task> findTaskById(Long id) {
        return taskRepository.findTaskWithUser(id);
    }

    // Update existing task (enhanced with user assignment)
    public Task updateTask(Long id, Task updatedTask) {
        Optional<Task> existingTaskOpt = taskRepository.findById(id);

        if (existingTaskOpt.isPresent()) {
            Task existingTask = existingTaskOpt.get();

            // Update basic fields
            existingTask.setTitle(updatedTask.getTitle());
            existingTask.setDescription(updatedTask.getDescription());
            existingTask.setStatus(updatedTask.getStatus());

            // Handle user assignment update
            if (updatedTask.getUser() != null) {
                // Validate user exists and is active
                Optional<User> userOpt = userRepository.findById(updatedTask.getUser().getId());
                if (userOpt.isPresent() && userOpt.get().getActive()) {
                    existingTask.setUser(userOpt.get());
                } else {
                    throw new IllegalArgumentException("User with ID " + updatedTask.getUser().getId() + " not found or inactive");
                }
            }

            return taskRepository.save(existingTask);
        }

        return null; // Task not found
    }

    // Assign task to user
    public Task assignTaskToUser(Long taskId, Long userId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task with ID " + taskId + " not found");
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty() || !userOpt.get().getActive()) {
            throw new IllegalArgumentException("User with ID " + userId + " not found or inactive");
        }

        Task task = taskOpt.get();
        User user = userOpt.get();

        task.setUser(user);
        return taskRepository.save(task);
    }

    // Unassign task from user
    public Task unassignTask(Long taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new IllegalArgumentException("Task with ID " + taskId + " not found");
        }

        Task task = taskOpt.get();
        task.setUser(null);
        return taskRepository.save(task);
    }

    // Delete task (unchanged)
    public boolean deleteTask(Long id) {
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // =====================================================
    // ORIGINAL METHODS (unchanged from Day 4)
    // =====================================================

    // Get tasks by status
    public List<Task> getTasksByStatus(String status) {
        return taskRepository.findByStatus(status);
    }

    // Search tasks by keyword (in title or description)
    public List<Task> searchTasks(String keyword) {
        return taskRepository.searchTasks(keyword);
    }

    // Get tasks by status ordered by creation date
    public List<Task> getTasksByStatusOrderedByDate(String status) {
        return taskRepository.findTasksByStatusOrderByCreatedAt(status);
    }

    // Get tasks created between dates
    public List<Task> getTasksCreatedBetween(LocalDateTime start, LocalDateTime end) {
        return taskRepository.findByCreatedAtBetween(start, end);
    }

    // Get task statistics (enhanced with assignment info)
    public TaskStats getTaskStatistics() {
        long totalTasks = taskRepository.count();
        long todoTasks = taskRepository.countByStatus("TODO");
        long inProgressTasks = taskRepository.countByStatus("IN_PROGRESS");
        long doneTasks = taskRepository.countByStatus("DONE");

        // NEW: Add assignment statistics
        long assignedTasks = taskRepository.countAssignedTasks();
        long unassignedTasks = taskRepository.countUnassignedTasks();

        return new TaskStats(totalTasks, todoTasks, inProgressTasks, doneTasks, assignedTasks, unassignedTasks);
    }

    // =====================================================
    // NEW: USER-SPECIFIC METHODS
    // =====================================================

    // Get tasks by user ID
    public List<Task> getTasksByUserId(Long userId) {
        return taskRepository.findByUserId(userId);
    }

    // Get tasks by user ID and status
    public List<Task> getTasksByUserIdAndStatus(Long userId, String status) {
        return taskRepository.findByUserIdAndStatus(userId, status);
    }

    // Get tasks by username
    public List<Task> getTasksByUsername(String username) {
        return taskRepository.findByUsername(username);
    }

    // Get unassigned tasks
    public List<Task> getUnassignedTasks() {
        return taskRepository.findUnassignedTasks();
    }

    // Get assigned tasks
    public List<Task> getAssignedTasks() {
        return taskRepository.findAssignedTasks();
    }

    // Search tasks for specific user
    public List<Task> searchTasksByUser(Long userId, String keyword) {
        return taskRepository.searchTasksByUser(userId, keyword);
    }

    // Get user's task statistics
    public UserTaskStats getUserTaskStats(Long userId) {
        long totalTasks = taskRepository.countByUserId(userId);
        long todoTasks = taskRepository.countByUserIdAndStatus(userId, "TODO");
        long inProgressTasks = taskRepository.countByUserIdAndStatus(userId, "IN_PROGRESS");
        long doneTasks = taskRepository.countByUserIdAndStatus(userId, "DONE");

        return new UserTaskStats(userId, totalTasks, todoTasks, inProgressTasks, doneTasks);
    }

    // Check if user has any tasks
    public boolean userHasTasks(Long userId) {
        return taskRepository.existsByUserId(userId);
    }

    // Get recent tasks for user
    public List<Task> getRecentTasksByUser(Long userId) {
        return taskRepository.findRecentTasksByUser(userId);
    }

    // =====================================================
    // UPDATED INNER CLASSES
    // =====================================================

    // Enhanced TaskStats with assignment information
    public static class TaskStats {
        private final long total;
        private final long todo;
        private final long inProgress;
        private final long done;
        private final long assigned;     // NEW
        private final long unassigned;   // NEW

        public TaskStats(long total, long todo, long inProgress, long done, long assigned, long unassigned) {
            this.total = total;
            this.todo = todo;
            this.inProgress = inProgress;
            this.done = done;
            this.assigned = assigned;
            this.unassigned = unassigned;
        }

        // Original getters
        public long getTotal() { return total; }
        public long getTodo() { return todo; }
        public long getInProgress() { return inProgress; }
        public long getDone() { return done; }

        // NEW getters
        public long getAssigned() { return assigned; }
        public long getUnassigned() { return unassigned; }

        // Calculate percentages
        public double getCompletionPercentage() {
            return total > 0 ? (double) done / total * 100 : 0;
        }

        public double getAssignmentPercentage() {
            return total > 0 ? (double) assigned / total * 100 : 0;
        }
    }

    // NEW: User-specific task statistics
    public static class UserTaskStats {
        private final Long userId;
        private final long total;
        private final long todo;
        private final long inProgress;
        private final long done;

        public UserTaskStats(Long userId, long total, long todo, long inProgress, long done) {
            this.userId = userId;
            this.total = total;
            this.todo = todo;
            this.inProgress = inProgress;
            this.done = done;
        }

        // Getters
        public Long getUserId() { return userId; }
        public long getTotal() { return total; }
        public long getTodo() { return todo; }
        public long getInProgress() { return inProgress; }
        public long getDone() { return done; }

        // Calculate completion percentage
        public double getCompletionPercentage() {
            return total > 0 ? (double) done / total * 100 : 0;
        }
    }
}