package com.emp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "leave_balances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "employee_id", unique = true)
    private User employee;

    private Integer casualLeaveBalance = 12; // Default annual casual leave balance
    private Integer sickLeaveBalance = 15;   // Default annual sick leave balance
    private Integer earnedLeaveBalance = 30; // Default annual earned leave balance
    
    private Integer year; // To track leave balances by year
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public User getEmployee() {
        return employee;
    }
    
    public void setEmployee(User employee) {
        this.employee = employee;
    }
    
    public Integer getCasualLeaveBalance() {
        return casualLeaveBalance;
    }
    
    public void setCasualLeaveBalance(Integer casualLeaveBalance) {
        this.casualLeaveBalance = casualLeaveBalance;
    }
    
    public Integer getSickLeaveBalance() {
        return sickLeaveBalance;
    }
    
    public void setSickLeaveBalance(Integer sickLeaveBalance) {
        this.sickLeaveBalance = sickLeaveBalance;
    }
    
    public Integer getEarnedLeaveBalance() {
        return earnedLeaveBalance;
    }
    
    public void setEarnedLeaveBalance(Integer earnedLeaveBalance) {
        this.earnedLeaveBalance = earnedLeaveBalance;
    }
    
    public Integer getYear() {
        return year;
    }
    
    public void setYear(Integer year) {
        this.year = year;
    }
}