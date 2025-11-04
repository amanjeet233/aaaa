package com.emp.repository;

import com.emp.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByDate(LocalDate date);
    List<Attendance> findByEmployeeId(String employeeId);
    Attendance findByEmployeeIdAndDate(String employeeId, LocalDate date);
    List<Attendance> findByDateBetween(LocalDate startDate, LocalDate endDate);
    List<Attendance> findByEmployeeIdAndDateBetween(String employeeId, LocalDate startDate, LocalDate endDate);
}