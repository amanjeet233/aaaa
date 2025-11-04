package com.emp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.emp.model.Employee;
import com.emp.repository.EmployeeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.emp.security.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.emp.repository.UserRepository;
import com.emp.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    @Autowired
    private EmployeeRepository repo;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepo;

    @GetMapping
    public List<Employee> all() {
        return repo.findAll();
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody Employee emp) {
        if (repo.existsById(emp.getEmpId()) || repo.existsByEmail(emp.getEmail())) {
            return ResponseEntity.status(409).body(Map.of("message", "Employee exists"));
        }
        // Hash password before saving (if provided)
        if (emp.getPassword() != null && !emp.getPassword().isEmpty()) {
            String hashed = passwordEncoder.encode(emp.getPassword());
            emp.setPassword(hashed);
        }
        repo.save(emp);

        // Create corresponding User entry if not present (so numeric userId exists)
        if (emp.getEmail() != null && !emp.getEmail().isEmpty()) {
            userRepo.findByEmail(emp.getEmail()).orElseGet(() -> {
                User u = new User();
                u.setName(emp.getName());
                u.setEmail(emp.getEmail());
                u.setPassword(emp.getPassword()); // already hashed
                u.setRole(emp.getRole() != null ? emp.getRole().toUpperCase() : "EMPLOYEE");
                return userRepo.save(u);
            });
        }

        return ResponseEntity.ok(emp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        Optional<Employee> o = repo.findById(id);
        return o.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Employee payload) {
        return repo.findById(id).map(e -> {
            e.setName(payload.getName());
            e.setEmail(payload.getEmail());
            if (payload.getPassword() != null && !payload.getPassword().isEmpty()) {
                String hashed = passwordEncoder.encode(payload.getPassword());
                e.setPassword(hashed);
                // update users table password if exists
                if (e.getEmail() != null) {
                    userRepo.findByEmail(e.getEmail()).ifPresent(u -> {
                        u.setPassword(hashed);
                        userRepo.save(u);
                    });
                }
            }
            e.setDepartment(payload.getDepartment());
            e.setRole(payload.getRole());
            repo.save(e);
            return ResponseEntity.ok(e);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("deleted", id));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(name = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing Authorization header"));
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
        String subject = jwtUtil.extractUsername(token); // stored empId at token creation
        return repo.findById(subject).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
