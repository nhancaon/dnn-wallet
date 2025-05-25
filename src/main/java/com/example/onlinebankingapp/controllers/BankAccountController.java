package com.example.onlinebankingapp.controllers;

import com.example.onlinebankingapp.dtos.requests.BankAccount.BankAccountRequest;
import com.example.onlinebankingapp.dtos.requests.BankAccount.BankAssociationRequest;
import com.example.onlinebankingapp.dtos.responses.Bank.BankListResponse;
import com.example.onlinebankingapp.dtos.responses.Bank.BankResponse;
import com.example.onlinebankingapp.dtos.responses.BankAccount.BankAccountListResponse;
import com.example.onlinebankingapp.dtos.responses.BankAccount.BankAccountsOfPAResponse;
import com.example.onlinebankingapp.dtos.responses.SMSOTPResponse;
import com.example.onlinebankingapp.entities.BankAccountEntity;
import com.example.onlinebankingapp.dtos.responses.BankAccount.BankAccountResponse;
import com.example.onlinebankingapp.dtos.responses.ResponseObject;
import com.example.onlinebankingapp.entities.BankEntity;
import com.example.onlinebankingapp.enums.OTPPurpose;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import com.example.onlinebankingapp.services.BankAccount.BankAccountService;
import com.example.onlinebankingapp.services.VerificationServices.OTPService;
import com.example.onlinebankingapp.services.VerificationServices.SMSService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bankAccounts")
@RequiredArgsConstructor
public class BankAccountController {
    private final BankAccountService bankAccountService;
    private final OTPService otpService;

    @Autowired
    private SMSService smsService;

