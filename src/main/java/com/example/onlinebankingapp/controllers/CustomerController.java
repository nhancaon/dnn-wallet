package com.example.onlinebankingapp.controllers;

import com.example.onlinebankingapp.dtos.requests.Customer.*;
import com.example.onlinebankingapp.dtos.requests.LoginRequest;
import com.example.onlinebankingapp.dtos.requests.RefreshTokenRequest;
import com.example.onlinebankingapp.dtos.responses.SMSOTPResponse;
import com.example.onlinebankingapp.entities.CustomerEntity;
import com.example.onlinebankingapp.entities.TokenCustomerEntity;
import com.example.onlinebankingapp.dtos.responses.Customer.CustomerListResponse;
import com.example.onlinebankingapp.dtos.responses.Customer.LoginResponse;
import com.example.onlinebankingapp.dtos.responses.ResponseObject;
import com.example.onlinebankingapp.dtos.responses.Customer.CustomerResponse;
import com.example.onlinebankingapp.enums.OTPPurpose;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import com.example.onlinebankingapp.services.Customer.CustomerService;
import com.example.onlinebankingapp.services.PaymentAccount.PaymentAccountService;
import com.example.onlinebankingapp.services.Token.TokenCustomerService;
import com.example.onlinebankingapp.services.VerificationServices.EmailService;
import com.example.onlinebankingapp.services.VerificationServices.OTPService;
import com.example.onlinebankingapp.services.VerificationServices.SMSService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;
    private final PaymentAccountService paymentAccountService;
    private final TokenCustomerService tokenCustomerService;
    private final OTPService otpService;
    private final EmailService emailService;

    @Autowired
    private SMSService smsService;

    // Fetch data for viewing when sign-up (ID card info) via clientSession
    @GetMapping("/getDataSignup")
    public ResponseEntity<?> getDataSignup(
            @Valid @RequestParam String clientSession
    ) {
        CustomerEntity customerEntityResponse = customerService.getCustomerByClientSession(clientSession);

        //return result in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get customer information when sign up via client session: " + clientSession + " successfully")
                .result(CustomerResponse.fromCustomerResponse(customerEntityResponse))
                .build());
    }

    // Insert a new customer into the system/ register account
    @PostMapping("/insertCustomer")
    public ResponseEntity<?> insertCustomer(
            @Valid @RequestBody CustomerRequest customerRequest
    ) {
        // Send OTP SMS and validate phone number
        String OTP = otpService.generateOTP("",
                customerRequest.getPhoneNumber(),
                4,
                OTPPurpose.SMS_CUSTOMER_SIGN_UP);
        String content = "Welcome to DNN e-wallet! Your OTP for account activation is: " + OTP + ". This number is only valid for 4 minutes.";

        SMSOTPResponse smsotpResponse = smsService.sendSMSOTP(customerRequest.getPhoneNumber(), content);
        if(!smsotpResponse.getStatus().equals("success")){
            throw new AppException(ErrorCode.SMS_OTP_FAIL);
        }

        // Insert a customer
        CustomerEntity customerEntityResponse = customerService.insertCustomer(customerRequest);

        // Return data in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Insert successfully but INACTIVE. So must verify SMS OTP to login")
                .result(CustomerResponse.fromCustomerResponse(customerEntityResponse))
                .build());
    }

    // Require verify OTP to enable account
    @PostMapping("/insertCustomer/activeCustomerAccount")
    public ResponseEntity<?> activeCustomerAccount(
            @Valid @RequestBody CustomerActiveRequest customerActiveRequest
    ) {
        // Call service layer to verify OTP via SMS
        boolean isValid = otpService.verifyOTP("",
                customerActiveRequest.getPhoneNumber(),
                customerActiveRequest.getOtp(),
                OTPPurpose.SMS_CUSTOMER_SIGN_UP);

        // Return response based on validation result
        if (isValid) {
            // Enable customer account after verify OTP successfully
            CustomerEntity customerEntityResponse = customerService.activeCustomerAccount(customerActiveRequest);

            // Create first default payment account using their phone number as account number
            paymentAccountService.insertPaymentAccount(customerActiveRequest.getPhoneNumber(), customerEntityResponse.getId());

            //return data in response
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Enable the customer account successfully. Now can login")
                    .result(CustomerResponse.fromCustomerResponse(customerEntityResponse))
                    .build());
        }
        else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid OTP");
        }
    }

    // This endpoint perform login action
    @PostMapping("/login")
    public ResponseEntity<?> login (
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request
    ) {
        // Authenticate login using email, password in DTO
        String token = customerService.login(loginRequest);

        // Build response and jwt token
        String customerAgent = request.getHeader("Customer-Agent");
        CustomerEntity customer = customerService.getCustomerDetailsFromToken(token);
        TokenCustomerEntity jwtToken = tokenCustomerService.addTokenForCustomer(customer, token, isMobileDevice(customerAgent));

        // Build response
        LoginResponse loginResponse = LoginResponse.builder()
                .token(jwtToken.getToken())
                .tokenType(jwtToken.getTokenType())
                .refreshToken(jwtToken.getRefreshToken())
                .fullName(customer.getName())
                .id(customer.getId())
                .build();

        //return response with jwt token and user info
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Customer login successfully")
                .result(loginResponse)
                .build());
    }

    // Refresh an expired access token using a valid refresh token
    @PostMapping("/refreshToken")
    public ResponseEntity<?> refreshToken (
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest
    ) throws Exception {
        // Retrieve customer details from the refresh token
        CustomerEntity customer = customerService.getCustomerDetailsFromRefreshToken(refreshTokenRequest.getRefreshToken());

        // Refresh the access token
        TokenCustomerEntity jwtToken = tokenCustomerService.refreshTokenForCustomer(refreshTokenRequest.getRefreshToken(), customer);

        // Return a successful response with the new access token
        LoginResponse loginResponse = LoginResponse.builder()
                .token(jwtToken.getToken())
                .tokenType(jwtToken.getTokenType())
                .refreshToken(jwtToken.getRefreshToken())
                .fullName(customer.getName())
                .id(customer.getId())
                .build();

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Refresh token for customer successfully")
                .result(loginResponse)
                .build());
    }

    // Get all customers
    @GetMapping("/getAllCustomers")
    public ResponseEntity<?> getAllCustomers() {
        // Retrieve all customers
        List<CustomerEntity> customerEntities = customerService.getAllCustomers();

        List<CustomerResponse> customerResponses = customerEntities.stream()
                .map(CustomerResponse::fromCustomerResponse)
                .toList();

        CustomerListResponse customerListResponse = CustomerListResponse.builder()
                .customerResponses(customerResponses)
                .build();

        //return result in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get all customers successfully")
                .result(customerListResponse)
                .build());
    }

    // Get all paginated customers
    @GetMapping("/getPaginationListCustomer")
    public ResponseEntity<?> getPaginationListCustomer(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String orderedBy,
            @RequestParam(defaultValue = "false") String isAscending,
            @RequestParam(defaultValue = "") String keyword
    ) {
        Boolean isAsc = Boolean.parseBoolean(isAscending);

        CustomerListResponse customerPaginated = customerService
                .getPaginationListCustomer(page, size, orderedBy, isAsc, keyword);

        //return result in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get all paginated customers successfully")
                .result(customerPaginated)
                .build());
    }

    // Get a customer by id
    @GetMapping("/getById/{customerId}")
    public ResponseEntity<?> getCustomerById(
            @Valid @PathVariable("customerId") long customerId
    ) {
        CustomerEntity customerEntityResponse = customerService.getCustomerById(customerId);

        //return result in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get customer information with ID: " + customerId + " successfully")
                .result(CustomerResponse.fromCustomerResponse(customerEntityResponse))
                .build());
    }

    // Check if it's a mobile device
    public static boolean isMobileDevice(String customerAgent) {
        // Kiểm tra Customer-Agent header để xác định thiết bị di động
        // Ví dụ đơn giản:
        return customerAgent != null && customerAgent.toLowerCase().contains("mobile");
    }

    // Request to update customer profile (Email unverified)
    @PostMapping("/updateCustomerProfile/sendEmailOtp")
    public ResponseEntity<?> sendEmailOtp(
            @Valid @RequestBody CustomerRequest customerRequest
    ) {
        // Call service layer to generate and send otp
        String otp = otpService.generateOTP(customerRequest.getEmail(),
                "",
                5,
                OTPPurpose.EMAIL_CUSTOMER_UPDATE_PROFILE);
        String subject = "OTP for Profile Update";

        // Create email body for personal information update notification
        String emailBody = "Dear Customer,\n\n" +
                "DNN E-wallet has generated an OTP for your recent update personal information request. Please use the OTP provided below to complete your modification.\n\n" +
                "Your OTP is: " + otp + "\n\n" +
                "Important Information:\n" +
                "- This OTP is only valid for 5 minutes.\n" +
                "- Do not share this OTP with anyone.\n" +
                "- If you did not initiate this personal information update, please contact our customer support immediately.\n\n" +
                "Thank you for choosing DNN E-wallet. We are committed to ensuring the security and privacy of your financial information.\n\n" +
                "Best regards,\n" +
                "DNN E-wallet Customer Support Team";

        // Activate email sending function
        emailService.sendEmail(customerRequest.getEmail(), subject, emailBody);

        // Return response indicating OTP was sent
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("OTP sent to email for update profile!")
                .result("OTP has been sent: " + otp)
                .build());
    }

    // Email verified to update customer profile (email/ PIN number)
    @PutMapping("/updateCustomerProfile/{customerId}/{otp}")
    public ResponseEntity<?> updateCustomerProfile(
            @Valid @PathVariable("customerId") long customerId,
            @Valid @PathVariable("otp") String otp,
            @Valid @RequestBody CustomerRequest customerRequest
    ) {
        // Verify the OTP
        boolean isValid = otpService.verifyOTP(customerRequest.getEmail(),
                "",
                otp,
                OTPPurpose.EMAIL_CUSTOMER_UPDATE_PROFILE);
        if (!isValid) {
            return ResponseEntity.badRequest().body("Invalid email OTP for verification to update profile");
        }

        // Update the customer's profile
        CustomerEntity customerEntityResponse = customerService.updateCustomerProfile(customerId, customerRequest);

        // Return updated customer information in the response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Update customer email/ PIN number successfully")
                .result(CustomerResponse.fromCustomerResponse(customerEntityResponse))
                .build());
    }

    // Update field PASSWORD of a customer
    @PutMapping("/changePassword/{customerId}")
    public ResponseEntity<?> changePassword (
            @PathVariable Long customerId,
            @Valid @RequestBody ChangePasswordCustomerRequest customerDTO
    ) {
        // Change user password
        customerService.changePassword(customerId, customerDTO);

        // Return result in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Change password successfully")
                .result(customerDTO)
                .build());
    }

    // Do not hard-delete (permanent) -> soft-delete (inactive the account)
    // Only employee can use this
    // But in real-life end-users can also deactivate their accounts permanently
    @PutMapping("/deleteCustomer/{customerId}")
    public ResponseEntity<?> deleteCustomer(
            @Valid @PathVariable("customerId") long customerId
    ) {
        // Soft-delete the customer's profile
        CustomerEntity customerEntityResponse = customerService.deleteCustomer(customerId);

        // Return deleted customer information in the response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Delete customer account with ID: " + customerId + " successfully")
                .result(CustomerResponse.fromCustomerResponse(customerEntityResponse))
                .build());
    }

    // Endpoint for verifying pin number
    @PostMapping("/verifyPinNumber/{customerId}")
    public ResponseEntity<?> verifyPinNumber (
            @PathVariable Long customerId,
            @Valid @RequestParam String pinNumber
    ) {
        boolean result = customerService.verifyPinNumber(customerId, pinNumber);

        if (result) {
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Verify password successfully")
                    .result(Map.of("isVerified", true))
                    .build());
        } else {
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Verify password fail")
                    .result(Map.of("isVerified", false))
                    .build());
        }
    }

    // Endpoint for updating customer data by employee (admin/ staff)
    @PutMapping("/updateCustomerByEmployee/{customerId}")
    public ResponseEntity<?> updateCustomerByEmployee (
            @Valid @PathVariable("customerId") Long customerId,
            @Valid @RequestBody CustomerRequest customerRequest
    ) {
        // Update customer profile by employee
        CustomerEntity updatedCustomerProfile = customerService.updateCustomerByEmployee(customerId, customerRequest);

        // Return updated customer information in the response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Update customer by employee successfully")
                .result(CustomerResponse.fromCustomerResponse(updatedCustomerProfile))
                .build());
    }
}
