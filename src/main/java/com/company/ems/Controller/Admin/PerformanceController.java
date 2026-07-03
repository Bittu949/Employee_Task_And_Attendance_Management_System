package com.company.ems.Controller.Admin;

import com.company.ems.Dto.Performance;
import com.company.ems.Exception.DataExportException;
import com.company.ems.Repository.AttendanceRepository;
import com.company.ems.Service.Admin.AttendanceService;
import com.company.ems.Service.Admin.PerformanceService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Controller
@RequestMapping("/admin/performance")
@AllArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;
    private final AttendanceService attendanceService;

    @GetMapping("/details")
    public String displayPerformanceDetails(Model model,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {

        Page<Performance> performancePage =
                performanceService.getPaginatedPerformance(page, size);

        model.addAttribute("performances", performancePage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", performancePage.getTotalPages());
        model.addAttribute("totalEmployees", performanceService.totalEmployeesCount());
        model.addAttribute("averageCompletionRate", performanceService.averageCompletionRate());
        model.addAttribute("totalCompletedTasks", performanceService.totalCompletedTasks());
        model.addAttribute("years", attendanceService.getYears());
        model.addAttribute("totalPendingTasks", performanceService.totalPendingTasks());

        return "Admin_dashboard/Performance_report/performance_report";
    }

    @GetMapping("/filter")
    @ResponseBody
    public Page<Performance> filter(@RequestParam(required = false) String search,
                                    @RequestParam(required = false) Long month,
                                    @RequestParam(required = false) Long year,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {

        return performanceService.filterPerformanceWithPagination(search, month, year, page, size);
    }

    @GetMapping("/export")
    public void exportPerformance(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long month,
            @RequestParam(required = false) Long year,
            HttpServletResponse response) {

        try {
            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=performance_report.csv");

            List<Performance> performances =
                    performanceService.filterPerformance(search, month, year);

            PrintWriter writer = response.getWriter();

            writer.println("Employee Name,Tasks Assigned,Pending,On Time Completed,Late Completed,Completion %,Attendance %,Status");

            for (Performance p : performances) {
                writer.println(
                        p.getEmployeeName() + "," +
                                p.getTasksAssigned() + "," +
                                p.getPending() + "," +
                                p.getOnTimeCompleted() + "," +
                                p.getLateCompleted() + "," +
                                p.getCompletionPercentage() + "," +
                                p.getAttendancePercentage() + "," +
                                p.getStatus()
                );
            }

            writer.flush();
            writer.close();

        } catch (IOException e) {
            throw new DataExportException("Error exporting performance report");
        }
    }
}
