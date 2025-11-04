package com.emp.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.emp.model.Employee;
import com.emp.model.User;
import com.emp.repository.EmployeeRepository;
import com.emp.service.AttendanceService;

import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private EmployeeRepository repo;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    private AttendanceService attendanceService;

    public static class LoginReq {
        @NotBlank(message = "Username is required")
        private String username;
        
        @NotBlank(message = "Password is required")
        private String password;
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
    // username can be email or empId

    @Autowired
    private com.emp.security.JwtUtil jwtUtil;
    @Autowired
    private com.emp.repository.UserRepository userRepo;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginReq req) {
        String username = req.getUsername();
        String password = req.getPassword();

        // Try find by email then by empId
        Employee user = repo.findByEmail(username).orElse(null);
        if (user == null) user = repo.findById(username).orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        }

        // Use BCrypt to verify password. Support legacy plain-text password migration:
        boolean passwordOk = false;
        String stored = user.getPassword();
        if (stored != null) {
            // bcrypt hashes start with $2a$ or $2b$ or $2y$
            if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
                passwordOk = passwordEncoder.matches(password, stored);
            } else {
                // legacy unhashed password in DB: accept it, then migrate to bcrypt
                if (stored.equals(password)) {
                    passwordOk = true;
                    try {
                        String encoded = passwordEncoder.encode(password);
                        user.setPassword(encoded);
                        repo.save(user);
                        // also update users table if a matching user exists
                        if (user.getEmail() != null) {
                            userRepo.findByEmail(user.getEmail()).ifPresent(u -> {
                                u.setPassword(encoded);
                                userRepo.save(u);
                            });
                        }
                    } catch (Exception ex) {
                        // log and continue — migration failure shouldn't block login
                        System.err.println("Failed to migrate plaintext password for empId=" + user.getEmpId() + ": " + ex.getMessage());
                    }
                }
            }
        }

        if (!passwordOk) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid credentials"));
        }
        
        // Auto-attendance logic for logins between 9-10 AM
        LocalTime loginTime = LocalTime.now();
        if (loginTime.isAfter(LocalTime.of(9, 0)) && loginTime.isBefore(LocalTime.of(10, 0))) {
            attendanceService.markPresent(user.getEmpId());
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(user.getEmpId(), user.getRole());
        return ResponseEntity.ok(Map.of(
            "token", token,
            "role", user.getRole(),
            "empId", user.getEmpId(),
            "name", user.getName()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(name = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing Authorization header"));
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
        }

        String subject = jwtUtil.extractUsername(token); // this was set to empId when token created
        String role = jwtUtil.extractClaim(token, claims -> claims.get("role", String.class));

        // Try find Employee by empId
        Employee emp = repo.findById(subject).orElse(null);

        // Try find User by email if available
        Long userId = null;
        String email = null;
        if (emp != null) {
            email = emp.getEmail();
            userId = userRepo.findByEmail(email).map(User::getId).orElse(null);
        } else {
            // as fallback, try parse subject as numeric user id
            try {
                Long id = Long.valueOf(subject);
                if (userRepo.existsById(id)) userId = id;
            } catch (Exception ignored) {}
        }

        Map<String, Object> resp = Map.of(
                "empId", emp != null ? emp.getEmpId() : subject,
                "name", emp != null ? emp.getName() : "",
                "email", email != null ? email : "",
                "userId", userId,
                "role", role
        );

        return ResponseEntity.ok(resp);
    }
}
