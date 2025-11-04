package com.emp.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.emp.model.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, String> {
    Optional<Employee> findByEmail(String email);
    boolean existsByEmail(String email);
}
