package com.example.onlinebankingapp.controllers;

import com.example.onlinebankingapp.dtos.requests.PaymentAccount.MoneyFlowRequest;
import com.example.onlinebankingapp.dtos.requests.PaymentAccount.PaymentAccountRequest;
import com.example.onlinebankingapp.dtos.responses.SMSOTPResponse;
import com.example.onlinebankingapp.dtos.responses.Transaction.TransactionResponse;
import com.example.onlinebankingapp.dtos.responses.TransactionCustomer.TransactionCustomerResponse;
import com.example.onlinebankingapp.entities.*;
import com.example.onlinebankingapp.dtos.responses.Customer.CustomerResponse;
import com.example.onlinebankingapp.dtos.responses.PaymentAccount.PaymentAccountListResponse;
import com.example.onlinebankingapp.dtos.responses.PaymentAccount.PaymentAccountResponse;
import com.example.onlinebankingapp.dtos.responses.ResponseObject;
import com.example.onlinebankingapp.enums.OTPPurpose;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import com.example.onlinebankingapp.services.BankAccount.BankAccountService;
import com.example.onlinebankingapp.services.PaymentAccount.PaymentAccountService;
import com.example.onlinebankingapp.services.Transaction.TransactionService;
import com.example.onlinebankingapp.services.VerificationServices.OTPService;
import com.example.onlinebankingapp.services.VerificationServices.SMSService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/paymentAccounts")
@RequiredArgsConstructor
public class PaymentAccountController {
    private final PaymentAccountService paymentAccountService;
    private final BankAccountService bankAccountService;
    private final TransactionService transactionService;
    private final OTPService otpService;

    @Autowired
    private SMSService smsService;

    // End point for inserting a payment account -> already has DEFAULT
    @PostMapping("/insertPaymentAccount/{customerId}")
    public ResponseEntity<?> insertPaymentAccount(
            @Valid @RequestBody PaymentAccountRequest paymentAccountRequest,
            @Valid @PathVariable("customerId") Long customerId
    ) {
        // Insert a payment account but not using phone no. as account number
        PaymentAccountEntity paymentAccountResponse = paymentAccountService
                .insertPaymentAccount(paymentAccountRequest.getAccountNumber(), customerId);

        //return response
        return ResponseEntity.ok(
                ResponseObject.builder()
                        .result(PaymentAccountResponse.fromPaymentAccount(paymentAccountResponse))
                        .message("Insert an Payment Account successfully!")
                        .status(HttpStatus.OK)
                        .build());
    }

    // End point for setting an account as the default account
    @PostMapping("/setDefaultAccount/{customerId}")
    public ResponseEntity<?> setDefaultPaymentAccount(
            @Valid @PathVariable("customerId") Long customerId,
            @Valid @RequestBody PaymentAccountRequest paymentAccountRequest

    ) {
        //set the requested account as default account
        paymentAccountService.setDefaultPaymentAccount(customerId, paymentAccountRequest.getAccountNumber());

        //return response
        return ResponseEntity.ok(
                ResponseObject.builder()
                        .status(HttpStatus.OK)
                        .message("Set default Payment Account successfully!")
                        .result("Payment account number: " + paymentAccountRequest.getAccountNumber() + " is DEFAULT")
                        .build());
    }

