package com.emp.controller;

import com.emp.model.LeaveRequest;
import com.emp.repository.LeaveRequestRepository;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final LeaveRequestRepository repo;

    public ReportController(LeaveRequestRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv() {
        List<LeaveRequest> all = repo.findAll();
        String header = "id,employeeEmail,leaveType,fromDate,toDate,status,reason\n";
        String body = all.stream().map(l -> String.format("%d,%s,%s,%s,%s,%s,%s",
                l.getId(),
                l.getEmployee() == null ? "" : l.getEmployee().getEmail(),
                l.getLeaveType(),
                l.getFromDate(),
                l.getToDate(),
                l.getStatus(),
                (l.getReason() == null ? "" : l.getReason().replace(",", " ")))
        ).collect(Collectors.joining("\n"));
        byte[] bytes = (header + body).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("leaves.csv").build());
        headers.setContentLength(bytes.length);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}
