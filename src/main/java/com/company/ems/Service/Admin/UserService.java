package com.company.ems.Service.Admin;

import com.company.ems.Entity.Attendance;
import com.company.ems.Entity.User;
import com.company.ems.Exception.InvalidInputException;
import com.company.ems.Exception.ResourceNotFoundException;
import com.company.ems.Repository.AttendanceRepository;
import com.company.ems.Repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class UserService {

    UserRepository userRepository;
    AttendanceRepository attendanceRepository;
    PasswordEncoder passwordEncoder;

    public void addUser(User user, String confirmPassword){

        if(user == null){
            throw new InvalidInputException("User cannot be null");
        }

        if(confirmPassword == null){
            throw new InvalidInputException("Confirm password is required");
        }

        if(!user.getPassword().equals(confirmPassword)){
            throw new InvalidInputException("Passwords do not match");
        }

        String email = user.getEmail().trim().toLowerCase();

        if(userRepository.findByEmail(email).isPresent()){
            throw new InvalidInputException("Email already exists");
        }

        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if(user.getStatus()!=null && user.getStatus().equalsIgnoreCase("ACTIVE")) {
            user.setJoiningDate(LocalDate.now());
            user.setLeavingDate(null);
        }
        if(user.getStatus()!=null && user.getStatus().equalsIgnoreCase("INACTIVE")){
            user.setJoiningDate(null);
            user.setLeavingDate(null);
        }

        userRepository.save(user);
    }

    public void editUser(long userId,
                         String fullName,
                         String email,
                         String userName,
                         String role,
                         String status){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFullName(fullName);

        if(email != null){

            String normalizedEmail = email.trim().toLowerCase();

            if(!normalizedEmail.equals(user.getEmail())){

                if(userRepository.findByEmail(normalizedEmail).isPresent()){
                    throw new InvalidInputException("Email already exists");
                }

                user.setEmail(normalizedEmail);
            }
        }

        if(userName != null && !userName.isEmpty()){
            user.setUsername(userName);
        }

        if(role != null && !role.isEmpty()){
            user.setRole(role);
        }

        if(status != null && !status.isEmpty()){
            if(user.getStatus().equalsIgnoreCase("INACTIVE") && status.equalsIgnoreCase("ACTIVE")){
                user.setJoiningDate(LocalDate.now());
                user.setLeavingDate(null);
            }

            else if(user.getStatus().equalsIgnoreCase("ACTIVE") && status.equalsIgnoreCase("INACTIVE")){

                LocalDate today = LocalDate.now();
                LocalDate leavingDate = today.minusDays(1);

                if(user.getJoiningDate() != null &&
                        leavingDate.isBefore(user.getJoiningDate()) &&
                        !user.getJoiningDate().equals(today)) {

                    throw new InvalidInputException("Leaving date cannot be before joining date");
                }
                List<Attendance> todayRecords = attendanceRepository.findAllByUser_Id(user.getId())
                        .stream()
                        .filter(a -> a.getDate() != null && a.getDate().equals(today))
                        .toList();

                if(!todayRecords.isEmpty()){
                    attendanceRepository.deleteAll(todayRecords);
                }

                user.setLeavingDate(leavingDate);
            }
            user.setStatus(status);
        }

        userRepository.save(user);
    }

    public List<User> getActiveUsersExcept(Long userId){

        if(userId == null){
            throw new InvalidInputException("UserId cannot be null");
        }

        return userRepository.findAllByRole("EMPLOYEE")
                .stream()
                .filter(u -> u.getStatus()!=null && u.getStatus().equalsIgnoreCase("ACTIVE"))
                .filter(u -> u.getId() != userId)
                .toList();
    }

    public List<User> getActiveUsers(){
        return userRepository.findAllByRole("EMPLOYEE")
                .stream()
                .filter(u -> u.getStatus()!=null && u.getStatus().equalsIgnoreCase("ACTIVE"))
                .toList();
    }

    public void activateUser(long userId){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if(user.getStatus()!=null && user.getStatus().equalsIgnoreCase("ACTIVE")){
            throw new InvalidInputException("User is already active");
        }

        user.setStatus("ACTIVE");
        user.setJoiningDate(LocalDate.now());
        user.setLeavingDate(null);
        userRepository.save(user);
    }

    public void deactivateUser(long userId){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if(user.getStatus()!=null && user.getStatus().equalsIgnoreCase("INACTIVE")){
            throw new InvalidInputException("User is already inactive");
        }

        LocalDate today = LocalDate.now();
        LocalDate leavingDate = today.minusDays(1);

        if(user.getJoiningDate() != null &&
                leavingDate.isBefore(user.getJoiningDate()) &&
                !user.getJoiningDate().equals(today)) {
            throw new InvalidInputException("Leaving date cannot be before joining date");
        }

        List<Attendance> todayRecords = attendanceRepository.findAllByUser_Id(user.getId())
                .stream()
                .filter(a -> a.getDate() != null && a.getDate().equals(today))
                .toList();

        if(!todayRecords.isEmpty()){
            attendanceRepository.deleteAll(todayRecords);
        }

        user.setStatus("INACTIVE");
        user.setLeavingDate(leavingDate);
        userRepository.save(user);
    }

    public void resetPassword(long userId, String newPassword, String confirmPassword){

        if(newPassword == null || confirmPassword == null){
            throw new InvalidInputException("Password fields cannot be empty");
        }

        if(!newPassword.equals(confirmPassword)){
            throw new InvalidInputException("Passwords do not match");
        }

        if(newPassword.length() < 6){
            throw new InvalidInputException("Password must be at least 6 characters");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public Page<User> paginatedUsers(int page, int size){

        if(page < 0) page = 0;

        List<User> users = showAllUsers();

        int start = page * size;
        int end = Math.min(start + size, users.size());

        List<User> paginatedList;

        if(start < users.size()){
            paginatedList = users.subList(start, end);
        } else {
            paginatedList = new ArrayList<>();
        }

        return new PageImpl<>(paginatedList, PageRequest.of(page, size), users.size());
    }

    public List<User> showAllUsers(){
        return userRepository.findAllByRole("EMPLOYEE");
    }

    public Page<User> filterPaginatedUsers(String search,
                                           String role,
                                           String status,
                                           int page,
                                           int size){

        if(page < 0) page = 0;

        List<User> users = filterUsers(search, role, status);

        int start = page * size;
        int end = Math.min(start + size, users.size());

        List<User> paginatedList;

        if(start < users.size()){
            paginatedList = users.subList(start, end);
        } else {
            paginatedList = new ArrayList<>();
        }

        return new PageImpl<>(paginatedList, PageRequest.of(page, size), users.size());
    }

    public List<User> filterUsers(String search, String role, String status){

        List<User> list = userRepository.findAllByRole("EMPLOYEE");

        if(search != null){
            search = search.trim().toLowerCase();

            if(!search.isEmpty()){
                String finalSearch = search;

                list = list.stream()
                        .filter(u ->
                                (u.getFullName()!=null && u.getFullName().toLowerCase().contains(finalSearch)) ||
                                        (u.getEmail()!=null && u.getEmail().toLowerCase().contains(finalSearch))
                        )
                        .toList();
            }
        }

        if(role != null && !role.equalsIgnoreCase("ALL")){
            list = list.stream()
                    .filter(u -> u.getRole()!=null && u.getRole().equalsIgnoreCase(role))
                    .toList();
        }

        if(status != null && !status.equalsIgnoreCase("ALL")){
            list = list.stream()
                    .filter(u -> u.getStatus()!=null && u.getStatus().equalsIgnoreCase(status))
                    .toList();
        }

        return list;
    }

    public List<User> getAllUsers(){
        return userRepository.findAllByRole("EMPLOYEE");
    }

    public User getUserById(long userId){

        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}