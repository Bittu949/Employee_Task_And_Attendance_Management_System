package com.company.ems.Controller.Employee;

import com.company.ems.Entity.Attendance;
import com.company.ems.Entity.Task;
import com.company.ems.Entity.User;
import com.company.ems.Repository.AttendanceRepository;
import com.company.ems.Repository.TaskRepository;
import com.company.ems.Repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.company.ems.Entity.EmploymentHistory;
import com.company.ems.Repository.EmploymentHistoryRepository;

import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@Controller
@RequestMapping("/employee")
public class EmployeeController {
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmploymentHistoryRepository employmentHistoryRepository;
    @GetMapping("/employee_dashboard")
    public String dashboard(Model model, Principal principal) {

        String email = principal.getName();
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return "redirect:/index";
        }
        long userId = user.getId();

        long totalTasks = taskRepository.countByUser_IdAndDeletedFalse(userId);
        long completedTasks = taskRepository.countByUser_IdAndStatusAndDeletedFalse(userId, "Completed");
        long pendingTasks = taskRepository.countByUser_IdAndStatusAndDeletedFalse(userId, "Pending");

        List<Task> tasks = taskRepository.findAllByUser_IdAndDeletedFalse(userId);

        List<Task> recentTasks = tasks.stream()
                .filter(t -> "Pending".equalsIgnoreCase(t.getStatus()))
                .sorted(Comparator.comparing(Task::getDeadline))
                .limit(5)
                .toList();

        Set<LocalDate> employmentDates = new HashSet<>();

        List<EmploymentHistory> employmentPeriods =
                employmentHistoryRepository.findAllByUser_Id(userId);

        for (EmploymentHistory period : employmentPeriods) {

            LocalDate startDate = period.getStartDate();

            LocalDate endDate =
                    period.getEndDate() == null
                            ? LocalDate.now()
                            : period.getEndDate();

            for (LocalDate date = startDate;
                 !date.isAfter(endDate);
                 date = date.plusDays(1)) {

                employmentDates.add(date);
            }
        }

        long totalDays = employmentDates.size();

        long presentDays =
                attendanceRepository.countByUserIdAndStatus(userId, "PRESENT");

        double attendancePercentage = 0;

        if (totalDays > 0) {
            attendancePercentage =
                    Math.round((presentDays * 100.0) / totalDays);
        }

        LocalDate today = LocalDate.now();

        Attendance todayAttendance = attendanceRepository
                .findAllByUser_Id(userId)
                .stream()
                .filter(a -> today.equals(a.getDate()))
                .findFirst()
                .orElse(null);

        model.addAttribute("user", user);
        model.addAttribute("totalTasks", totalTasks);
        model.addAttribute("completedTasks", completedTasks);
        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("attendancePercentage", attendancePercentage);
        model.addAttribute("todayAttendance", todayAttendance);
        model.addAttribute("recentTasks", recentTasks);

        return "Employee_dashboard/emp_dashboard";
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
