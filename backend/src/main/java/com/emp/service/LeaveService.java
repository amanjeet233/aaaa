package com.emp.service;

import com.emp.model.LeaveRequest;
import com.emp.model.User;
import com.emp.repository.LeaveRequestRepository;
import com.emp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaveService {
    private final LeaveRequestRepository leaveRepo;
    private final UserRepository userRepo;
    private final EmailService emailService;

    public LeaveService(LeaveRequestRepository leaveRepo, UserRepository userRepo, EmailService emailService) {
        this.leaveRepo = leaveRepo;
        this.userRepo = userRepo;
        this.emailService = emailService;
    }

    public LeaveRequest applyLeave(LeaveRequest lr) {
        LeaveRequest saved = leaveRepo.save(lr);
        // notify manager/employee (stub)
        emailService.sendEmail(lr.getEmployee().getEmail(), "Leave applied", "Your leave is submitted");
        return saved;
    }

    public List<LeaveRequest> getAll() { return leaveRepo.findAll(); }

    public List<LeaveRequest> getByEmployee(Long empId) { return leaveRepo.findByEmployeeId(empId); }

    public LeaveRequest approveOrReject(Long id, String status, String managerComment) {
        LeaveRequest lr = leaveRepo.findById(id).orElseThrow(() -> new RuntimeException("Leave not found"));
        lr.setStatus(status);
        lr.setManagerComment(managerComment);
        LeaveRequest saved = leaveRepo.save(lr);
        emailService.sendEmail(lr.getEmployee().getEmail(), "Leave " + status, "Your leave has been " + status);
        return saved;
    }
}
