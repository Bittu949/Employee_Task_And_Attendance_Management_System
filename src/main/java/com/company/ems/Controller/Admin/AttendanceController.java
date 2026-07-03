package com.company.ems.Controller.Admin;

import com.company.ems.Entity.Attendance;
import com.company.ems.Service.Admin.AttendanceService;
import com.company.ems.Exception.BadRequestException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin/attendance")
@AllArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/allRecords")
    public String showAttendanceRecords(Model model,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "12") int size){

        if(page < 0 || size <= 0){
            throw new BadRequestException("Invalid pagination parameters");
        }
        List<Attendance> attendanceList = attendanceService.showAttendanceRecords();

        Page<Attendance> attendancePage =
                attendanceService.getPaginatedAttendance(page, size, attendanceList);

        model.addAttribute("attendanceRecords", attendancePage.getContent());
        model.addAttribute("currentPage", attendancePage.getNumber());
        model.addAttribute("totalPages", attendancePage.getTotalPages());
        model.addAttribute("totalEmployees", attendanceService.totalEmployeesCount());
        model.addAttribute("presentCount", attendanceService.presentCount(attendanceList));
        model.addAttribute("years", attendanceService.getYears());
        model.addAttribute("absentCount", attendanceService.absentCount(attendanceList));
        model.addAttribute("attendancePercentage", attendanceService.showAttendancePercentage(attendanceList));

        return "Admin_dashboard/Attendance_report/attendance_reports";
    }

    @GetMapping("/filter")
    @ResponseBody
    public Page<Attendance> filterAttendance(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size){

        if(page < 0 || size <= 0){
            throw new BadRequestException("Invalid pagination parameters");
        }

        if(month != null && (month < 1 || month > 12)){
            throw new BadRequestException("Invalid month value");
        }

        if(year != null && year < 2000){
            throw new BadRequestException("Invalid year value");
        }

        return attendanceService
                .filterAttendanceWithPagination(
                        name,
                        month,
                        year,
                        status,
                        date,
                        page,
                        size);
    }

    @GetMapping("/export")
    public void exportAttendance(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date,
            HttpServletResponse response) throws IOException {

        if(month != null && (month < 1 || month > 12)){
            throw new BadRequestException("Invalid month value");
        }

        if(year != null && year < 2000){
            throw new BadRequestException("Invalid year value");
        }

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=attendance_report.csv");

        List<Attendance> attendances =
                attendanceService.filterAttendance(
                        search,
                        month,
                        year,
                        status,
                        date);

        PrintWriter writer = response.getWriter();

        writer.println("Employee Name,Date,Check-in,Check-out,Working Hours,Status");

        for (Attendance a : attendances) {

            writer.println(
                    "\"" + (a.getUser() != null ? a.getUser().getFullName() : "N/A") + "\"," +
                            a.getDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "," +
                            (a.getCheckInTime() != null ? a.getCheckInTime() : "-") + "," +
                            (a.getCheckOutTime() != null ? a.getCheckOutTime() : "-") + "," +
                            (a.getWorkingHours() != null ? a.getWorkingHours() : "-") + "," +
                            a.getStatus()
            );
        }

        writer.flush();
        writer.close();
    }
}