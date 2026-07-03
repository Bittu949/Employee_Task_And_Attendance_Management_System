package com.company.ems.Service.Admin;

import com.company.ems.Dto.CompletedTaskDto;
import com.company.ems.Dto.TaskSummaryDto;
import com.company.ems.Entity.Task;
import com.company.ems.Entity.User;
import com.company.ems.Exception.InvalidInputException;
import com.company.ems.Exception.ResourceNotFoundException;
import com.company.ems.Repository.TaskRepository;
import com.company.ems.Repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.company.ems.Entity.TaskStatusHistory;
import com.company.ems.Repository.TaskStatusHistoryRepository;
import java.util.Optional;

@AllArgsConstructor
@Service
public class TaskService {

    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private TaskStatusHistoryRepository historyRepository;

    public void addTask(Task task){

        if(task == null){
            throw new InvalidInputException("Task cannot be null");
        }

        task.setCreatedAt(LocalDateTime.now());
        task.setDeleted(false);
        taskRepository.save(task);
    }

    public void assignTask(long taskId, long userId){

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if(task.getUser() != null){
            throw new InvalidInputException("Task is already assigned");
        }

        if(user.getStatus() == null || !user.getStatus().equalsIgnoreCase("ACTIVE")){
            throw new InvalidInputException("Cannot assign task to inactive user");
        }

        task.setUser(user);
        task.setAssignedAt(LocalDate.now());
        taskRepository.save(task);

        TaskStatusHistory history = new TaskStatusHistory();

        history.setTask(task);
        history.setOldStatus(null);
        history.setNewStatus(task.getStatus());
        history.setChangedAt(LocalDateTime.now());

        historyRepository.save(history);
    }