    // Getting all the payment accounts
    @GetMapping("/getAllPaymentAccounts")
    public ResponseEntity<?> getAllPaymentAccounts() {
        //get all the payment accounts
        List<PaymentAccountEntity> paymentAccounts = paymentAccountService.getAllPaymentAccounts();

        List<PaymentAccountResponse> paymentAccountResponse = paymentAccounts.stream()
                .map(PaymentAccountResponse::fromPaymentAccount)
                .toList();

        PaymentAccountListResponse paymentAccountListResponse = PaymentAccountListResponse
                .builder()
                .paymentAccounts(paymentAccountResponse)
                .build();

        //return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .message("Get Payment Account list successfully")
                .status(HttpStatus.OK)
                .result(paymentAccountListResponse)
                .build());
    }

    // Getting all the payment accounts with pagination
    @GetMapping("/getPaginationListPaymentAccount")
    public ResponseEntity<?> getPaginationListPaymentAccount(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String orderedBy,
            @RequestParam(defaultValue = "false") String isAscending,
            @RequestParam(defaultValue = "") String keyword
    ) {
        Boolean isAsc = Boolean.parseBoolean(isAscending);

        PaymentAccountListResponse paymentAccountPaginated = paymentAccountService.
                getPaginationListPaymentAccount(page, size, orderedBy, isAsc, keyword);

        //return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .message("Get list of paginated payment accounts successfully")
                .status(HttpStatus.OK)
                .result(paymentAccountPaginated)
                .build());
    }

    // Getting the default payment account of a customer
    @GetMapping("/getDefaultAccount/{customerId}")
    public ResponseEntity<?> getDefaultPaymentAccount(
            @Valid @PathVariable("customerId") Long customerId
    ) {
        //get the default account
        PaymentAccountEntity paymentAccount = paymentAccountService.getDefaultPaymentAccount(customerId);

        //return the account in response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .message("Get Payment Account list successfully")
                .status(HttpStatus.OK)
                .result(PaymentAccountResponse.fromPaymentAccount(paymentAccount))
                .build());
    }

    // Getting a payment account by its id
    @GetMapping("/getPaymentAccountById/{paymentAccountId}")
    public ResponseEntity<?> getPaymentAccountById(
            @Valid @PathVariable("paymentAccountId") Long paymentAccountId
    ) {
        //get the payment account by id
        PaymentAccountEntity existingPaymentAccount = paymentAccountService.getPaymentAccountById(paymentAccountId);

        //return the account in response
        return ResponseEntity.ok(ResponseObject.builder()
                .result(PaymentAccountResponse.fromPaymentAccount(existingPaymentAccount))
                .message("Get Payment Account successfully")
                .status(HttpStatus.OK)
                .build());
    }

    // Getting a customer by the payment account number
    @GetMapping("/getByAccountNumber/{accountNumber}")
    public ResponseEntity<?> getCustomerByAccountNumber(
            @Valid @PathVariable("accountNumber") String accountNumber
    ) {
        //get the customer by the payment account number
        PaymentAccountEntity existingPaymentAccount = paymentAccountService.getPaymentAccountByAccountNumber(accountNumber);

        //return result in response
        return ResponseEntity.ok(ResponseObject.builder()
                .result(CustomerResponse.fromCustomerResponse(existingPaymentAccount.getCustomer()))
                .message("Get Payment Account successfully")
                .status(HttpStatus.OK)
                .build());
    }

    // Getting all payments accounts of a customer
    @GetMapping("/getPaymentAccounts/{customerId}")
    public ResponseEntity<?> getPaymentAccountsByCustomerId(
            @Valid @PathVariable("customerId") Long customerId
    ){
        // Retrieve payment accounts for the given customer ID
        List<PaymentAccountEntity> paymentAccounts = paymentAccountService.getPaymentAccountsByCustomerId(customerId);

        //Build response
        List<PaymentAccountResponse> paymentAccountResponse = paymentAccounts.stream()
                .map(PaymentAccountResponse::fromPaymentAccount)
                .toList();

        PaymentAccountListResponse paymentAccountListResponse = PaymentAccountListResponse
                .builder()
                .paymentAccounts(paymentAccountResponse)
                .build();

        // Return a successful response with the payment account data
        return ResponseEntity.ok().body(ResponseObject.builder()
                .message("Get Payment Account list by CustomerID successfully")
                .status(HttpStatus.OK)
                .result(paymentAccountListResponse)
                .build());
    }

    // End point to send OTP for adding money to a payment account
    // TRANSACTION for ADD_FROM_BA_TO_PA (Trans 5)
    @PostMapping("/sendOTPToAddMoney/{bankName}")
    public ResponseEntity<?> sendOTPToAddMoney(
            @Valid @PathVariable("bankName") String bankName,
            @Valid @RequestBody MoneyFlowRequest moneyFlowRequest
    ) {
        double amountTopUp = moneyFlowRequest.getAmountOperationRequest().getAmount();
        if(amountTopUp < 10000){
            throw new AppException(ErrorCode.MINIMUM_ADD_MONEY);
        } else if (amountTopUp > 50000000) {
            throw new AppException(ErrorCode.MAXIMUM_ADD_MONEY);
        }

        // Check existing bank account
        BankAccountEntity existingBankAccount = bankAccountService
                .checkBankAccountExist(moneyFlowRequest.getBankAccountRequest(), bankName);

        // Create pending transaction for ADD MONEY
        TransactionCustomerEntity pendingAddMoneyToPA = paymentAccountService
                .insertTransactionForAddMoneyToPA(existingBankAccount.getId(),
                        existingBankAccount.getPaymentAccount().getId(),
                        amountTopUp);

        // Send OTP SMS and validate phone number
        String OTP = otpService.generateOTP("",
                existingBankAccount.getPhoneNumber(),
                4,
                OTPPurpose.SMS_PAYMENT_ACCOUNT_ADD_MONEY);

        String content = "DO NOT share your OTP with others to avoid fraud. The OTP for the transaction with the amount of "
                + amountTopUp + "VND on bank " + bankName + " is: " + OTP;

        SMSOTPResponse smsotpResponse = smsService.sendSMSOTP(existingBankAccount.getPhoneNumber(), content);
        if(!smsotpResponse.getStatus().equals("success")){
            throw new AppException(ErrorCode.SMS_OTP_FAIL);
        }

        // Return a successful response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Send OTP for adding money and create pending ADD money transaction successfully")
                .result(TransactionCustomerResponse.fromTransactionCustomer(pendingAddMoneyToPA))
                .build());
    }

    // End point validate OTP for adding money to a payment account
    @PutMapping("/addMoneyToPaymentAccount/{paymentAccountId}/{bankName}/{transactionId}")
    public ResponseEntity<?> addMoneyToPaymentAccount(
            @Valid @PathVariable("paymentAccountId") Long paymentAccountId,
            @Valid @PathVariable("bankName") String bankName,
            @Valid @PathVariable("transactionId") Long transactionId,
            @Valid @RequestBody MoneyFlowRequest moneyFlowRequest,
            @Valid @RequestParam Integer failTime
    ) {
        double amountTopUp = moneyFlowRequest.getAmountOperationRequest().getAmount();

        // Check existing bank account
        BankAccountEntity existingBankAccount = bankAccountService
                .checkBankAccountExist(moneyFlowRequest.getBankAccountRequest(), bankName);

        // Call service layer to verify OTP via SMS
        boolean isValid = otpService.verifyOTP("",
                moneyFlowRequest.getBankAccountRequest().getPhoneNumber(),
                moneyFlowRequest.getAmountOperationRequest().getOtp(),
                OTPPurpose.SMS_PAYMENT_ACCOUNT_ADD_MONEY);

        // Find existingPaymentAccount
        PaymentAccountEntity existingPaymentAccount = existingBankAccount.getPaymentAccount();
        CustomerEntity existingCustomer = existingPaymentAccount.getCustomer();
        TransactionEntity existingTransaction = transactionService.getTransactionById(transactionId);

        // Return response based on validation result
        if (isValid) {
            // Compare request top-up money with current balance in bankAcc
            if(amountTopUp > existingBankAccount.getCurrentBalance()){
                // Set transaction to FAIL
                TransactionEntity failTransaction = transactionService
                        .failAddMoneyToPA(existingCustomer, existingTransaction);

                // Build response
                Map<String, Object> failedAddMoneyFromBAToPA = new HashMap<>();
                failedAddMoneyFromBAToPA.put("failTransaction", TransactionResponse.fromTransaction(failTransaction));
                failedAddMoneyFromBAToPA.put("paymentAccount", PaymentAccountResponse.fromPaymentAccount(existingPaymentAccount));

                return ResponseEntity.badRequest().body(ResponseObject.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("Verify wrong OTP for transaction exceed 3 times")
                        .result("Fail to add money from BA to PA")
                        .multiResult(failedAddMoneyFromBAToPA)
                        .build());
            }

            // Perform the add money operation to specified PA from BA
            PaymentAccountEntity paymentAccountResponse = paymentAccountService
                    .addMoneyToPaymentAccount(paymentAccountId,
                            bankName,
                            moneyFlowRequest.getBankAccountRequest(),
                            moneyFlowRequest.getAmountOperationRequest());

            // Transaction
            TransactionEntity completeAddMoneyFromBAToPA = transactionService
                    .completeAddMoneyFromBAToPA(existingCustomer, existingTransaction);

            // Build response
            Map<String, Object> addMoneyFromBAToPA = new HashMap<>();
            addMoneyFromBAToPA.put("completeTransaction", TransactionResponse.fromTransaction(completeAddMoneyFromBAToPA));
            addMoneyFromBAToPA.put("paymentAccount", PaymentAccountResponse.fromPaymentAccount(paymentAccountResponse));

            // Return a successful response
            return ResponseEntity.ok().body(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Add money from bank account to payment account successfully")
                    .multiResult(addMoneyFromBAToPA)
                    .build());
        }
        else {
            // Check OTP verification 3 fails
            if(failTime == 3){
                // Set transaction to FAIL
                TransactionEntity failTransaction = transactionService
                        .failAddMoneyToPA(existingCustomer, existingTransaction);

                // Build response
                Map<String, Object> failedAddMoneyFromBAToPA = new HashMap<>();
                failedAddMoneyFromBAToPA.put("failTransaction", TransactionResponse.fromTransaction(failTransaction));
                failedAddMoneyFromBAToPA.put("paymentAccount", PaymentAccountResponse.fromPaymentAccount(existingPaymentAccount));

                return ResponseEntity.badRequest().body(ResponseObject.builder()
                        .status(HttpStatus.BAD_REQUEST)
                        .message("Verify wrong OTP for transaction exceed 3 times")
                        .result("Fail to add money from BA to PA")
                        .multiResult(failedAddMoneyFromBAToPA)
                        .build());
            }

            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.UNAUTHORIZED)
                    .message("Invalid OTP")
                    .result("Invalid input OTP: " + moneyFlowRequest.getAmountOperationRequest().getOtp())
                    .build());
        }
    }

    // End point for withdrawing money from a payment account
    // TRANSACTION for WITHDRAW_FROM_PA_TO_BA (Trans 6)
    @PutMapping("/withdrawFromPaymentAccount/{paymentAccountId}/{bankName}")
    public ResponseEntity<?> withdrawFromPaymentAccount(
            @Valid @PathVariable("paymentAccountId") Long paymentAccountId,
            @Valid @PathVariable("bankName") String bankName,
            @Valid @RequestBody MoneyFlowRequest moneyFlowRequest
    ) {
        if(moneyFlowRequest.getAmountOperationRequest().getAmount() < 50000){
            throw new AppException(ErrorCode.MINIMUM_WITHDRAW_MONEY);
        } else if (moneyFlowRequest.getAmountOperationRequest().getAmount() > 50000000) {
            throw new AppException(ErrorCode.MAXIMUM_WITHDRAW_MONEY);
        }

        // Perform the withdrawal money operation from specified payment account
        Map<String, Object> completedWithdrawMoneyFromPAToBA  = paymentAccountService
                .withdrawFromPaymentAccount(paymentAccountId, bankName,
                        moneyFlowRequest.getBankAccountRequest(),
                        moneyFlowRequest.getAmountOperationRequest());

        // Return a successful response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Withdraw money from payment account to bank account successfully")
                .result(completedWithdrawMoneyFromPAToBA)
                .build());
    }

    // End point for update a payment account by id
    @PutMapping("/updatePaymentAccount/{paymentAccountId}")
    public ResponseEntity<?> updatePaymentAccount(
            @Valid @PathVariable("paymentAccountId") Long paymentAccountId,
            @Valid @RequestBody PaymentAccountRequest paymentAccountRequest
    ) {
        // Update the payment account details
        PaymentAccountEntity paymentAccountResponse = paymentAccountService
                .updatePaymentAccount(paymentAccountId, paymentAccountRequest);

        // Return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Update payment account with ID: " + paymentAccountId + "successfully")
                .result(PaymentAccountResponse.fromPaymentAccount(paymentAccountResponse))
                .build());
    }

    // End point for delete a payment account by id
    @DeleteMapping("/deletePaymentAccount/{paymentAccountId}")
    public ResponseEntity<?> deletePaymentAccount(
            @Valid @PathVariable("paymentAccountId") long paymentAccountId
    ) {
        // Hard-delete payment account's profile
        paymentAccountService.deletePaymentAccountById(paymentAccountId);

        // Return deleted payment account information in the response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Delete payment account with ID: " + paymentAccountId + " successfully")
                .build());
    }

    @GetMapping("/getPaymentAccountByAccountNumber/{accountNumber}")
    public ResponseEntity<?> getPaymentAccountByAccountNumber(
            @Valid @PathVariable("accountNumber") String accountNumber
    ) {
        PaymentAccountEntity existingPaymentAccount = paymentAccountService.getPaymentAccountByAccountNumber(accountNumber);

        Map<String, Object> result = new HashMap<>();
        result.put("account_number", existingPaymentAccount.getAccountNumber());
        result.put("phone_number", existingPaymentAccount.getCustomer().getPhoneNumber());
        result.put("name", existingPaymentAccount.getCustomer().getName());
        result.put("id", existingPaymentAccount.getId());

        //return result in response
        return ResponseEntity.ok(ResponseObject.builder()
                .result(result)
                .message("Get Payment Account successfully")
                .status(HttpStatus.OK)
                .build());
    }

    @GetMapping("/searchPaymentAccountByAccountNumber/{accountNumber}")
    public ResponseEntity<?> searchPaymentAccountByAccountNumber(
            @Valid @PathVariable("accountNumber") String accountNumber
    ) {
        List<PaymentAccountEntity> existingPaymentAccounts = paymentAccountService.searchPaymentAccountByAccountNumber(accountNumber);

        List<PaymentAccountResponse> paymentAccountResponse = existingPaymentAccounts.stream()
                .map(PaymentAccountResponse::fromPaymentAccount)
                .toList();

        PaymentAccountListResponse paymentAccountListResponse = PaymentAccountListResponse
                .builder()
                .paymentAccounts(paymentAccountResponse)
                .build();

        //return result in response
        return ResponseEntity.ok(ResponseObject.builder()
                .result(paymentAccountListResponse)
                .message("Get Payment Account successfully")
                .status(HttpStatus.OK)
                .build());
    }

    ////////////////////////////////////////////////////////////////////////////
    // Case 3: same customer's multiple PAs -> Manage money in own PAs

}
