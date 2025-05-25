package com.example.onlinebankingapp.controllers;

import com.example.onlinebankingapp.dtos.requests.Employee.EmployeeRequest;
import com.example.onlinebankingapp.dtos.requests.LoginRequest;
import com.example.onlinebankingapp.dtos.requests.RefreshTokenRequest;
import com.example.onlinebankingapp.entities.EmployeeEntity;
import com.example.onlinebankingapp.entities.TokenEmployeeEntity;
import com.example.onlinebankingapp.dtos.responses.Customer.LoginResponse;
import com.example.onlinebankingapp.dtos.responses.Employee.EmployeeListResponse;
import com.example.onlinebankingapp.dtos.responses.Employee.EmployeeResponse;
import com.example.onlinebankingapp.dtos.responses.ResponseObject;
import com.example.onlinebankingapp.services.Employee.EmployeeServiceImpl;
import com.example.onlinebankingapp.services.Token.TokenEmployeeService;
import com.example.onlinebankingapp.services.VerificationServices.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeServiceImpl employeeService;
    private final TokenEmployeeService tokenEmployeeService;
    private final EmailService emailService;

    // Insert a new employee into the system/ register account (admin)
    @PostMapping("/insertEmployee")
    public ResponseEntity<?> insertEmployee(
            @Valid @RequestBody EmployeeRequest employeeRequest
    ) {
        // Insert a customer
        EmployeeEntity employeeEntityResponse = employeeService.insertEmployee(employeeRequest);

        // Set subject to send login information to employees
        String subject = "Welcome to DNN E-wallet family";
        // Create email body for personal information update notification
        String emailBody = "Dear Mr./Mrs. " + employeeRequest.getName() + ",\n\n" +
                "Welcome to our DNN E-wallet. It would be an honour for us to work with you. " +
                "You have been added by our Administrator as role: " + employeeRequest.getRole().toUpperCase() + ".\n\n" +
                "Your access system account is:\n" +
                "- Your own email: " + employeeRequest.getEmail() + "\n" +
                "- Default password: Aaa123456@\n\n" +
                "Important Information:\n" +
                "- Do not share this password with anyone.\n" +
                "Thank you for choosing DNN E-wallet. We are committed to ensuring the security and privacy of your information.\n\n" +
                "Best regards,\n" +
                "DNN E-wallet Human Resource Management Team";

        // Activate email sending function
        emailService.sendEmail(employeeRequest.getEmail(), subject, emailBody);

        // Return data in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Insert employee successfully")
                .result(EmployeeResponse.fromEmployeeResponse(employeeEntityResponse))
                .build());
    }

    // Login as employee (admin/ staff)
    @PostMapping("/login")
    public ResponseEntity<?> login (
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request
    ) {
        // Authenticate login using email, password in DTO
        String token = employeeService.login(loginRequest);

        // Build response and jwt token
        String employeeAgent = request.getHeader("Employee-Agent");
        EmployeeEntity employee = employeeService.getEmployeeDetailsFromToken(token);
        TokenEmployeeEntity jwtToken = tokenEmployeeService.addTokenForEmployee(employee, token, isMobileDevice(employeeAgent));

        // Build response
        LoginResponse loginResponse = LoginResponse.builder()
                .token(jwtToken.getToken())
                .tokenType(jwtToken.getTokenType())
                .refreshToken(jwtToken.getRefreshToken())
                .fullName(employee.getName())
                .id(employee.getId())
                .role(employee.getRole())
                .build();

        //return response with jwt token and user info
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Employee login successfully")
                .result(loginResponse)
                .build());
    }

    private boolean isMobileDevice(String employeeAgent) {
        // Kiểm tra Employee-Agent header để xác định thiết bị di động
        // Ví dụ đơn giản:
        return employeeAgent != null && employeeAgent.toLowerCase().contains("mobile");
    }

    // Refresh an expired access token using a valid refresh token
    @PostMapping("/refreshToken")
    public ResponseEntity<?> refreshToken (
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest
    ) throws Exception {
        // Retrieve employee details from the refresh token
        EmployeeEntity employee = employeeService.getEmployeeDetailsFromRefreshToken(refreshTokenRequest.getRefreshToken());

        // Refresh the access token
        TokenEmployeeEntity jwtToken = tokenEmployeeService.refreshTokenForEmployee(refreshTokenRequest.getRefreshToken(), employee);

        // Return a successful response with the new access token
        LoginResponse loginResponse = LoginResponse.builder()
                .token(jwtToken.getToken())
                .tokenType(jwtToken.getTokenType())
                .refreshToken(jwtToken.getRefreshToken())
                .fullName(employee.getName())
                .id(employee.getId())
                .role(employee.getRole())
                .build();

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Refresh token for employee successfully")
                .result(loginResponse)
                .build());
    }

    // Get all employee (admin)
    @GetMapping("/getAllEmployees")
    public ResponseEntity<?> getAllEmployees() {
        // Retrieve all customers
        List<EmployeeEntity> employeeEntities = employeeService.getAllEmployees();

        List<EmployeeResponse> employeeResponses = employeeEntities.stream()
                .map(EmployeeResponse::fromEmployeeResponse)
                .toList();

        EmployeeListResponse employeeListResponse = EmployeeListResponse.builder()
                .employeeResponses(employeeResponses)
                .build();

        //return result in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get all employees successfully")
                .result(employeeListResponse)
                .build());
    }

    // Get all employee paginated (admin)
    @GetMapping("/getPaginationListEmployee")
    public ResponseEntity<?> getPaginationListEmployee(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String orderedBy,
            @RequestParam(defaultValue = "false") String isAscending,
            @RequestParam(defaultValue = "") String keyword
    ) {
        Boolean isAsc = Boolean.parseBoolean(isAscending);

        EmployeeListResponse employeePaginated = employeeService
                .getPaginationListEmployee(page, size, orderedBy, isAsc, keyword);

        //return result in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get all paginated employees successfully")
                .result(employeePaginated)
                .build());
    }

    // Get employee by id (admin)
    @GetMapping("/getById/{employeeId}")
    public ResponseEntity<?> getEmployeeById(
            @Valid @PathVariable("employeeId") long employeeId
    ) {
        EmployeeEntity employeeEntityResponse = employeeService.getEmployeeById(employeeId);

        //return result in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get employee information with ID: " + employeeId + " successfully")
                .result(EmployeeResponse.fromEmployeeResponse(employeeEntityResponse))
                .build());
    }

    // Update employee information (admin) (address/ dateOfBirth/ email/ phoneNumber/ password)
    @PutMapping("/updateEmployeeProfile/{employeeId}")
    public ResponseEntity<?> updateEmployeeProfile(
            @Valid @PathVariable("employeeId") long employeeId,
            @Valid @RequestBody EmployeeRequest employeeRequest
    ) {
        // Update the employee's profile
        EmployeeEntity employeeEntityResponse = employeeService.updateEmployeeProfile(employeeId, employeeRequest);

        // Return updated customer information in the response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Update employee successfully")
                .result(EmployeeResponse.fromEmployeeResponse(employeeEntityResponse))
                .build());
    }

    // Delete employee by id
    @DeleteMapping("/deleteEmployee/{employeeId}")
    public ResponseEntity<?> deleteEmployee(
            @Valid @PathVariable("employeeId") long employeeId
    ) {
        // Hard-delete employee's profile
        employeeService.deleteEmployeeById(employeeId);

        // Return deleted customer information in the response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Delete employee with ID: " + employeeId + " successfully")
                .build());
    }
}