    public Task showTaskPreview(long taskId){

        if(taskId == 0){
            throw new InvalidInputException("Invalid task id");
        }

        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    public List<Task> getUnassignedTasks(){
        List<Task> tasks = taskRepository.findByUserIsNull();
        return tasks.stream().filter(t -> !t.isDeleted()).toList();
    }

    public void editTask(long taskId,
                         String taskTitle,
                         String description,
                         LocalDate deadline,
                         String priority,
                         String status){

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if(taskTitle != null && !taskTitle.isEmpty()){
            task.setTaskTitle(taskTitle);
        }

        if(description != null && !description.isEmpty()){
            task.setDescription(description);
        }

        if(deadline != null){
            task.setDeadline(deadline);
        }

        if(priority != null && !priority.isEmpty()){
            task.setPriority(priority);
        }

        String oldStatus = task.getStatus();

        if(status != null &&
                !status.isEmpty() &&
                !status.equalsIgnoreCase(oldStatus)){

            task.setStatus(status);

            if("COMPLETED".equalsIgnoreCase(status)){
                task.setCompletedAt(LocalDateTime.now());
            }else{
                task.setCompletedAt(null);
            }

            TaskStatusHistory history = new TaskStatusHistory();

            history.setTask(task);
            history.setOldStatus(oldStatus);
            history.setNewStatus(status);
            history.setChangedAt(LocalDateTime.now());

            historyRepository.save(history);
        }

        taskRepository.save(task);
    }

    public Page<Task> showPaginatedTasks(int page, int size){

        if(page < 0) page = 0;

        List<Task> tasks = showAllTasks();

        int start = page * size;
        int end = Math.min(start + size, tasks.size());

        List<Task> paginatedList;

        if(start < tasks.size()){
            paginatedList = tasks.subList(start, end);
        } else {
            paginatedList = new ArrayList<>();
        }

        return new PageImpl<>(paginatedList, PageRequest.of(page, size), tasks.size());
    }

    public Task viewTask(long taskId){

        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    public void deleteTask(long taskId){

        if(!taskRepository.existsById(taskId)){
            throw new ResourceNotFoundException("Task not found");
        }

        Task task = taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        task.setDeleted(true);
        taskRepository.save(task);
    }

    public void reassignTask(long taskId, long userId){

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if(task.getUser() != null && task.getUser().getId() == userId){
            throw new InvalidInputException("Task is already assigned to this user");
        }

        if(user.getStatus() == null || !user.getStatus().equalsIgnoreCase("ACTIVE")){
            throw new InvalidInputException("Cannot assign task to inactive user");
        }

        task.setUser(user);
        task.setAssignedAt(LocalDate.now());
        taskRepository.save(task);
    }

    public List<Task> showAllTasks(){
        return taskRepository.findAllByDeletedFalse();
    }

    public Page<CompletedTaskDto> filterCompletedTasks(
            String search,
            Long userId,
            String completionType,
            int page,
            int size){

        List<Task> tasks = taskRepository.findAllByDeletedFalse()
                .stream()
                .filter(t ->
                        "COMPLETED".equalsIgnoreCase(
                                t.getStatus()))
                .toList();

        if(search != null && !search.trim().isEmpty()){

            String finalSearch =
                    search.trim().toLowerCase();

            tasks = tasks.stream()
                    .filter(t ->
                            t.getTaskTitle() != null &&
                                    t.getTaskTitle()
                                            .toLowerCase()
                                            .contains(finalSearch))
                    .toList();
        }

        if(userId != null && userId != 0){

            tasks = tasks.stream()
                    .filter(t ->
                            t.getUser() != null &&
                                    t.getUser().getId() == userId)
                    .toList();
        }

        if(completionType != null &&
                !completionType.equalsIgnoreCase("ALL")){

            if(completionType.equalsIgnoreCase("ON_TIME")){

                tasks = tasks.stream()
                        .filter(t ->
                                t.getCompletedAt() != null &&
                                        !t.getCompletedAt()
                                                .toLocalDate()
                                                .isAfter(t.getDeadline()))
                        .toList();
            }

            else if(completionType.equalsIgnoreCase("LATE")){

                tasks = tasks.stream()
                        .filter(t ->
                                t.getCompletedAt() != null &&
                                        t.getCompletedAt()
                                                .toLocalDate()
                                                .isAfter(t.getDeadline()))
                        .toList();
            }
        }

        List<CompletedTaskDto> result =
                tasks.stream()
                        .map(task -> {

                            String taskResult =
                                    task.getCompletedAt()
                                            .toLocalDate()
                                            .isAfter(task.getDeadline())
                                            ? "LATE"
                                            : "ON_TIME";

                            return new CompletedTaskDto(
                                    task.getId(),
                                    task.getTaskTitle(),
                                    task.getUser() != null
                                            ? task.getUser().getFullName()
                                            : "Unassigned",
                                    task.getDeadline().toString(),
                                    task.getCompletedAt().toString(),
                                    taskResult
                            );
                        })
                        .toList();

        int start = page * size;
        int end = Math.min(start + size,
                result.size());

        List<CompletedTaskDto> paginated;

        if(start < result.size()){
            paginated = result.subList(start, end);
        }
        else{
            paginated = List.of();
        }

        return new PageImpl<>(
                paginated,
                PageRequest.of(page, size),
                result.size()
        );
    }

    public Page<TaskSummaryDto> filterTaskSummary(
            String search,
            String status,
            Long userId,
            String deadline,
            int page,
            int size){

        Page<Task> tasks =
                filterPaginatedTasks(
                        search,
                        status,
                        userId,
                        deadline,
                        page,
                        size
                );

        return tasks.map(task -> new TaskSummaryDto(
                task.getId(),
                task.getTaskTitle(),
                task.getUser() != null
                        ? task.getUser().getFullName()
                        : "Unassigned",
                task.getDeadline() != null
                        ? task.getDeadline().toString()
                        : "",
                task.getPriority(),
                task.getStatus()
        ));
    }
    public Page<Task> filterPaginatedTasks(String search,
                                           String status,
                                           Long userId,
                                           String deadline,
                                           int page,
                                           int size){

        if(page < 0) page = 0;

        List<Task> tasks = filterTasks(search, status, userId, deadline);

        int start = page * size;
        int end = Math.min(start + size, tasks.size());

        List<Task> paginatedList;

        if(start < tasks.size()){
            paginatedList = tasks.subList(start, end);
        } else {
            paginatedList = new ArrayList<>();
        }

        return new PageImpl<>(paginatedList, PageRequest.of(page, size), tasks.size());
    }

    public List<Task> filterTasks(String search, String status, Long userId, String deadline){

        List<Task> tasks = taskRepository.findAllByDeletedFalse();

        if(search != null){
            search = search.trim().toLowerCase();
            if(!search.isEmpty()){
                String finalSearch = search;
                tasks = tasks.stream()
                        .filter(t -> t.getTaskTitle() != null &&
                                t.getTaskTitle().toLowerCase().contains(finalSearch))
                        .toList();
            }
        }

        if(status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")){
            tasks = tasks.stream()
                    .filter(t -> t.getStatus()!=null &&
                            t.getStatus().equalsIgnoreCase(status))
                    .toList();
        }

        if(userId != null && userId != 0){
            tasks = tasks.stream()
                    .filter(t -> t.getUser() != null &&
                            t.getUser().getId() == userId)
                    .toList();
        }

        if(deadline != null && !deadline.trim().isEmpty() && !deadline.equalsIgnoreCase("ALL")){

            LocalDate today = LocalDate.now();
            LocalDate thisWeek = today.plusDays(7);

            if(deadline.equalsIgnoreCase("TODAY")){
                tasks = tasks.stream()
                        .filter(t -> t.getDeadline()!=null &&
                                t.getDeadline().isEqual(today))
                        .toList();
            }
            else if(deadline.equalsIgnoreCase("THISWEEK")){
                tasks = tasks.stream()
                        .filter(t -> t.getDeadline()!=null &&
                                !t.getDeadline().isBefore(today) &&
                                !t.getDeadline().isAfter(thisWeek))
                        .toList();
            }
            else if(deadline.equalsIgnoreCase("OVERDUE")){

                tasks = tasks.stream()
                        .filter(t -> t.getDeadline() != null)
                        .filter(t -> t.getDeadline().isBefore(today))
                        .filter(t ->
                                "PENDING".equalsIgnoreCase(t.getStatus())
                                        || "IN_PROGRESS".equalsIgnoreCase(t.getStatus()))
                        .toList();
            }
            else{
                throw new InvalidInputException("Invalid deadline filter");
            }
        }

        return tasks;
    }

    public long totalTaskCount(){
        return taskRepository.countByDeletedFalse();
    }

    public long pendingTaskCount(){
        List<Task> tasks = taskRepository.findAllByStatus("PENDING");
        return tasks.stream().filter(t -> !t.isDeleted()).count();
    }

    public long inProgressTaskCount(){
        List<Task> tasks = taskRepository.findAllByStatus("IN_PROGRESS");
        return tasks.stream().filter(t -> !t.isDeleted()).count();
    }

    public long completedTaskCount(){
        List<Task> tasks = taskRepository.findAllByStatus("COMPLETED");
        return tasks.stream().filter(t -> !t.isDeleted()).count();
    }
    public long taskDueThisWeekCount(){

        LocalDate today = LocalDate.now();
        LocalDate thisWeek = today.plusDays(7);

        return taskRepository.findAllByDeletedFalse()
                .stream()
                .filter(t -> t.getDeadline() != null)
                .filter(t -> !t.getDeadline().isBefore(today))
                .filter(t -> !t.getDeadline().isAfter(thisWeek))
                .filter(t -> !"COMPLETED".equalsIgnoreCase(t.getStatus()))
                .count();
    }
    public Task getTaskById(long id){

        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }
    public List<Task> showDeletedTasks(){
        return taskRepository.findAllByDeletedTrue();
    }
    public void restoreTask(Long taskId){

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        task.setDeleted(false);

        taskRepository.save(task);
    }
    public void permanentDelete(Long taskId){

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        taskRepository.delete(task);
    }
    public List<TaskStatusHistory> getTaskHistory(Long taskId){

        return historyRepository
                .findByTask_IdOrderByChangedAtDesc(taskId);
    }
}