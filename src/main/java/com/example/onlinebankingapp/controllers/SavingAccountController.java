package com.example.onlinebankingapp.controllers;

import com.example.onlinebankingapp.dtos.requests.SavingAccountRequest;
import com.example.onlinebankingapp.dtos.responses.SMSOTPResponse;
import com.example.onlinebankingapp.dtos.responses.Transaction.TransactionResponse;
import com.example.onlinebankingapp.dtos.responses.TransactionCustomer.TransactionCustomerResponse;
import com.example.onlinebankingapp.entities.*;
import com.example.onlinebankingapp.dtos.responses.ResponseObject;
import com.example.onlinebankingapp.dtos.responses.SavingAccount.SavingAccountListResponse;
import com.example.onlinebankingapp.dtos.responses.SavingAccount.SavingAccountResponse;
import com.example.onlinebankingapp.enums.OTPPurpose;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import com.example.onlinebankingapp.services.PaymentAccount.PaymentAccountService;
import com.example.onlinebankingapp.services.SavingAccount.SavingAccountService;
import com.example.onlinebankingapp.services.Transaction.TransactionService;
import com.example.onlinebankingapp.services.VerificationServices.OTPService;
import com.example.onlinebankingapp.services.VerificationServices.SMSService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/savingAccounts")
@RequiredArgsConstructor
public class SavingAccountController {
    private final SavingAccountService savingAccountService;
    private final PaymentAccountService paymentAccountService;
    private final TransactionService transactionService;
    private final OTPService otpService;

    @Autowired
    private SMSService smsService;

