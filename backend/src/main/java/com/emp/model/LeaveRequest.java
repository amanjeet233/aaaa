package com.emp.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "leave_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Leave type is required")
    private String leaveType; // CASUAL, SICK, EARNED, etc.

    @NotNull(message = "From date is required")
    private LocalDate fromDate;
    
    @NotNull(message = "To date is required")
    private LocalDate toDate;

    @NotBlank(message = "Reason is required")
    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    @Column(length=1000)
    private String reason;

    private String status = "PENDING"; // PENDING / APPROVED / REJECTED

    @Column(length=1000)
    private String managerComment;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private User employee;

    private String documentUrl; // for uploaded medical docs (optional)
    
    // Adding explicit getter methods
    public String getLeaveType() {
        return leaveType;
    }
    
    public LocalDate getFromDate() {
        return fromDate;
    }
    
    public LocalDate getToDate() {
        return toDate;
    }
    
    public String getReason() {
        return reason;
    }
    
    public String getStatus() {
        return status;
    }
    
    // Adding explicit setter methods
    public void setEmployee(User employee) {
        this.employee = employee;
    }
    
    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }
    
    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }
    
    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public void setManagerComment(String managerComment) {
        this.managerComment = managerComment;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Long getId() {
        return id;
    }
    
    public User getEmployee() {
        return employee;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }
}
