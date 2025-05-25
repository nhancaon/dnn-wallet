package com.example.onlinebankingapp.controllers;

import com.example.onlinebankingapp.dtos.requests.LoginRequest;
import com.example.onlinebankingapp.dtos.responses.Customer.CustomerResponse;
import com.example.onlinebankingapp.dtos.responses.Customer.LoginResponse;
import com.example.onlinebankingapp.dtos.responses.IDCardExtraction.AddFileResponse;
import com.example.onlinebankingapp.dtos.responses.ResponseObject;
import com.example.onlinebankingapp.entities.CustomerEntity;
import com.example.onlinebankingapp.entities.TokenCustomerEntity;
import com.example.onlinebankingapp.repositories.CustomerRepository;
import com.example.onlinebankingapp.services.Customer.CustomerService;
import com.example.onlinebankingapp.services.IDCardExtraction.IDCardExtractionService;
import com.example.onlinebankingapp.services.Token.TokenCustomerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/faceRecognition")
@RequiredArgsConstructor
public class FaceRecognitionController {
    private final IDCardExtractionService idCardExtractionService;
    private final CustomerService customerService;
    private final TokenCustomerService tokenCustomerService;

    // Endpoint for login using FACE ID
    // Use clientSession (use to find existingCustomer with clientSession)
    @PostMapping("/uploadFile/loginFaceId")
    public ResponseEntity<?> loginFaceId(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("clientSession") String clientSession,
            HttpServletRequest request
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is required.");
        }

        AddFileResponse addFileResponse = idCardExtractionService.addFile(file, title, description, clientSession);

        // Find field loginStatus in Object exist
        Boolean loginStatus = (Boolean) addFileResponse.getObject().get("loginStatus");
        if (Boolean.TRUE.equals(loginStatus)) {
            addFileResponse.getObject().remove("loginStatus");

            CustomerEntity existingCustomer = customerService.getCustomerByClientSession(clientSession);
            if (existingCustomer == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseObject.builder()
                                .status(HttpStatus.UNAUTHORIZED)
                                .message("Customer not found or session invalid.")
                                .build());
            }

            // Generate JWT directly for FaceID login
            String token = customerService.loginFaceId(existingCustomer.getEmail());

            String customerAgent = request.getHeader("Customer-Agent");
            TokenCustomerEntity jwtToken = tokenCustomerService.addTokenForCustomer(existingCustomer, token, CustomerController.isMobileDevice(customerAgent));

            LoginResponse loginResponse = LoginResponse.builder()
                    .token(jwtToken.getToken())
                    .tokenType(jwtToken.getTokenType())
                    .refreshToken(jwtToken.getRefreshToken())
                    .fullName(existingCustomer.getName())
                    .id(existingCustomer.getId())
                    .build();

            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Customer login successfully")
                    .result(loginResponse)
                    .build());
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseObject.builder()
                .status(HttpStatus.UNAUTHORIZED)
                .message("Invalid FACE ID for login")
                .result(addFileResponse)
                .build());
    }

    // Endpoint for check face for transaction TRANSFER_MONEY >= 10M
    @GetMapping("/uploadFile/transferFaceId")
    public ResponseEntity<?> transferFaceId(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("clientSession") String clientSession
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is required.");
        }

        AddFileResponse addFileResponse = idCardExtractionService.addFile(file, title, description, clientSession);

        // Find field allowTransfer in Object exist
        Boolean allowTransfer = (Boolean) addFileResponse.getObject().get("allowTransfer");
        if (Boolean.TRUE.equals(allowTransfer)) {
            addFileResponse.getObject().remove("allowTransfer");

            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Face ID check for transaction exceed 10M successfully")
                    .result("SUCCESS")
                    .build());
        }
        else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResponseObject.builder()
                    .status(HttpStatus.UNAUTHORIZED)
                    .message("Invalid FACE ID for transaction exceed 10M")
                    .result(addFileResponse)
                    .build());
        }
    }
}