    // Endpoint for inserting a new saving account
    @PostMapping("/insertSavingAccount")
    public ResponseEntity<?> insertSavingAccount(
            @Valid @RequestBody SavingAccountRequest savingAccountRequest
    ){
        // Call service layer to insert a saving account
        SavingAccountEntity savingAccountResponse = savingAccountService.insertSavingAccount(savingAccountRequest);

        // Return response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Insert new saving account successfully. But initial and current amount are 0 until completed SMS OTP verification")
                .result(SavingAccountResponse.fromSavingAccount(savingAccountResponse))
                .build());
    }

    // Endpoint for inserting a new saving account used by employee
    @PostMapping("/insertSavingAccountByEmployee")
    public ResponseEntity<?> insertSavingAccountByEmployee(
            @Valid @RequestBody SavingAccountRequest savingAccountRequest
    ){
        // Call service layer to insert a saving account
        SavingAccountEntity savingAccountResponse = savingAccountService.insertSavingAccountByEmployee(savingAccountRequest);

        // Return response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Insert new saving account successfully by employee")
                .result(SavingAccountResponse.fromSavingAccount(savingAccountResponse))
                .build());
    }

    // End point for recording pending transaction to add money to SA from PA
    // Prior to choose DEFAULT PA as source money
    @PostMapping("/insertTransactionForAddMoneyToSA/{savingAccountId}")
    public ResponseEntity<?> insertTransactionForAddMoneyToSA(
            @Valid @PathVariable("savingAccountId") Long savingAccountId,
            @Valid @RequestBody SavingAccountRequest savingAccountRequest

    ){
        // Call service layer to insert a pending transaction to add money to SA from PA
        TransactionCustomerEntity pendingAddMoneyToSAFromPA = savingAccountService
                .insertTransactionForAddMoneyToSA(savingAccountRequest.getPaymentAccountId(),
                        savingAccountId,
                        savingAccountRequest.getSavingInitialAmount());

        // Return response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Record pending transaction add money to SA from PA successfully. But initial and current amount are 0 until completed SMS OTP verification")
                .result(TransactionCustomerResponse.fromTransactionCustomer(pendingAddMoneyToSAFromPA))
                .build());
    }

    // Endpoint for sending OTP to add money from PA to SA
    // TRANSACTION for ADD_FROM_PA_TO_SA (Trans 7)
    @PostMapping("/sendOTPForAddMoneyToSA/{savingAccountId}")
    public ResponseEntity<?> sendOTPForAddMoneyToSA(
            @Valid @PathVariable("savingAccountId") Long savingAccountId
    ){
        // Find customer phone number by payment account using PA id
        SavingAccountEntity existingSavingAccount = savingAccountService.getSavingAccountById(savingAccountId);
        PaymentAccountEntity existingPaymentAccount = paymentAccountService.getPaymentAccountById(existingSavingAccount.getPaymentAccount().getId());
        String phoneTo = existingPaymentAccount.getCustomer().getPhoneNumber();

        // Send OTP SMS and validate phone number
        String OTP = otpService.generateOTP("",
                phoneTo,
                4,
                OTPPurpose.SMS_SAVING_ACCOUNT_ADD_MONEY);

        String content = "Your OTP is: " + OTP
                + ". The transaction authentication number for the wallet is valid for only 4 minutes. "
                + "DO NOT share your OTP with others to avoid fraud";

        SMSOTPResponse smsotpResponse = smsService.sendSMSOTP(phoneTo, content);
        if(!smsotpResponse.getStatus().equals("success")){
            throw new AppException(ErrorCode.SMS_OTP_FAIL);
        }

        // Return response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Send OTP for adding money to SA from PA successfully")
                .result(OTP)
                .build());
    }

    // Endpoint for verifying SMS OTP for add money to SA from PA
    @PostMapping("/verifyOTPForAddMoneyToSA/{savingAccountId}/{transactionId}")
    public ResponseEntity<?> verifyOTPForAddMoneyToSA(
            @Valid @PathVariable("savingAccountId") Long savingAccountId,
            @Valid @PathVariable("transactionId") Long transactionId,
            @Valid @RequestParam Integer failTime,
            @Valid @RequestParam String otpTransfer
    ) {
        // Find customer phone number by payment account using PA id
        SavingAccountEntity existingSavingAccount = savingAccountService.getSavingAccountById(savingAccountId);
        PaymentAccountEntity existingPaymentAccount = paymentAccountService.getPaymentAccountById(existingSavingAccount.getPaymentAccount().getId());
        CustomerEntity existingCustomer = existingPaymentAccount.getCustomer();

        // Check transaction exist suitable for transfer money
        TransactionEntity existingTransaction = transactionService.getTransactionById(transactionId);

        // Call service layer to verify OTP via SMS
        boolean isValid = otpService.verifyOTP("",
                existingCustomer.getPhoneNumber(),
                otpTransfer,
                OTPPurpose.SMS_SAVING_ACCOUNT_ADD_MONEY);

        // Return response based on validation result
        if (isValid) {
            // Perform the add money operation to specified SA from PA
            SavingAccountEntity savingAccountResponse = savingAccountService
                    .addMoneyToSavingAccount(savingAccountId, existingTransaction);

            // Complete and update transfer money data
            TransactionEntity completeAddMoneyFromPAToSA = transactionService
                    .completeAddMoneyFromPAToSA(existingCustomer, existingTransaction);

            // Build response
            Map<String, Object> addMoneyFromPAToSA = new HashMap<>();
            addMoneyFromPAToSA.put("completeTransaction", TransactionResponse.fromTransaction(completeAddMoneyFromPAToSA));
            addMoneyFromPAToSA.put("savingAccount", SavingAccountResponse.fromSavingAccount(savingAccountResponse));

            // Return a successful response
            return ResponseEntity.ok().body(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Add money from payment account to saving account successfully")
                    .multiResult(addMoneyFromPAToSA)
                    .build());
        }
        else {
            // Check OTP verification 3 fails
            if(failTime == 3){
                // Set transaction to FAIL
                TransactionEntity failTransaction = transactionService
                        .failAddMoneyToSA(existingCustomer, existingTransaction);

                // Build response
                Map<String, Object> failedAddMoneyFromPAToSA = new HashMap<>();
                failedAddMoneyFromPAToSA.put("failTransaction", TransactionResponse.fromTransaction(failTransaction));
                failedAddMoneyFromPAToSA.put("savingAccount", SavingAccountResponse.fromSavingAccount(existingSavingAccount));

                return ResponseEntity.badRequest().body(ResponseObject.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("Verify wrong OTP for transaction exceed 3 times")
                        .result("Fail to add money from PA to SA")
                        .multiResult(failedAddMoneyFromPAToSA)
                        .build());
            }

            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.UNAUTHORIZED)
                    .message("Invalid OTP")
                    .result("Invalid input OTP: " + otpTransfer)
                    .build());
        }
    }

    // Endpoint for withdrawing money from a saving account to associated payment account
    // TRANSACTION for WITHDRAW_FROM_SA_TO_PA (Trans 10)
    @PutMapping("/withdrawSavingAccountToPA/{savingAccountId}")
    public ResponseEntity<?> withdrawSavingAccountToPA(
            @Valid @PathVariable("savingAccountId") Long savingAccountId
    ){
        // Call service layer to withdraw a saving account
        SavingAccountEntity existingSavingAccount = savingAccountService
                .withdrawFromSavingAccount(savingAccountId);

        // Return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Withdraw SA with ID: " + savingAccountId + " to PA with ID: " + existingSavingAccount.getPaymentAccount().getId() + " successfully")
                .result(SavingAccountResponse.fromSavingAccount(existingSavingAccount))
                .build());
    }

    // Endpoint for getting a saving account by its ID
    @GetMapping("/getSavingAccountById/{savingAccountId}")
    public ResponseEntity<?> getSavingAccountById(
            @Valid @PathVariable("savingAccountId") Long savingAccountId
    ){
        // Call service layer to get a saving account
        SavingAccountEntity savingAccount = savingAccountService.getSavingAccountById(savingAccountId);

        // Return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get saving account with ID: " + savingAccountId + " successfully")
                .result(SavingAccountResponse.fromSavingAccount(savingAccount))
                .build());
    }

    // Endpoint for getting all saving accounts of a customer
    @GetMapping("/getSavingAccountsOfCustomer/{customerId}")
    public ResponseEntity<?> getSavingAccountsOfCustomer(
            @Valid @PathVariable("customerId") Long customerId
    ){
        // Call service layer to get all saving accounts of a user
        List<SavingAccountEntity> savingAccountEntityList = savingAccountService.getSavingAccountsOfCustomer(customerId);

        // Build response
        List<SavingAccountResponse> savingAccountResponses = savingAccountEntityList.stream()
                .map(SavingAccountResponse::fromSavingAccount)
                .toList();

        SavingAccountListResponse savingAccountListResponse = SavingAccountListResponse
                .builder()
                .savingAccounts(savingAccountResponses)
                .build();

        // Return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get list of saving accounts linked with PA of customer with ID: " + customerId + " successfully!")
                .result(savingAccountListResponse)
                .build());
    }

    // Endpoint for getting a list of all saving accounts (admin/ staff)
    @GetMapping("/getAllSavingAccounts")
    public ResponseEntity<?> getAllSavingAccounts(){
        // Call service layer to get all saving accounts
        List<SavingAccountEntity> savingAccountEntityList = savingAccountService.getAllSavingAccounts();

        // Build response
        List<SavingAccountResponse> savingAccountResponses = savingAccountEntityList.stream()
                .map(SavingAccountResponse::fromSavingAccount)
                .toList();

        SavingAccountListResponse savingAccountListResponse = SavingAccountListResponse
                .builder()
                .savingAccounts(savingAccountResponses)
                .build();

        // Return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get list of all saving accounts successfully")
                .result(savingAccountListResponse)
                .build());
    }

    // Endpoint for getting a list of all payment accounts with pagination
    @GetMapping("/getPaginationListSavingAccount")
    public ResponseEntity<?> getPaginationListSavingAccount(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String orderedBy,
            @RequestParam(defaultValue = "false") String isAscending,
            @RequestParam(defaultValue = "") String keyword
    ) {
        Boolean isAsc = Boolean.parseBoolean(isAscending);

        SavingAccountListResponse savingAccountPaginated = savingAccountService.
                getPaginationListSavingAccount(page, size, orderedBy, isAsc, keyword);

        // Return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .message("Get list of paginated saving accounts successfully")
                .status(HttpStatus.OK)
                .result(savingAccountPaginated)
                .build());
    }

    // Endpoint for getting a list of all saving accounts linked with a PA
    @GetMapping("/getSavingAccountsOfPaymentAccount/{paymentAccountId}")
    public ResponseEntity<?> getSavingAccountsOfPaymentAccount(
            @Valid @PathVariable("paymentAccountId") Long paymentAccountId
    ){
        // Call service layer to get all saving accounts
        List<SavingAccountEntity> savingAccountEntityList = savingAccountService.getSavingAccountsOfPaymentAccount(paymentAccountId);

        // Build response
        List<SavingAccountResponse> savingAccountResponses = savingAccountEntityList.stream()
                .map(SavingAccountResponse::fromSavingAccount)
                .toList();

        SavingAccountListResponse savingAccountListResponse = SavingAccountListResponse
                .builder()
                .savingAccounts(savingAccountResponses)
                .build();

        // Return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get list of all saving accounts linked with a PA successfully")
                .result(savingAccountListResponse)
                .build());
    }

    // Endpoint for updating a saving account by its ID used by employee
    @PutMapping("/updateSavingAccountByEmployee/{savingAccountId}")
    public ResponseEntity<?> updateSavingAccountByEmployee(
            @Valid @PathVariable("savingAccountId") Long savingAccountId,
            @Valid @RequestBody SavingAccountRequest savingAccountRequest
    ) {
        // Update the saving account details
        SavingAccountEntity savingAccountResponse = savingAccountService
                .updateSavingAccountByEmployee(savingAccountId, savingAccountRequest);

        // Return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Update saving account with ID: " + savingAccountId + "successfully")
                .result(SavingAccountResponse.fromSavingAccount(savingAccountResponse))
                .build());
    }

    // Endpoint for deleting a saving account by its ID
    @DeleteMapping("/deleteSavingAccountById/{savingAccountId}")
    public ResponseEntity<?> deleteSavingAccountById(
            @Valid @PathVariable("savingAccountId") Long savingAccountId
    ){
        // Call service layer to get a saving account
        savingAccountService.deleteSavingAccount(savingAccountId);

        // Return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Delete saving account with ID: " + savingAccountId + " successfully")
                .build());
    }

    // UPDATE
    // Case 1: additional money into Saving Account (mid-period, end of a month, end of term)
    // Case 2: withdraw money earlier than term (mid-period, end of a month)
    // Endpoint for updating ...
    ////////////////////////////////////////
}
