package com.emp.controller;

import com.emp.dto.LeaveRequestDto;
import com.emp.model.LeaveBalance;
import com.emp.model.LeaveRequest;
import com.emp.model.User;
import com.emp.repository.LeaveRequestRepository;
import com.emp.repository.UserRepository;
import com.emp.service.FileStorageService;
import com.emp.service.LeaveBalanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/leaves")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:8080"})
public class LeaveController {

    private final LeaveRequestRepository leaveRepo;
    private final UserRepository userRepo;
    private final FileStorageService fileStorageService;
    private final LeaveBalanceService leaveBalanceService;

    public LeaveController(LeaveRequestRepository leaveRepo, UserRepository userRepo, 
                          FileStorageService fileStorageService, LeaveBalanceService leaveBalanceService) {
        this.leaveRepo = leaveRepo;
        this.userRepo = userRepo;
        this.fileStorageService = fileStorageService;
        this.leaveBalanceService = leaveBalanceService;
    }

    @GetMapping
    public List<LeaveRequest> getAll() {
        return leaveRepo.findAll();
    }

    @GetMapping("/employee/{employeeId}")
    public List<LeaveRequest> byEmployee(@PathVariable Long employeeId) {
        return leaveRepo.findByEmployeeId(employeeId);
    }

    // alias used by CalendarView component; returns only APPROVED with startDate/endDate keys
    @GetMapping("/all")
    public List<Map<String, Object>> approvedForCalendar() {
        return leaveRepo.findAll().stream()
                .filter(l -> "APPROVED".equalsIgnoreCase(l.getStatus()))
                .map(l -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", l.getId());
                    Map<String, Object> emp = new HashMap<>();
                    emp.put("name", l.getEmployee() != null ? l.getEmployee().getName() : "");
                    m.put("employee", emp);
                    m.put("startDate", l.getFromDate());
                    m.put("endDate", l.getToDate());
                    return m;
                })
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestParam Long employeeId, @RequestBody LeaveRequestDto dto) {
        Optional<User> empOpt = userRepo.findById(employeeId);
        if (empOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Employee not found"));

        LeaveRequest lr = new LeaveRequest();
        lr.setEmployee(empOpt.get());
        lr.setLeaveType(dto.getLeaveType());
        lr.setFromDate(dto.getFromDate());
        lr.setToDate(dto.getToDate());
        lr.setReason(dto.getReason());
        lr.setStatus("PENDING");
        LeaveRequest saved = leaveRepo.save(lr);
        return ResponseEntity.ok(saved);
    }

    // compatibility alias with existing leaveService.js
    @PostMapping("/apply")
    public ResponseEntity<?> apply(@RequestBody Map<String, Object> payload) {
        Object empIdObj = payload.get("employeeId");
        if (empIdObj == null) return ResponseEntity.badRequest().body(Map.of("error", "employeeId is required"));
        Long employeeId = Long.valueOf(empIdObj.toString());
        Optional<User> empOpt = userRepo.findById(employeeId);
        if (empOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Employee not found"));

        LeaveRequest lr = new LeaveRequest();
        lr.setEmployee(empOpt.get());
        lr.setLeaveType(String.valueOf(payload.getOrDefault("leaveType", "CASUAL")));
        lr.setReason(String.valueOf(payload.getOrDefault("reason", "")));
        // Expecting ISO dates in payload
        lr.setFromDate(java.time.LocalDate.parse(String.valueOf(payload.get("fromDate"))));
        lr.setToDate(java.time.LocalDate.parse(String.valueOf(payload.get("toDate"))));
        lr.setStatus("PENDING");
        LeaveRequest saved = leaveRepo.save(lr);
        return ResponseEntity.ok(saved);
    }
    
    @PostMapping("/apply-with-document")
    public ResponseEntity<?> applyWithDocument(
            @RequestParam("employeeId") Long employeeId,
            @RequestParam("leaveType") String leaveType,
            @RequestParam("fromDate") String fromDate,
            @RequestParam("toDate") String toDate,
            @RequestParam("reason") String reason,
            @RequestParam(value = "document", required = false) MultipartFile document) {
        
        Optional<User> empOpt = userRepo.findById(employeeId);
        if (empOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Employee not found"));

        LeaveRequest lr = new LeaveRequest();
        lr.setEmployee(empOpt.get());
        lr.setLeaveType(leaveType);
        lr.setFromDate(java.time.LocalDate.parse(fromDate));
        lr.setToDate(java.time.LocalDate.parse(toDate));
        lr.setReason(reason);
        lr.setStatus("PENDING");
        
        // Handle document upload if present
        if (document != null && !document.isEmpty()) {
            String documentUrl = fileStorageService.storeFile(document);
            lr.setDocumentUrl(documentUrl);
        }
        
        LeaveRequest saved = leaveRepo.save(lr);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        Optional<LeaveRequest> lrOpt = leaveRepo.findById(id);
        if (lrOpt.isEmpty()) return ResponseEntity.notFound().build();
        LeaveRequest lr = lrOpt.get();
        
        // Check if employee has sufficient leave balance
        if (!leaveBalanceService.checkLeaveAvailability(lr)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Insufficient leave balance"));
        }
        
        lr.setStatus("APPROVED");
        if (body != null && body.get("managerComment") != null) {
            lr.setManagerComment(body.get("managerComment"));
        }
        
        // Update leave balance when approved
        leaveBalanceService.updateLeaveBalance(lr);
        
        leaveRepo.save(lr);
        return ResponseEntity.ok(lr);
    }
    
    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<?> getLeaveBalance(@PathVariable Long employeeId, @RequestParam(required = false) Integer year) {
        // Default to current year if not specified
        int requestedYear = year != null ? year : LocalDate.now().getYear();
        
        Optional<User> empOpt = userRepo.findById(employeeId);
        if (empOpt.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Employee not found"));
        
        LeaveBalance balance = leaveBalanceService.getOrCreateLeaveBalance(empOpt.get(), requestedYear);
        return ResponseEntity.ok(balance);
    }
}
