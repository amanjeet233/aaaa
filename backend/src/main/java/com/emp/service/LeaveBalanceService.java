package com.emp.service;

import com.emp.model.LeaveBalance;
import com.emp.model.LeaveRequest;
import com.emp.model.User;
import com.emp.repository.LeaveBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;

    public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepository) {
        this.leaveBalanceRepository = leaveBalanceRepository;
    }

    @Transactional
    public LeaveBalance getOrCreateLeaveBalance(User employee, int year) {
        Optional<LeaveBalance> existingBalance = leaveBalanceRepository.findByEmployeeIdAndYear(employee.getId(), year);
        
        if (existingBalance.isPresent()) {
            return existingBalance.get();
        } else {
            LeaveBalance newBalance = new LeaveBalance();
            newBalance.setEmployee(employee);
            newBalance.setYear(year);
            // Default values are set in the entity
            return leaveBalanceRepository.save(newBalance);
        }
    }

    @Transactional
    public boolean checkLeaveAvailability(LeaveRequest leaveRequest) {
        User employee = leaveRequest.getEmployee();
        String leaveType = leaveRequest.getLeaveType();
        LocalDate fromDate = leaveRequest.getFromDate();
        LocalDate toDate = leaveRequest.getToDate();
        
        // Calculate number of days
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        
        // Get current year's leave balance
        LeaveBalance balance = getOrCreateLeaveBalance(employee, fromDate.getYear());
        
        // Check if enough leave balance is available
        switch (leaveType.toUpperCase()) {
            case "CASUAL":
                return balance.getCasualLeaveBalance() >= days;
            case "SICK":
                return balance.getSickLeaveBalance() >= days;
            case "EARNED":
                return balance.getEarnedLeaveBalance() >= days;
            default:
                return false;
        }
    }

    @Transactional
    public void updateLeaveBalance(LeaveRequest leaveRequest) {
        User employee = leaveRequest.getEmployee();
        String leaveType = leaveRequest.getLeaveType();
        LocalDate fromDate = leaveRequest.getFromDate();
        LocalDate toDate = leaveRequest.getToDate();
        
        // Calculate number of days
        long days = ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        
        // Get current year's leave balance
        LeaveBalance balance = getOrCreateLeaveBalance(employee, fromDate.getYear());
        
        // Update the appropriate leave balance
        switch (leaveType.toUpperCase()) {
            case "CASUAL":
                balance.setCasualLeaveBalance(balance.getCasualLeaveBalance() - (int)days);
                break;
            case "SICK":
                balance.setSickLeaveBalance(balance.getSickLeaveBalance() - (int)days);
                break;
            case "EARNED":
                balance.setEarnedLeaveBalance(balance.getEarnedLeaveBalance() - (int)days);
                break;
        }
        
        leaveBalanceRepository.save(balance);
    }
    
    public LeaveBalance getEmployeeLeaveBalance(Long employeeId, Integer year) {
        return leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, year)
                .orElse(null);
    }
}