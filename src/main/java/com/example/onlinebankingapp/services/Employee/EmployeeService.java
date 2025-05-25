package com.example.onlinebankingapp.services.Employee;

import com.example.onlinebankingapp.dtos.requests.LoginRequest;
import com.example.onlinebankingapp.dtos.requests.Employee.EmployeeRequest;
import com.example.onlinebankingapp.dtos.responses.Employee.EmployeeListResponse;
import com.example.onlinebankingapp.entities.EmployeeEntity;
import com.example.onlinebankingapp.enums.EmployeeRole;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface EmployeeService {
    String login(LoginRequest loginRequest);
    EmployeeEntity insertEmployee(EmployeeRequest employeeRequest);
    EmployeeEntity getEmployeeDetailsFromToken(String token);
    EmployeeEntity getEmployeeDetailsFromRefreshToken(String refreshToken);
    List<EmployeeEntity> getAllEmployeesByRole(EmployeeRole employeeRole);
    List<EmployeeEntity> getAllEmployees();
    EmployeeListResponse getPaginationListEmployee(Integer page, Integer size, String orderedBy, Boolean isAscending, String keyword);
    EmployeeEntity getEmployeeById(long employeeId);
    EmployeeEntity updateEmployeeProfile(long employeeId, EmployeeRequest employeeRequest);
    void deleteEmployeeById(long employeeId);
}
