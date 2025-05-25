package com.example.onlinebankingapp.services.Employee;

import com.example.onlinebankingapp.components.JwtTokenUtils;
import com.example.onlinebankingapp.dtos.requests.LoginRequest;
import com.example.onlinebankingapp.dtos.responses.Employee.EmployeeListResponse;
import com.example.onlinebankingapp.dtos.responses.Employee.EmployeeResponse;
import com.example.onlinebankingapp.entities.TokenEmployeeEntity;
import com.example.onlinebankingapp.exceptions.*;
import com.example.onlinebankingapp.dtos.requests.Employee.EmployeeRequest;
import com.example.onlinebankingapp.entities.EmployeeEntity;
import com.example.onlinebankingapp.enums.EmployeeRole;
import com.example.onlinebankingapp.repositories.EmployeeRepository;
import com.example.onlinebankingapp.repositories.TokenEmployeeRepository;
import com.example.onlinebankingapp.services.Token.TokenEmployeeServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.example.onlinebankingapp.utils.ValidationUtils.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtils jwtTokenUtil;
    private final TokenEmployeeRepository tokenEmployeeRepository;
    private final TokenEmployeeServiceImpl tokenEmployeeService;

    @Override
    public String login(
            LoginRequest loginRequest
    ) {
        Optional<EmployeeEntity> optionalEmployee = Optional.empty();
        String subject = null;

        // Check if the email is in a valid format
        if (!isValidEmail(loginRequest.getEmail())){
            throw new AppException(ErrorCode.EMAIL_INVALID);
        }

        // Retrieve employee by email if available
        if (loginRequest.getEmail() != null) {
            optionalEmployee = employeeRepository.findByEmail(loginRequest.getEmail());
            subject = loginRequest.getEmail();
        }

        // Check if the employee exists and the password matches
        EmployeeEntity existingEmployee = optionalEmployee.get();

        if(!passwordEncoder.matches(loginRequest.getPassword(), existingEmployee.getPassword())) {
            throw new AppException(ErrorCode.PASSWORD_INCORRECT);
        }

        // Authenticate user and generate JWT token -> chua lien ket bang token voi Employee
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                subject, loginRequest.getPassword(),
                existingEmployee.getAuthorities()
        );

        authenticationManager.authenticate(authenticationToken);
        return jwtTokenUtil.generateTokenForEmployee(existingEmployee);
    }

    @Override
    public EmployeeEntity insertEmployee(
            EmployeeRequest employeeRequest
    ) {
        String validationMessage = isEmployeeDTOValid(employeeRequest);
        if (!"OK".equals(validationMessage)) {
            throw new DataIntegrityViolationException(validationMessage);  // Handle validation failure
        }

        // Encode password
        String encodedPassword = passwordEncoder.encode("Aaa123456@");

        // Check for existing employees with the same email, citizen ID, or phone number
        if (employeeRepository.existsByEmail(employeeRequest.getEmail())){
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        }
        if (employeeRepository.existsByCitizenId(employeeRequest.getCitizenId())) {
            throw new AppException(ErrorCode.CITIZEN_ID_EXISTS);
        }
        if(employeeRepository.existsByPhoneNumber(employeeRequest.getPhoneNumber())){
            throw new AppException(ErrorCode.PHONE_NUMBER_EXISTS);
        }

        if (employeeRepository.existsByEmailAndCitizenId(employeeRequest.getEmail(), employeeRequest.getCitizenId())) {
            throw new AppException(ErrorCode.USER_EXISTS);
        }

        //Check DoB
        java.sql.Date formatDateOfBirth = new java.sql.Date(employeeRequest.getDateOfBirth().getTime());

        //Check Role
        EmployeeRole parsedEmployeeRole;
        try{
            parsedEmployeeRole = EmployeeRole.valueOf(employeeRequest.getRole());
        }
        catch (Exception e) {
            throw new AppException(ErrorCode.USER_ROLE_INVALID);
        }

        EmployeeEntity newEmployeeEntity = EmployeeEntity.builder()
                .address(employeeRequest.getAddress())
                .citizenId(employeeRequest.getCitizenId())
                .dateOfBirth(formatDateOfBirth)
                .email(employeeRequest.getEmail())
                .name(employeeRequest.getName())
                .password(encodedPassword)
                .phoneNumber(employeeRequest.getPhoneNumber())
                .role(parsedEmployeeRole.toString())
                .build();

        return employeeRepository.save(newEmployeeEntity);
    }

    @Override
    public EmployeeEntity getEmployeeDetailsFromToken(String token) {
        // Check if the token has expired
        if(jwtTokenUtil.isTokenExpired(token)) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        // Extract the subject (phone number or email) from the token
        String subject = jwtTokenUtil.getSubject(token);
        Optional<EmployeeEntity> employee;

        // Try to find the employee by phone number
        employee = employeeRepository.findByPhoneNumber(subject);

        // If employee not found by phone number and subject is a valid email, try finding by email
        if (employee.isEmpty() && isValidEmail(subject)) {
            employee = employeeRepository.findByEmail(subject);

        }
        // Return the employee if found, otherwise throw an exception
        return employee.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public EmployeeEntity getEmployeeDetailsFromRefreshToken(String refreshToken) {
        // Find the token entity corresponding to the refresh token
        TokenEmployeeEntity existingToken = tokenEmployeeRepository.findByRefreshToken(refreshToken);

        // If refresh token not found, throw an exception
        if (existingToken == null) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        // logger.debug("Token found: {}", existingToken.getToken());

        // Retrieve employee details using the token associated with the refresh token
        return getEmployeeDetailsFromToken(existingToken.getToken());
    }

    @Override
    public List<EmployeeEntity> getAllEmployeesByRole(EmployeeRole employeeRole) {
        return employeeRepository.getAllByRole(String.valueOf(employeeRole));
    }

    // Get all employees (admin)
    @Override
    public List<EmployeeEntity> getAllEmployees() {
        return employeeRepository.findAll();
    }

    @Override
    public EmployeeListResponse getPaginationListEmployee(
            Integer page, Integer size, String orderedBy, Boolean isAscending, String keyword
    ) {
        Long totalQuantity;
        Page<EmployeeEntity> employeePage;

        // Get ascending or descending sort
        Sort sort = Boolean.TRUE.equals(isAscending)
                ? Sort.by(orderedBy).ascending()
                : Sort.by(orderedBy).descending();

        try {
            employeePage = employeeRepository.findByNameContainingIgnoreCase(
                    keyword, PageRequest.of(page - 1, size, sort));
            totalQuantity = employeePage.getTotalElements();
        }
        catch (Exception e){
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        List<EmployeeResponse> employeeResponses = employeePage.stream()
                .map(EmployeeResponse::fromEmployeeResponse)
                .toList();

        return EmployeeListResponse.builder()
                .employeeResponses(employeeResponses)
                .totalQuantity(totalQuantity)
                .build();
    }

    // Get employee by id (admin)
    @Override
    public EmployeeEntity getEmployeeById(
            long employeeId
    ) {
        // Find employee by ID
        Optional<EmployeeEntity> optionalEmployee = employeeRepository.findById(employeeId);

        // If employee exists, return it, otherwise throw an exception
        if(optionalEmployee.isPresent()) {
            return optionalEmployee.get();
        }
        throw new AppException(ErrorCode.USER_NOT_FOUND);
    }

    // Update employee's profile
    @Override
    public EmployeeEntity updateEmployeeProfile(
            long employeeId,
            EmployeeRequest employeeRequest
    ) {
        String validationMessage = isEmployeeDTOValid(employeeRequest);
        if (!"OK".equals(validationMessage)) {
            throw new DataIntegrityViolationException(validationMessage);  // Handle validation failure
        }

        EmployeeEntity updatedEmployeeEntity = getEmployeeById(employeeId);
        if(updatedEmployeeEntity == null) {
            // Employee not found, throw an exception
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        //Check DoB
        java.sql.Date formatDateOfBirth = new java.sql.Date(employeeRequest.getDateOfBirth().getTime());

        updatedEmployeeEntity.setAddress(employeeRequest.getAddress());
        updatedEmployeeEntity.setDateOfBirth(formatDateOfBirth);
        updatedEmployeeEntity.setEmail(employeeRequest.getEmail());
        updatedEmployeeEntity.setPhoneNumber(employeeRequest.getPhoneNumber());
        updatedEmployeeEntity.setRole(employeeRequest.getRole());
        // Encode and set the new password
        if(employeeRequest.getPassword() != null) {
            updatedEmployeeEntity.setPassword(passwordEncoder.encode(employeeRequest.getPassword()));
        }

        return employeeRepository.save(updatedEmployeeEntity);
    }

    private String isEmployeeDTOValid(
            EmployeeRequest employeeRequest
    ){
        String newAddress = employeeRequest.getAddress();
        Date newDateOfBirth = employeeRequest.getDateOfBirth();
        String newEmail = employeeRequest.getEmail();
        String newPhoneNumber = employeeRequest.getPhoneNumber();
        String newPassword = employeeRequest.getPassword();

        if (newAddress.length() > 100 || newAddress.length() < 5) {
            return "Address is invalid";
        }

        if (!isValidDateOfBirth(newDateOfBirth)) {
            return "Invalid date of birth";
        }

        if (!isValidEmail(newEmail)) {
            return "Invalid email address";
        }

        if (!isValidPhoneNumber(newPhoneNumber)) {
            return "Invalid Vietnamese phone number";
        }
        System.out.println("Passwprd: " + employeeRequest.getPassword());

        if(employeeRequest.getPassword() != null && !isValidPassword(newPassword)){
            return "Password must satisfy the following conditions:\n" +
                    "The length must be from 8 to 20 characters\n" +
                    "Contains at least 01 digit, 01 letter and 01 special character";
        }

        return "OK";
    }

    // Permanent delete employee by id
    @Override
    public void deleteEmployeeById(
            long employeeId
    ) {
        EmployeeEntity deleteEmployeeEntity = getEmployeeById(employeeId);
        tokenEmployeeService.deleteTokenByEmployeeId(deleteEmployeeEntity);
        employeeRepository.delete(deleteEmployeeEntity);
    }
}
