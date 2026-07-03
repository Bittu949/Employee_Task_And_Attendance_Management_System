package com.company.ems.Repository;

import com.company.ems.Entity.EmploymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmploymentHistoryRepository
        extends JpaRepository<EmploymentHistory, Long> {

    List<EmploymentHistory> findAllByUser_Id(Long userId);

    Optional<EmploymentHistory>
    findFirstByUser_IdAndEndDateIsNullOrderByStartDateDesc(Long userId);
}