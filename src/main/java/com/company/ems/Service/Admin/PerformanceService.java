package com.company.ems.Service.Admin;

import com.company.ems.Dto.Performance;
import com.company.ems.Entity.Attendance;
import com.company.ems.Entity.Task;
import com.company.ems.Entity.User;
import com.company.ems.Exception.InvalidInputException;
import com.company.ems.Repository.AttendanceRepository;
import com.company.ems.Repository.TaskRepository;
import com.company.ems.Repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class PerformanceService {

    TaskRepository taskRepository;
    AttendanceRepository attendanceRepository;
    UserRepository userRepository;
    AttendanceService attendanceService;

    public Page<Performance> getPaginatedPerformance(int page, int size){

        if(page < 0) page = 0;

        List<Performance> performances = showAllPerformanceDetails();

        int start = page * size;
        int end = Math.min(start + size, performances.size());

        List<Performance> paginatedList;

        if(start < performances.size()){
            paginatedList = performances.subList(start, end);
        } else {
            paginatedList = new ArrayList<>();
        }

        return new PageImpl<>(paginatedList, PageRequest.of(page, size), performances.size());
    }

    public List<Performance> showAllPerformanceDetails(){

        List<User> users = userRepository.findAllByRole("EMPLOYEE");
        List<Attendance> fullAttendanceList = attendanceService.showAttendanceRecords();

        if(users.isEmpty()){
            return new ArrayList<>();
        }

        List<Performance> performances = new ArrayList<>();

        for(User user : users){

            Performance performance = new Performance();

            performance.setEmployeeId(user.getId());
            performance.setEmployeeName(user.getFullName());
            performance.setStatus(user.getStatus());

            long totalTasks = taskRepository.countByUser_IdAndDeletedFalse(user.getId());
            long completedTasks = taskRepository.countByUser_IdAndStatusAndDeletedFalse(user.getId(), "COMPLETED");
            long pendingTasks = taskRepository.countByUser_IdAndStatusAndDeletedFalse(user.getId(), "PENDING");

            performance.setTasksAssigned(totalTasks);
            performance.setPending(pendingTasks);

            if(totalTasks == 0){
                performance.setCompletionPercentage(0);
            }else{
                performance.setCompletionPercentage(Math.round((completedTasks * 100.0) / totalTasks));
            }

            List<Attendance> userAttendance = fullAttendanceList.stream()
                    .filter(a -> a.getUser().getId() == user.getId())
                    .toList();

            long totalAttendance = userAttendance.size();

            long presentDays = userAttendance.stream()
                    .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()))
                    .count();

            if(totalAttendance == 0){
                performance.setAttendancePercentage(0);
            }else{
                performance.setAttendancePercentage(Math.round((presentDays * 100.0) / totalAttendance));
            }

            performances.add(performance);
        }

        return performances;
    }

    public Page<Performance> filterPerformanceWithPagination(String search,
                                                             Long month,
                                                             Long year,
                                                             int page,
                                                             int size){

        if(page < 0) page = 0;

        List<Performance> performances = filterPerformance(search, month, year);

        int start = page * size;
        int end = Math.min(start + size, performances.size());

        List<Performance> paginatedList;

        if(start < performances.size()){
            paginatedList = performances.subList(start, end);
        } else {
            paginatedList = new ArrayList<>();
        }

        return new PageImpl<>(paginatedList, PageRequest.of(page, size), performances.size());
    }

    public List<Performance> filterPerformance(String search, Long month, Long year){

        List<Performance> performances = showAllPerformanceDetails();

        if(search != null){
            search = search.trim().toLowerCase();
            if(!search.isEmpty()){
                String finalSearch = search;
                performances = performances.stream()
                        .filter(p -> p.getEmployeeName() != null &&
                                p.getEmployeeName().toLowerCase().contains(finalSearch))
                        .toList();
            }
        }

        if(month == null && year == null){
            return performances;
        }

        List<Attendance> fullAttendanceList = attendanceService.showAttendanceRecords();

        performances.forEach(p -> {

            long userId = p.getEmployeeId();

            List<Attendance> userAttendance = fullAttendanceList.stream()
                    .filter(a -> a.getUser().getId() == userId)
                    .toList();

            if(year != null){
                userAttendance = userAttendance.stream()
                        .filter(a -> a.getDate().getYear() == year)
                        .toList();
            }

            if(month != null){
                userAttendance = userAttendance.stream()
                        .filter(a -> a.getDate().getMonthValue() == month)
                        .toList();
            }

            long totalAttendance = userAttendance.size();

            long presentDays = userAttendance.stream()
                    .filter(a -> "PRESENT".equalsIgnoreCase(a.getStatus()))
                    .count();

            p.setAttendancePercentage(totalAttendance == 0 ? 0 :
                    Math.round((presentDays * 100.0) / totalAttendance));

            List<Task> userTasks = taskRepository.findAllByUser_IdAndDeletedFalse(userId);

            if(year != null){
                userTasks = userTasks.stream()
                        .filter(t -> t.getAssignedAt() != null &&
                                t.getAssignedAt().getYear() == year)
                        .toList();
            }

            if(month != null){
                userTasks = userTasks.stream()
                        .filter(t -> t.getAssignedAt() != null &&
                                t.getAssignedAt().getMonthValue() == month)
                        .toList();
            }

            long totalTasks = userTasks.size();

            long completedTasks = userTasks.stream()
                    .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                    .count();

            long pendingTasks = userTasks.stream()
                    .filter(t -> "PENDING".equalsIgnoreCase(t.getStatus()))
                    .count();

            p.setTasksAssigned(totalTasks);
            p.setPending(pendingTasks);

            p.setCompletionPercentage(totalTasks == 0 ? 0 :
                    Math.round((completedTasks * 100.0) / totalTasks));
        });

        return performances;
    }

    public long totalEmployeesCount(){
        return userRepository.countByRole("EMPLOYEE");
    }

    public double averageCompletionRate(){

        long totalTasks = taskRepository.countByDeletedFalse();

        if(totalTasks == 0){
            return 0;
        }

        long completedTasks = taskRepository.countByStatusAndDeletedFalse("COMPLETED");

        return Math.round((completedTasks * 100.0) / totalTasks);
    }

    public long totalCompletedTasks(){
         List<Task> tasks = taskRepository.findAllByStatus("COMPLETED");
         return tasks.stream().filter(t -> !t.isDeleted()).count();
    }

    public long totalPendingTasks(){
        List<Task> tasks = taskRepository.findAllByStatus("PENDING");
        return tasks.stream().filter(t -> !t.isDeleted()).count();
    }

    public long countByUserIdAndMonth(long userId, long month){
        return taskRepository.findAllByUser_Id(userId)
                .stream()
                .filter(t -> t.getAssignedAt() != null &&
                        t.getAssignedAt().getMonthValue() == month)
                .count();
    }

    public long countByUserIdAndYear(long userId, long year){
        return taskRepository.findAllByUser_Id(userId)
                .stream()
                .filter(t -> t.getAssignedAt() != null &&
                        t.getAssignedAt().getYear() == year)
                .count();
    }

    public long countByUserIdMonthAndStatus(long userId, long month, String status){
        return taskRepository.findAllByUser_Id(userId)
                .stream()
                .filter(t -> t.getAssignedAt()!=null &&
                        t.getAssignedAt().getMonthValue()==month &&
                        status.equalsIgnoreCase(t.getStatus()))
                .count();
    }

    public long countByUserIdYearAndStatus(long userId, long year, String status){
        return taskRepository.findAllByUser_Id(userId)
                .stream()
                .filter(t -> t.getAssignedAt()!=null &&
                        t.getAssignedAt().getYear()==year &&
                        status.equalsIgnoreCase(t.getStatus()))
                .count();
    }

    public long countAttendanceByUserIdAndMonth(long userId, long month){
        return attendanceRepository.findAllByUser_Id(userId)
                .stream()
                .filter(a -> a.getDate()!=null &&
                        a.getDate().getMonthValue()==month)
                .count();
    }

    public long countAttendanceByUserIdAndYear(long userId, long year){
        return attendanceRepository.findAllByUser_Id(userId)
                .stream()
                .filter(a -> a.getDate()!=null &&
                        a.getDate().getYear()==year)
                .count();
    }

    public long countAttendanceByUserIdMonthAndStatus(long userId, long month, String status){
        return attendanceRepository.findAllByUser_Id(userId)
                .stream()
                .filter(a -> a.getDate()!=null &&
                        a.getDate().getMonthValue()==month &&
                        status.equalsIgnoreCase(a.getStatus()))
                .count();
    }

    public long countAttendanceByUserIdYearAndStatus(long userId, long year, String status){
        return attendanceRepository.findAllByUser_Id(userId)
                .stream()
                .filter(a -> a.getDate()!=null &&
                        a.getDate().getYear()==year &&
                        status.equalsIgnoreCase(a.getStatus()))
                .count();
    }

    public long countTasksByUserIdMonthAndYear(long userId, long month, long year){
        return taskRepository.findAllByUser_Id(userId)
                .stream()
                .filter(t -> t.getAssignedAt()!=null &&
                        t.getAssignedAt().getMonthValue()==month &&
                        t.getAssignedAt().getYear()==year)
                .count();
    }

    public long countAttendanceByUserIdMonthAndYear(long userId, long month, long year){
        return attendanceRepository.findAllByUser_Id(userId)
                .stream()
                .filter(a -> a.getDate()!=null &&
                        a.getDate().getMonthValue()==month &&
                        a.getDate().getYear()==year)
                .count();
    }

    public long countTasksByUserIdMonthYearAndStatus(long userId, long month, long year, String status){
        return taskRepository.findAllByUser_Id(userId)
                .stream()
                .filter(t -> t.getAssignedAt()!=null &&
                        t.getAssignedAt().getMonthValue()==month &&
                        t.getAssignedAt().getYear()==year &&
                        status.equalsIgnoreCase(t.getStatus()))
                .count();
    }

    public long countAttendanceByUserIdMonthYearAndStatus(long userId, long month, long year, String status){
        return attendanceRepository.findAllByUser_Id(userId)
                .stream()
                .filter(a -> a.getDate()!=null &&
                        a.getDate().getMonthValue()==month &&
                        a.getDate().getYear()==year &&
                        status.equalsIgnoreCase(a.getStatus()))
                .count();
    }
}