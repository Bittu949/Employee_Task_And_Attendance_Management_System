package com.company.ems.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TaskSummaryDto {

    private Long taskId;
    private String taskTitle;
    private String employeeName;
    private String deadline;
    private String priority;
    private String status;
}