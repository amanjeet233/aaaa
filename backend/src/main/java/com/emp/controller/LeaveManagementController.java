package com.emp.controller;

import com.emp.model.LeaveRequest;
import com.emp.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/manage/leaves")
public class LeaveManagementController {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @GetMapping
    public ResponseEntity<?> getAllLeaves() {
        return ResponseEntity.ok(leaveRequestRepository.findAll());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateLeaveStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        if (status == null || status.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
        }

        Optional<LeaveRequest> leaveOpt = leaveRequestRepository.findById(id);
        if (leaveOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        LeaveRequest leave = leaveOpt.get();
        leave.setStatus(status);
        leaveRequestRepository.save(leave);

        return ResponseEntity.ok(leave);
    }
}