package com.taskmanager.app.service;

import com.taskmanager.app.model.Task;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service// This tells Spring: "This is a service class, manage it for me!"
public class TaskService{

    private List<Task> tasks = new ArrayList<>();
    private long idCounter = 1L;

    //return all task
    public List<Task> getAllTasks(){
        return tasks;
    }

    // Create a new task
    public Task createTask(Task task) {
        task.setId(idCounter++);
        tasks.add(task);
        return task;
    }

    // Get task by ID
    public Task getTaskById(Long id) {
        return tasks.stream()
                .filter(task -> task.getId().equals(id))
                .findFirst()
                .orElse(null); // Returns null if not found
    }

    // Update an existing task
    public Task updateTask(Long id, Task updatedTask) {
        for(int i = 0; i < tasks.size(); i++) {
            if(tasks.get(i).getId().equals(id)) {
                updatedTask.setId(id); // Keep the same ID
                tasks.set(i, updatedTask); // Replace the task at index i
                return updatedTask;
            }
        }
        return null; // Task not found
    }

    // Delete a task
    public boolean deleteTask(Long id) {
        return tasks.removeIf(task -> task.getId().equals(id));
        // removeIf returns true if a task was removed, false otherwise
    }

    // Get tasks by status
    public List<Task> getTasksByStatus(String status) {
        return tasks.stream()
                .filter(task -> task.getStatus().equalsIgnoreCase(status))
                .toList();
    }


}