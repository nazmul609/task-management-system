package com.taskmanager.app.repository;

import com.taskmanager.app.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}