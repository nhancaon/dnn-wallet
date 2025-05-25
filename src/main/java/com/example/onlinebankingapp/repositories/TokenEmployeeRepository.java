package com.example.onlinebankingapp.repositories;

import com.example.onlinebankingapp.entities.EmployeeEntity;
import com.example.onlinebankingapp.entities.TokenEmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TokenEmployeeRepository extends JpaRepository<TokenEmployeeEntity, Long> {
    // Find tokens associated with a specific employee
    List<TokenEmployeeEntity> findByEmployee(EmployeeEntity employee);

    // Find a token by its token string
    TokenEmployeeEntity findByToken(String token);

    // Find a token by its refresh token string
    TokenEmployeeEntity findByRefreshToken(String refreshToken);
}
