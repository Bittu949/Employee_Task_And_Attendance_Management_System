package com.company.ems.Repository;

import com.company.ems.Entity.TaskStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskStatusHistoryRepository
        extends JpaRepository<TaskStatusHistory, Long> {

    List<TaskStatusHistory> findByTask_IdOrderByChangedAtDesc(Long taskId);
}