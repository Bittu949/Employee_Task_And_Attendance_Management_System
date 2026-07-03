package com.company.ems.Service.Admin;

import com.company.ems.Entity.Attendance;
import com.company.ems.Entity.User;
import com.company.ems.Exception.InvalidDataException;
import com.company.ems.Repository.AttendanceRepository;
import com.company.ems.Repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.company.ems.Entity.EmploymentHistory;
import com.company.ems.Repository.EmploymentHistoryRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final EmploymentHistoryRepository employmentHistoryRepository;

    public Page<Attendance> getPaginatedAttendance(int page, int size, List<Attendance> attendanceList) {

        if (page < 0) page = 0;

        if (size <= 0) {
            throw new InvalidDataException("Page size must be greater than 0");
        }

        int start = page * size;
        int end = Math.min(start + size, attendanceList.size());

        List<Attendance> paginatedList;

        if (start < attendanceList.size()) {
            paginatedList = attendanceList.subList(start, end);
        } else {
            paginatedList = new ArrayList<>();
        }

        return new PageImpl<>(paginatedList, PageRequest.of(page, size), attendanceList.size());
    }

//    public List<Attendance> showAttendanceRecords() {
//
//        List<User> employees = userRepository.findAllByRole("EMPLOYEE");
//        List<Attendance> finalList = new ArrayList<>();
//
//        LocalDate today = LocalDate.now();
//
//        for (User user : employees) {
//            List<Attendance> records = attendanceRepository.findAllByUser_Id(user.getId());
//            Map<LocalDate, Attendance> attendanceMap = records.stream()
//                    .collect(Collectors.toMap(Attendance::getDate, a -> a));
//
//            if (user.getJoiningDate() == null) continue;
//
//            LocalDate endDate = today;
//            if (user.getLeavingDate() != null) {
//                endDate = user.getLeavingDate().equals(today)
//                        ? today.minusDays(1)
//                        : user.getLeavingDate();
//            }
//
//            for (LocalDate date = user.getJoiningDate();
//                 !date.isAfter(endDate);
//                 date = date.plusDays(1)) {
//
//                Attendance found = attendanceMap.get(date);
//
//                if (found != null) {
//                    handleMissedCheckoutForAdmin(found);
//                    finalList.add(found);
//                } else {
//                    Attendance absent = new Attendance();
//                    absent.setUser(user);
//                    absent.setDate(date);
//                    absent.setStatus("Absent");
//
//                    finalList.add(absent);
//                }
//            }
//        }
//        finalList.sort((a, b) -> b.getDate().compareTo(a.getDate()));
//
//        return finalList;
//    }

    public List<Attendance> showAttendanceRecords() {

        List<User> employees = userRepository.findAllByRole("EMPLOYEE");
        List<Attendance> finalList = new ArrayList<>();

        LocalDate today = LocalDate.now();

        for (User user : employees) {

            List<Attendance> records =
                    attendanceRepository.findAllByUser_Id(user.getId());

            Map<LocalDate, Attendance> attendanceMap = records.stream()
                    .collect(Collectors.toMap(
                            Attendance::getDate,
                            a -> a,
                            (a1, a2) -> a1
                    ));

            List<EmploymentHistory> employmentPeriods =
                    employmentHistoryRepository.findAllByUser_Id(user.getId());

            for (EmploymentHistory period : employmentPeriods) {

                LocalDate startDate = period.getStartDate();

                LocalDate endDate =
                        period.getEndDate() == null
                                ? today
                                : period.getEndDate();

                /*
                 * Special Case:
                 * Activated and deactivated on same day.
                 *
                 * Show the day ONLY if a real attendance record exists.
                 * Otherwise skip it completely.
                 */
                if (period.getEndDate() != null &&
                        startDate.equals(period.getEndDate())) {

                    Attendance sameDayAttendance =
                            attendanceMap.get(startDate);

                    if (sameDayAttendance != null) {

                        handleMissedCheckoutForAdmin(sameDayAttendance);

                        if (!finalList.contains(sameDayAttendance)) {
                            finalList.add(sameDayAttendance);
                        }
                    }

                    continue;
                }

                for (LocalDate date = startDate;
                     !date.isAfter(endDate);
                     date = date.plusDays(1)) {

                    Attendance found = attendanceMap.get(date);

                    if (found != null) {

                        handleMissedCheckoutForAdmin(found);

                        if (!finalList.contains(found)) {
                            finalList.add(found);
                        }

                    } else {

                        LocalDate currentDate = date;

                        boolean alreadyGenerated = finalList.stream()
                                .anyMatch(a ->
                                        a.getUser() != null &&
                                                a.getUser().getId() == user.getId() &&
                                                currentDate.equals(a.getDate()));

                        if (!alreadyGenerated) {

                            Attendance absent = new Attendance();

                            absent.setUser(user);
                            absent.setDate(currentDate);
                            absent.setStatus("ABSENT");

                            finalList.add(absent);
                        }
                    }
                }
            }
        }

        finalList.sort((a, b) -> b.getDate().compareTo(a.getDate()));

        return finalList;
    }

    private void handleMissedCheckoutForAdmin(Attendance attendance) {
        LocalDate today = LocalDate.now();

        if (attendance.getCheckInTime() != null &&
                attendance.getCheckOutTime() == null &&
                attendance.getDate() != null &&
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
            attendance.setWorkingHours(hours + " hrs " + minutes + " min (Auto Closed)");

            attendanceRepository.save(attendance);
        }
    }

    public long totalEmployeesCount() {
        return userRepository.countByRoleAndStatus("EMPLOYEE", "ACTIVE");
    }

    public List<Integer> getYears(){
        List<Integer> years = attendanceRepository.findDistinctYears();
        if (years.isEmpty()) {
            years.add(LocalDate.now().getYear());
        }
        Collections.reverse(years);
        return years;
    }

    public long presentCount(List<Attendance> attendanceList) {
        long count = 0;
        LocalDate today = LocalDate.now();

        for (Attendance attendance : attendanceList) {
            if (today.equals(attendance.getDate()) &&
                    attendance.getUser() != null &&
                    "ACTIVE".equalsIgnoreCase(attendance.getUser().getStatus()) &&
                    "PRESENT".equalsIgnoreCase(attendance.getStatus())) {

                count++;
            }
        }
        return count;
    }

    public long absentCount(List<Attendance> attendanceList) {
        long count = 0;
        LocalDate today = LocalDate.now();

        for (Attendance attendance : attendanceList) {
            if (today.equals(attendance.getDate()) &&
                    attendance.getUser() != null &&
                    "ACTIVE".equalsIgnoreCase(attendance.getUser().getStatus()) &&
                    "ABSENT".equalsIgnoreCase(attendance.getStatus())) {

                count++;
            }
        }
        return count;
    }

    public double showAttendancePercentage(List<Attendance> attendanceList) {

        long present = presentCount(attendanceList);

        long total = userRepository.countByRoleAndStatus("EMPLOYEE", "ACTIVE");

        if (total == 0) return 0;

        return Math.round((present * 100.0) / total);
    }

    public Page<Attendance> filterAttendanceWithPagination(
            String name,
            Integer month,
            Integer year,
            String status,
            String date,
            int page,
            int size) {

        if (page < 0) page = 0;

        if (size <= 0) {
            throw new InvalidDataException("Page size must be greater than 0");
        }

        List<Attendance> attendances =
                filterAttendance(name, month, year, status, date);

        int start = page * size;
        int end = Math.min(start + size, attendances.size());

        List<Attendance> paginatedList;

        if (start < attendances.size()) {
            paginatedList = attendances.subList(start, end);
        } else {
            paginatedList = new ArrayList<>();
        }

        return new PageImpl<>(
                paginatedList,
                PageRequest.of(page, size),
                attendances.size()
        );
    }

    public List<Attendance> filterAttendance(
            String name,
            Integer month,
            Integer year,
            String status,
            String date) {

        List<Attendance> attendance = showAttendanceRecords();

        if (name != null) {
            name = name.trim().toLowerCase();
        }

        if (name != null && !name.isEmpty()) {

            String finalName = name;

            attendance = attendance.stream()
                    .filter(a ->
                            a.getUser() != null &&
                                    a.getUser().getFullName() != null &&
                                    a.getUser().getFullName()
                                            .toLowerCase()
                                            .contains(finalName))
                    .toList();
        }

        if (month != null) {

            if (month < 1 || month > 12) {
                throw new InvalidDataException("Invalid month value");
            }

            attendance = attendance.stream()
                    .filter(a ->
                            a.getDate() != null &&
                                    a.getDate().getMonthValue() == month)
                    .toList();
        }

        if (year != null) {

            attendance = attendance.stream()
                    .filter(a ->
                            a.getDate() != null &&
                                    a.getDate().getYear() == year)
                    .toList();
        }

        if (status != null &&
                !status.trim().isEmpty() &&
                !status.equalsIgnoreCase("ALL")) {

            attendance = attendance.stream()
                    .filter(a ->
                            a.getStatus() != null &&
                                    a.getStatus().equalsIgnoreCase(status))
                    .toList();
        }

        if (date != null && !date.isBlank()) {

            LocalDate selectedDate =
                    LocalDate.parse(date);

            attendance = attendance.stream()
                    .filter(a ->
                            a.getDate() != null &&
                                    a.getDate().equals(selectedDate))
                    .toList();
        }

        return attendance;
    }
}