package com.example.onlinebankingapp.repositories;

import com.example.onlinebankingapp.entities.EmployeeEntity;
import com.example.onlinebankingapp.enums.EmployeeRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {
    boolean existsByEmailAndCitizenId(String email, String citizenId);
    boolean existsByEmail(String email);
    boolean existsByCitizenId(String citizenId);
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<EmployeeEntity> findByPhoneNumber(String phoneNumber);
    Optional<EmployeeEntity> findByEmail(String email);
    List<EmployeeEntity> getAllByRole(String role);
    Page<EmployeeEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
