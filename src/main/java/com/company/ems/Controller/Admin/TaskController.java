package com.company.ems.Controller.Admin;

import com.company.ems.Entity.Task;
import com.company.ems.Exception.InvalidDataException;
import com.company.ems.Exception.ResourceNotFoundException;
import com.company.ems.Service.Admin.TaskService;
import com.company.ems.Service.Admin.UserService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@AllArgsConstructor
@RequestMapping("/admin/task")
public class TaskController {

    private TaskService taskService;
    private UserService userService;

    @GetMapping("/createPage")
    public String createPage(Model model) {
        model.addAttribute("today", LocalDate.now());
        return "Admin_dashboard/Manage_tasks/Create_task/create_task";
    }

    @PostMapping("/create")
    public String addTask(@RequestParam String taskTitle,
                          @RequestParam String description,
                          @RequestParam String deadline,
                          @RequestParam String priority,
                          @RequestParam String status) {
        if(deadline != null){
            LocalDate parsedDate = LocalDate.parse(deadline);

            if (parsedDate.isBefore(LocalDate.now())) {
                throw new InvalidDataException("Deadline must be a future date");
            }
        }

        if (taskTitle == null || taskTitle.trim().isEmpty()) {
            throw new InvalidDataException("Task title cannot be empty");
        }

        if (deadline == null || deadline.trim().isEmpty()) {
            throw new InvalidDataException("Deadline is required");
        }

        taskService.addTask(
                new Task(taskTitle, description, LocalDate.parse(deadline), priority, status)
        );

        return "redirect:/admin/task/allTasks";
    }

    @GetMapping("/assignPage")
    public String assignPage(Model model) {
        model.addAttribute("tasks", taskService.getUnassignedTasks());
        model.addAttribute("users", userService.getActiveUsers());
        return "Admin_dashboard/Manage_tasks/Assign_task/assign_task";
    }

    @PostMapping("/assign")
    public String assignTo(@RequestParam long taskId,
                           @RequestParam long userId) {

        taskService.assignTask(taskId, userId);

        return "redirect:/admin/task/allTasks";
    }

    @GetMapping("/{taskId}/preview")
    @ResponseBody
    public Task assignTaskPreview(@PathVariable long taskId) {

        Task task = taskService.showTaskPreview(taskId);

        if (task == null) {
            throw new ResourceNotFoundException("Task not found");
        }

        return task;
    }

    @GetMapping("/{taskId}/editPage")
    public String editPage(@PathVariable long taskId,
                           Model model) {

        Task task = taskService.getTaskById(taskId);

        if (task == null) {
            throw new ResourceNotFoundException("Task not found");
        }

        model.addAttribute("task", task);
        model.addAttribute("today", LocalDate.now());

        return "Admin_dashboard/Manage_tasks/Edit_task/edit_task";
    }

    @PostMapping("/edit")
    public String editTask(@RequestParam long taskId,
                           @RequestParam(required = false) String taskTitle,
                           @RequestParam(required = false) String description,
                           @RequestParam(required = false) String deadline,
                           @RequestParam(required = false) String priority,
                           @RequestParam(required = false) String status) {
        if(deadline != null){
            LocalDate parsedDate = LocalDate.parse(deadline);

            if (parsedDate.isBefore(LocalDate.now())) {
                throw new InvalidDataException("Deadline must be a future date");
            }
        }

        if (deadline != null && !deadline.isEmpty()) {
            taskService.editTask(taskId, taskTitle, description,
                    LocalDate.parse(deadline), priority, status);
        } else {
            taskService.editTask(taskId, taskTitle, description,
                    null, priority, status);
        }

        return "redirect:/admin/task/allTasks";
    }

    @GetMapping("/{taskId}/view")
    public String viewTask(@PathVariable long taskId,
                           Model model) {

        Task task = taskService.viewTask(taskId);

        if (task == null) {
            throw new ResourceNotFoundException("Task not found");
        }

        model.addAttribute("viewTask", task);

        return "Admin_dashboard/Manage_tasks/View/view";
    }

    @PostMapping("/{taskId}/delete")
    public String deleteTask(@PathVariable long taskId) {

        taskService.deleteTask(taskId);

        return "redirect:/admin/task/allTasks";
    }

    @GetMapping("/{taskId}/reassignPage")
    public String showReassignPage(@PathVariable long taskId,
                                   Model model) {

        Task task = taskService.getTaskById(taskId);

        if (task == null) {
            throw new ResourceNotFoundException("Task not found");
        }

        model.addAttribute("task", task);

        if (task.getUser() != null) {
            model.addAttribute("users",
                    userService.getActiveUsersExcept(task.getUser().getId()));
        } else {
            model.addAttribute("users", userService.getActiveUsers());
        }

        return "Admin_dashboard/Manage_tasks/Reassign_task/reassign_task";
    }

    @PostMapping("/reassign")
    public String reassignTask(@RequestParam long taskId,
                               @RequestParam long userId) {

        taskService.reassignTask(taskId, userId);

        return "redirect:/admin/task/allTasks";
    }

    @GetMapping("/allTasks")
    public String displayAllTasks(Model model,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {

        Page<Task> tasks = taskService.showPaginatedTasks(page, size);

        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("tasks", tasks.getContent());
        model.addAttribute("currentPage", tasks.getNumber());
        model.addAttribute("totalPages", tasks.getTotalPages());
        model.addAttribute("totalTasks", taskService.totalTaskCount());
        model.addAttribute("pendingTasks", taskService.pendingTaskCount());
        model.addAttribute("inProgressTask", taskService.inProgressTaskCount());
        model.addAttribute("completedTask", taskService.completedTaskCount());

        return "Admin_dashboard/Manage_tasks/task";
    }

    @GetMapping("/filter")
    @ResponseBody
    public Page<Task> filterTasks(@RequestParam(required = false) String search,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) Long userId,
                                  @RequestParam(required = false) String deadline,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {

        return taskService.filterPaginatedTasks(search, status, userId, deadline, page, size);
    }
    @GetMapping("/restorePage")
    public String restorePage(Model model){

        List<Task> tasks = taskService.showDeletedTasks();

        model.addAttribute("tasks", tasks);

        return "Admin_dashboard/Manage_tasks/Restore_tasks/restore_tasks";
    }
    @PostMapping("/{taskId}/restore")
    @ResponseBody
    public ResponseEntity<?> restoreTask(@PathVariable Long taskId){

        taskService.restoreTask(taskId);

        return ResponseEntity.ok().build();
    }
    @PostMapping("/{taskId}/permanentDelete")
    @ResponseBody
    public ResponseEntity<?> permanentDelete(@PathVariable Long taskId){

        taskService.permanentDelete(taskId);

        return ResponseEntity.ok().build();
    }
}