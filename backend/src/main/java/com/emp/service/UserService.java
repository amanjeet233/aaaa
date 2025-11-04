package com.emp.service;

import com.emp.model.User;
import com.emp.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepo;
    private final BCryptPasswordEncoder encoder;

    public UserService(UserRepository userRepo, BCryptPasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    public User register(String name, String email, String rawPassword, String role) {
        if (userRepo.findByEmail(email).isPresent()) throw new RuntimeException("Email already taken");
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword(encoder.encode(rawPassword));
        u.setRole(role);
        return userRepo.save(u);
    }

    public Optional<User> findByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    public User findById(Long id) {
        return userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }
}
