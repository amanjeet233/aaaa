package com.emp.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveRequestDto {
    private String leaveType;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String reason;
    
    // Adding getter methods explicitly
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
}