    // End point for send OTP and check bank account exist
    @PostMapping("/checkBankAccountExist/{bankName}")
    public ResponseEntity<?> checkBankAccountExist(
            @Valid @PathVariable("bankName") String bankName,
            @Valid @RequestBody BankAccountRequest bankAccountRequest
    ) {
        // Link a payment account with a bank account
        BankAccountEntity existingBankAccount = bankAccountService.checkBankAccountExist(bankAccountRequest, bankName);

        if (existingBankAccount == null){
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.NOT_FOUND)
                    .message("This account number in bank account not exist!")
                    .result(null)
                    .build());
        }

        // Send OTP SMS
        String OTP = otpService.generateOTP("",
                existingBankAccount.getPhoneNumber(),
                4,
                OTPPurpose.SMS_BANK_ACCOUNT_INSERT);

        String content = "DO NOT share your OTP to avoid fraud. Your OTP to link your bank account and e-wallet payment account is: "
                + OTP
                + ". This number is only valid for 4 minutes.";
        SMSOTPResponse smsotpResponse = smsService.sendSMSOTP(existingBankAccount.getPhoneNumber(), content);
        if(!smsotpResponse.getStatus().equals("success")){
            throw new AppException(ErrorCode.SMS_OTP_FAIL);
        }

        //return response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Send SMS OTP successfully for bank account association!")
                .result(BankAccountResponse.fromBankAccount(existingBankAccount))
                .build());
    }

    // End point to validate SMS OTP and insertAssociation
    @PostMapping("/insertAssociationToPaymentAccount/{bankName}")
    public ResponseEntity<?> insertAssociation(
            @Valid @PathVariable("bankName") String bankName,
            @Valid @RequestBody BankAssociationRequest bankAssociationRequest
    ) {
        // Link a payment account with a bank account
        BankAccountEntity existingBankAccount = bankAccountService.checkBankAccountExist(
                bankAssociationRequest.getBankAccountActiveRequest(), bankName);

        if (existingBankAccount == null){
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.NOT_FOUND)
                    .message("This account number in bank account not exist!")
                    .result(null)
                    .build());
        }

        // Call service layer to verify OTP via SMS
        boolean isValid = otpService.verifyOTP("",
                existingBankAccount.getPhoneNumber(),
                bankAssociationRequest.getBankAccountActiveRequest().getOtp(),
                OTPPurpose.SMS_BANK_ACCOUNT_INSERT);

        // Return response based on validation result
        if (isValid) {
            BankAccountEntity bankAccountEntityResponse = bankAccountService
                    .insertAssociationToPaymentAccount(
                            existingBankAccount,
                            bankAssociationRequest.getPaymentAccountRequest(),
                            bankName);

            //return data in response
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Insert association between bank account with payment account successfully!")
                    .result(BankAccountResponse.fromBankAccount(bankAccountEntityResponse))
                    .build());
        }
        else {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.UNAUTHORIZED)
                    .message("Invalid OTP")
                    .result("Invalid input OTP: " + bankAssociationRequest.getBankAccountActiveRequest().getOtp())
                    .build());
        }
    }

    // End point to delete bank account association from payment account
    @PostMapping("/deleteAssociationFromPaymentAccount/{bankName}")
    public ResponseEntity<?> deleteAssociation(
            @Valid @PathVariable("bankName") String bankName,
            @Valid @RequestBody BankAssociationRequest bankAssociationRequest
    ) {
        // Link a payment account with a bank account
        BankAccountEntity existingBankAccount = bankAccountService.checkBankAccountExist(
                bankAssociationRequest.getBankAccountActiveRequest(), bankName);

        if (existingBankAccount == null){
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.NOT_FOUND)
                    .message("This account number in bank account not exist!")
                    .result(null)
                    .build());
        }

        BankAccountEntity bankAccountEntityResponse = bankAccountService
                .deleteAssociationFromPaymentAccount(
                        existingBankAccount,
                        bankAssociationRequest.getPaymentAccountRequest(),
                        bankName);

        //return data in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Delete association between bank account with payment account successfully!")
                .result(BankAccountResponse.fromBankAccount(bankAccountEntityResponse))
                .build());
    }

    // End point for get bank account exist -> transfer money
    @GetMapping("/getBankAccountByBankAccountId/{bankAccountId}")
    public ResponseEntity<?> getBankAccountByBankAccountId(
            @Valid @PathVariable("bankAccountId") Long bankAccountId
    ) {
        // Get bank account by bank account number
        BankAccountEntity existingBankAccount = bankAccountService.getBankAccountByBankAccountId(bankAccountId);

        // return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .message("Get bank account successfully")
                .status(HttpStatus.OK)
                .result(BankAccountResponse.fromBankAccount(existingBankAccount))
                .build());
    }

    @GetMapping("/getAllAssociatedBankAccountByCustomerId/{customerId}")
    public ResponseEntity<?> getAllAssociatedBankAccountByCustomerId(
            @Valid @PathVariable("customerId") Long customerId
    ) {
        List<BankAccountEntity> banks = bankAccountService.getAllAssociatedBankAccountByCustomerId(customerId);

        List<BankAccountResponse> bankAccountResponseList = banks.stream()
                .map(BankAccountResponse::fromBankAccount)
                .toList();

        BankAccountListResponse bankAccountListResponse = BankAccountListResponse.builder()
                .bankAccounts(bankAccountResponseList)
                .build();
        return ResponseEntity.ok(ResponseObject.builder()
                .message("Get all associated bank successfully")
                .status(HttpStatus.OK)
                .result(bankAccountListResponse)
                .build());

    }

    @GetMapping("/getByPaymentAccountId/{paymentAccountId}")
    public ResponseEntity<?> getBankAccountsByPaymentAccountId(
            @Valid @PathVariable("paymentAccountId") Long paymentAccountId
    ) {
        List<BankAccountEntity> existingBankAccounts = bankAccountService.getBankAccountsByPaymentAccountId(paymentAccountId);

        List<BankAccountsOfPAResponse> bankAccountResponses = existingBankAccounts.stream()
                .map(BankAccountsOfPAResponse::generateAccountsOfPAResponse)
                .toList();

        // return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .message("Get bank accounts successfully")
                .status(HttpStatus.OK)
                .result(bankAccountResponses)
                .build());
    }

    @GetMapping("/getByBankAccountNumber/{bankAccountNumber}")
    public ResponseEntity<?> getBankAccountsByPaymentAccountId(
            @Valid @PathVariable("bankAccountNumber") String bankAccountNumber
    ) {
        BankAccountEntity existingBankAccount = bankAccountService.getByBankAccountNumber(bankAccountNumber);


        // return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .message("Get bank accounts successfully")
                .status(HttpStatus.OK)
                .result(BankAccountResponse.fromBankAccount(existingBankAccount))
                .build());
    }


    // End point for get bank account exist -> transfer money
    @GetMapping("/getBankAccountByAccountNumber/{bankName}")
    public ResponseEntity<?> getBankAccountByAccountNumber(
            @Valid @PathVariable("bankName") String bankName,
            @Valid @RequestParam String bankAccountNumber
    ) {
        // Get bank account by bank account number
        BankAccountEntity existingBankAccount = bankAccountService.getBankAccountByAccountNumber(bankAccountNumber, bankName);

        Map<String, Object> result = new HashMap<>();
        result.put("account_number", existingBankAccount.getBankAccountNumber());
        result.put("name", existingBankAccount.getName());
        result.put("bank_name", existingBankAccount.getBank().getName());
        result.put("id", existingBankAccount.getId());

        // return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .message("Get bank account for transferring successfully")
                .status(HttpStatus.OK)
                .result(result)
                .build());
    }
}