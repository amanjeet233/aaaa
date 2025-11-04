package com.emp.controller;

import com.emp.model.Attendance;
import com.emp.model.Employee;
import com.emp.repository.AttendanceRepository;
import com.emp.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping("/date/{date}")
    public List<Attendance> getByDate(@PathVariable String date) {
        LocalDate parsedDate = LocalDate.parse(date);
        return attendanceRepository.findByDate(parsedDate);
    }

    @GetMapping("/employee/{empId}")
    public List<Attendance> getByEmployee(@PathVariable String empId) {
        return attendanceRepository.findByEmployeeId(empId);
    }

    @GetMapping("/month/{yearMonth}")
    public List<Attendance> getByMonth(@PathVariable String yearMonth, @RequestParam(required = false) String employeeId) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        
        if (employeeId != null && !employeeId.isEmpty()) {
            return attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate);
        } else {
            return attendanceRepository.findByDateBetween(startDate, endDate);
        }
    }

    @PostMapping
    public ResponseEntity<?> markAttendance(@RequestBody Attendance attendance) {
        Optional<Employee> employeeOpt = employeeRepository.findById(attendance.getEmployeeId());
        
        if (employeeOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Employee not found"));
        }
        
        attendance.setEmployee(employeeOpt.get());
        Attendance saved = attendanceRepository.save(attendance);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> markBulkAttendance(@RequestBody List<Attendance> attendanceList) {
        for (Attendance attendance : attendanceList) {
            Optional<Employee> employeeOpt = employeeRepository.findById(attendance.getEmployeeId());
            if (employeeOpt.isPresent()) {
                attendance.setEmployee(employeeOpt.get());
            }
        }
        
        List<Attendance> saved = attendanceRepository.saveAll(attendanceList);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAttendance(@PathVariable Long id, @RequestBody Attendance attendance) {
        if (!attendanceRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        attendance.setId(id);
        Optional<Employee> employeeOpt = employeeRepository.findById(attendance.getEmployeeId());
        if (employeeOpt.isPresent()) {
            attendance.setEmployee(employeeOpt.get());
        }
        
        Attendance updated = attendanceRepository.save(attendance);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAttendance(@PathVariable Long id) {
        if (!attendanceRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        attendanceRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("deleted", id));
    }
}