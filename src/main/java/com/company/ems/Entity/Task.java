package com.company.ems.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Task{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String taskTitle;
    private String description;
    private LocalDate deadline;
    private String priority;
    @ManyToOne
    @JoinColumn(name = "assigned_user_id")
    private User user;
    private String status;
    private LocalDateTime createdAt;
    private LocalDate assignedAt;
    @Column(name = "deleted")
    private boolean deleted = false;

    public Task(String taskTitle,
                String description,
                LocalDate deadline,
                String priority,
                String status){
        this.taskTitle = taskTitle;
        this.description = description;
        this.deadline = deadline;
        this.priority = priority;
        this.status = status;
    }
}
