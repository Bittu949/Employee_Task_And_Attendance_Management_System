package com.company.ems.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Performance {
    long employeeId;
    String employeeName;
    long tasksAssigned;
    long pending;
    long completionPercentage;
    long attendancePercentage;
    String status;
    private long onTimeCompleted;
    private long lateCompleted;
}
