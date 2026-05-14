package com.company.ems.Service.Employee;

import com.company.ems.Entity.Task;
import com.company.ems.Entity.User;
import com.company.ems.Repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class EmployeeTaskService {
    @Autowired
    private TaskRepository taskRepository;
    public List<Task> getTasksByUser(Long userId) {
        return taskRepository.findAllByUser_IdAndDeletedFalse(userId);
    }
    public long getTotalTasks(Long userId) {
        return taskRepository.countByUser_IdAndDeletedFalse(userId);
    }
    public long getCompletedTasks(Long userId) {
        return taskRepository.countByUser_IdAndStatusAndDeletedFalse(userId, "Completed");
    }
    public long getPendingTasks(Long userId) {
        return taskRepository.countByUser_IdAndStatusAndDeletedFalse(userId, "Pending");
    }
    public void updateTaskStatus(Long taskId, String status, Long userId) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (task.getUser().getId() != userId) {
            throw new RuntimeException("Unauthorized access");
        }

        task.setStatus(status);
        taskRepository.save(task);
    }
    public Task getTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
    }
    public List<Task> filterTasks(User user, String search, String status, String priority){

        long userId = user.getId();
        List<Task> list = taskRepository.findAllByUser_IdAndDeletedFalse(userId);

        if(search != null){
            search = search.trim().toLowerCase();

            if(!search.isEmpty()){
                String finalSearch = search;

                list = list.stream()
                        .filter(t -> (t.getTaskTitle()!=null && t.getTaskTitle().toLowerCase().contains(finalSearch)))
                        .toList();
            }
        }

        if(status != null && !status.trim().isEmpty()){
            list = list.stream()
                    .filter(t -> t.getStatus()!=null && t.getStatus().equalsIgnoreCase(status))
                    .toList();
        }

        if(priority != null && !priority.trim().isEmpty()){
            list = list.stream()
                    .filter(t -> t.getPriority()!=null && t.getPriority().equalsIgnoreCase(priority))
                    .toList();
        }

        return list;
    }
}
