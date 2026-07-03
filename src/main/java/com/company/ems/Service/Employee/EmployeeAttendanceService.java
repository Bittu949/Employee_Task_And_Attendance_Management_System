package com.company.ems.Service.Employee;

import com.company.ems.Entity.Attendance;
import com.company.ems.Entity.User;
import com.company.ems.Repository.AttendanceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.company.ems.Entity.EmploymentHistory;
import com.company.ems.Repository.EmploymentHistoryRepository;

@Service
public class EmployeeAttendanceService {
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private EmploymentHistoryRepository employmentHistoryRepository;
    public Attendance getTodayAttendance(long userId) {

        LocalDate today = LocalDate.now();
        List<Attendance> records = attendanceRepository.findAllByUser_Id(userId);
        return records.stream()
                .filter(a -> a.getDate().equals(today))
                .findFirst()
                .orElse(null);
    }

    public List<Attendance> getAttendanceHistory(long userId, LocalDate joiningDate) {

        LocalDate today = LocalDate.now();

        List<Attendance> records =
                attendanceRepository.findAllByUser_Id(userId);

        Map<LocalDate, Attendance> attendanceMap = records.stream()
                .collect(Collectors.toMap(
                        Attendance::getDate,
                        a -> a,
                        (a1, a2) -> a1
                ));

        List<EmploymentHistory> employmentPeriods =
                employmentHistoryRepository.findAllByUser_Id(userId);

        List<Attendance> finalList = new ArrayList<>();

        for (EmploymentHistory period : employmentPeriods) {

            LocalDate startDate = period.getStartDate();

            LocalDate endDate =
                    period.getEndDate() == null
                            ? today
                            : period.getEndDate();

            for (LocalDate date = startDate;
                 !date.isAfter(endDate);
                 date = date.plusDays(1)) {

                Attendance found = attendanceMap.get(date);

                if (found != null) {

                    handleMissedCheckout(found, today);

                    if (!finalList.contains(found)) {
                        finalList.add(found);
                    }

                } else {

                    LocalDate currentDate = date;

                    boolean alreadyGenerated =
                            finalList.stream()
                                    .anyMatch(a ->
                                            a.getDate() != null &&
                                                    a.getDate().equals(currentDate));

                    if (!alreadyGenerated) {

                        Attendance absent = new Attendance();

                        absent.setDate(currentDate);
                        absent.setStatus("ABSENT");

                        finalList.add(absent);
                    }
                }
            }
        }

        return finalList.stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .toList();
    }

    private void handleMissedCheckout(Attendance attendance, LocalDate today) {

        if (attendance.getCheckInTime() != null &&
                attendance.getCheckOutTime() == null &&
                !attendance.getDate().equals(today)) {
            LocalTime autoCheckout = LocalTime.of(23, 59);
            attendance.setCheckOutTime(autoCheckout);
            Duration duration = Duration.between(
                    attendance.getCheckInTime(),
                    autoCheckout
            );
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;
            attendance.setStatus("PRESENT");
            String working = hours + " hrs " + minutes + " min (Auto Closed)";
            attendance.setWorkingHours(working);
            attendanceRepository.save(attendance);
        }
    }

    public void checkIn(long userId, User user) {

        LocalDate today = LocalDate.now();
        List<Attendance> records = attendanceRepository.findAllByUser_Id(userId);
        boolean alreadyExists = records.stream()
                .anyMatch(a -> a.getDate().equals(today));
        if(alreadyExists){
            throw new RuntimeException("Already checked in today");
        }
        Attendance attendance = new Attendance();
        attendance.setUser(user);
        attendance.setDate(today);
        attendance.setCheckInTime(LocalTime.now());
        attendance.setStatus("PRESENT");

        attendanceRepository.save(attendance);
    }
    public void checkOut(long userId) {

        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findAllByUser_Id(userId)
                .stream()
                .filter(a -> a.getDate().equals(today))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No check-in found"));

        if(attendance.getCheckOutTime() != null){
            throw new RuntimeException("Already checked out");
        }

        LocalTime checkOutTime = LocalTime.now();

        attendance.setCheckOutTime(checkOutTime);

        Duration duration = Duration.between(attendance.getCheckInTime(), checkOutTime);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;

        String formatted = hours + " hrs " + minutes + " min";
        attendance.setWorkingHours(formatted);

        attendanceRepository.save(attendance);
    }
    public List<Attendance> filterAttendance(long userId,
                                             LocalDate joiningDate,
                                             String status,
                                             Integer month,
                                             Integer year){

        List<Attendance> list = getAttendanceHistory(userId, joiningDate);

        if(status != null && !status.trim().isEmpty()){
            list = list.stream()
                    .filter(a -> a.getStatus() != null &&
                            a.getStatus().equalsIgnoreCase(status))
                    .toList();
        }

        if(month != null){
            list = list.stream()
                    .filter(a -> a.getDate() != null &&
                            a.getDate().getMonthValue() == month)
                    .toList();
        }

        if(year != null){
            list = list.stream()
                    .filter(a -> a.getDate() != null &&
                            a.getDate().getYear() == year)
                    .toList();
        }

        return list;
    }
}