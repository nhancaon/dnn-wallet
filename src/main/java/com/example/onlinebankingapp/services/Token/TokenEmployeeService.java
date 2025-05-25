package com.example.onlinebankingapp.services.Token;

import com.example.onlinebankingapp.entities.EmployeeEntity;
import com.example.onlinebankingapp.entities.TokenEmployeeEntity;
import org.springframework.stereotype.Service;

@Service
public interface TokenEmployeeService {
    TokenEmployeeEntity addTokenForEmployee(EmployeeEntity employee, String token, boolean isMobileDevice);
    TokenEmployeeEntity refreshTokenForEmployee(String refreshToken, EmployeeEntity employee);
    void deleteTokenByEmployeeId(EmployeeEntity employee);
}
