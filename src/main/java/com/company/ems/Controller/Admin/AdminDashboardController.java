package com.company.ems.Controller.Admin;

import com.company.ems.Entity.Attendance;
import com.company.ems.Entity.Task;
import com.company.ems.Exception.BadRequestException;
import com.company.ems.Service.Admin.AttendanceService;
import com.company.ems.Service.Admin.TaskService;
import com.company.ems.Repository.TaskRepository;
import com.company.ems.Repository.UserRepository;
import com.company.ems.Repository.AttendanceRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@AllArgsConstructor
@RequestMapping("/admin")
public class AdminDashboardController {

    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceService attendanceService;

    @GetMapping("/admin_dashboard")
    public String showDashboardDetails(Model model,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size){

        if(page < 0 || size <= 0){
            throw new BadRequestException("Invalid pagination parameters");
        }
        List<Attendance> attendanceList = attendanceService.showAttendanceRecords();
        Page<Task> tasks = taskService.showPaginatedTasks(page, size);

        model.addAttribute("totalTasks", taskRepository.countByDeletedFalse());
        model.addAttribute("completedTasks", taskRepository.countByStatusAndDeletedFalse("COMPLETED"));
        model.addAttribute("pendingTasks", taskRepository.countByStatusAndDeletedFalse("PENDING"));
        model.addAttribute("todaysAttendancePercentage", attendanceService.showAttendancePercentage(attendanceList));
        model.addAttribute("totalEmployee", attendanceService.totalEmployeesCount());
        model.addAttribute("tasksDueThisWeek", taskService.taskDueThisWeekCount());

        model.addAttribute("allTasks", tasks.getContent());
        model.addAttribute("currentPage", tasks.getNumber());
        model.addAttribute("totalPages", tasks.getTotalPages());

        model.addAttribute("presentCount", attendanceService.presentCount(attendanceList));
        model.addAttribute("absentCount", attendanceService.absentCount(attendanceList));
        model.addAttribute("attendancePercentageToday", attendanceService.showAttendancePercentage(attendanceList));

        return "/Admin_dashboard/admin_dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/";
    }
}