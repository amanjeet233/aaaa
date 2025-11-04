package com.emp.service;

import com.emp.model.Attendance;
import com.emp.model.Employee;
import com.emp.repository.AttendanceRepository;
import com.emp.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    public void markPresent(String employeeId) {
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            return;
        }
        
        // Check if attendance already exists for today
        LocalDate today = LocalDate.now();
        Attendance existingAttendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, today);
        
        if (existingAttendance == null) {
            // Create new attendance record
            Attendance attendance = new Attendance();
            attendance.setEmployee(employee);
            attendance.setEmployeeId(employeeId);
            attendance.setDate(today);
            attendance.setStatus("PRESENT");
            attendance.setInTime(LocalTime.now().toString());
            attendanceRepository.save(attendance);
        }
    }
}