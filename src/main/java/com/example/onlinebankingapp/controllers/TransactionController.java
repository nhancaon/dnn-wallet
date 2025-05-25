package com.example.onlinebankingapp.controllers;

import com.example.onlinebankingapp.dtos.requests.AccountRewardRequest;
import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionRequest;
import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionToBA.TransactionToBAFromBARequest;
import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionToBA.TransactionToBAFromPARequest;
import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionToPA.TransactionToPAFromBARequest;
import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionToPA.TransactionToPAFromPARequest;
import com.example.onlinebankingapp.dtos.responses.SMSOTPResponse;
import com.example.onlinebankingapp.dtos.responses.TransactionCustomer.TransactionCustomerListResponse;
import com.example.onlinebankingapp.dtos.responses.TransactionCustomer.TransactionCustomerResponse;
import com.example.onlinebankingapp.entities.CustomerEntity;
import com.example.onlinebankingapp.entities.TransactionCustomerEntity;
import com.example.onlinebankingapp.entities.TransactionEntity;
import com.example.onlinebankingapp.dtos.responses.ResponseObject;
import com.example.onlinebankingapp.dtos.responses.Transaction.TransactionListResponse;
import com.example.onlinebankingapp.dtos.responses.Transaction.TransactionResponse;
import com.example.onlinebankingapp.enums.*;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import com.example.onlinebankingapp.services.Customer.CustomerService;
import com.example.onlinebankingapp.services.Transaction.TransactionServiceImpl;
import com.example.onlinebankingapp.services.VerificationServices.OTPService;
import com.example.onlinebankingapp.services.Transaction.TransactionService;
import com.example.onlinebankingapp.services.VerificationServices.SMSService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;
    private final CustomerService customerService;
    private final OTPService otpService;

    @Autowired
    private SMSService smsService;

    // OBJECTIVE for TRANSACTIONS TRACK HISTORY
    // TRANSACTION for ADD_FROM_BA_TO_SA (Trans 8)
    // TRANSACTION for WITHDRAW_FROM_SA_TO_BA (Trans 9)
    // TRANSACTION for REDEEM_REWARD (Trans 11)
    ///////////////////////////////////////////////////////////////////////////

    // Check unexpired PENDING transaction for TRANSFER_MONEY
    @GetMapping("/checkExpiredPendingTransaction/{customerId}")
    public ResponseEntity<?> checkExpiredPendingTransaction(
            @Valid @PathVariable("customerId") Long customerId,
            @Valid @RequestParam String transferTo
    ){
        // Check customer exist
        CustomerEntity existingCustomer = customerService.getCustomerById(customerId);
        if(!existingCustomer.isActive()){
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        if(transferTo.equals("PAYMENT_ACCOUNT")){
            Map<String, Object> transferToPA = transactionService.getUnexpiredPendingTransferToPA(existingCustomer);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Continue to pending or create new transaction for TRANSFER_MONEY to PAYMENT_ACCOUNT. ")
                    .multiResult(transferToPA)
                    .build());
        }
        else if(transferTo.equals("BANK_ACCOUNT")){
            Map<String, Object> transferToBA = transactionService.getUnexpiredPendingTransferToBA(existingCustomer);
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Continue to pending or create new transaction for TRANSFER_MONEY to BANK_ACCOUNT. ")
                    .multiResult(transferToBA)
                    .build());
        }
        else {
            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.UNAUTHORIZED)
                    .message("TransferTo parameter can be only either PAYMENT_ACCOUNT or BANK_ACCOUNT")
                    .result("Front-end transfer wrong transferTo parameter")
                    .build());
        }
    }

    // Force to check transaction amount in FE
    // Always prior to transfer via default PA (e-wallet)
    // Insert a new PENDING transaction to either PA or BA
    @PostMapping("/checkDefaultPAForTransfer/{customerId}")
    public ResponseEntity<?> checkDefaultPAForTransfer(
            @Valid @PathVariable("customerId") Long customerId,
            @RequestParam(value = "oldUnexpiredPendingTransactionId", required = false) Long oldUnexpiredPendingTransactionId,
            @Valid @RequestBody TransactionRequest transactionRequest
    ) {
        // Ensure check transaction amount once again
        if (transactionRequest.getAmount() < 2000) {
            throw new AppException(ErrorCode.MINIMUM_OUT_TRANSFER_MONEY);
        } else if (transactionRequest.getAmount() > 99999999) {
            throw new AppException(ErrorCode.MAXIMUM_OUT_TRANSFER_MONEY);
        }

        // Check customer exist with non-empty phone number
        CustomerEntity existingCustomer = customerService.getCustomerById(customerId);
        if(!existingCustomer.isActive() || existingCustomer.getPhoneNumber().isEmpty() || existingCustomer.getPhoneNumber().isBlank()){
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        // Create new transaction for display PENDING suggesting transaction if interrupted
        TransactionCustomerEntity newPendingTransactionCustomer = transactionService
                .checkDefaultPAForTransfer(existingCustomer, oldUnexpiredPendingTransactionId, transactionRequest);

        if(TransactionServiceImpl.defaultPAEnough){
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("DEFAULT PA HAS ENOUGH MONEY. Insert a new PENDING transaction for TRANSFER_MONEY successfully")
                    .result(TransactionCustomerResponse.fromTransactionCustomer(newPendingTransactionCustomer))
                    .build());
        }
        else {
            return ResponseEntity.status(HttpStatus.OK).body(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("DEFAULT PA DOES NOT HAVE ENOUGH MONEY. Insert a new PENDING transaction for TRANSFER_MONEY successfully")
                    .result(TransactionCustomerResponse.fromTransactionCustomer(newPendingTransactionCustomer))
                    .build());
        }
    }

    // Case 1: transfer TO PA (e-wallet) -> other people's PA
    // Trans 1: TRANSACTION for TRANSFER MONEY FROM PA TO PA
    @PostMapping("/transferToPaymentAccount/FromPaymentAccount/{customerId}")
    public ResponseEntity<?> transferToPaymentAccountFromPaymentAccount(
            @Valid @PathVariable("customerId") Long customerId,
            @Valid @RequestBody TransactionToPAFromPARequest transactionToPAFromPARequest
    ) {
        TransactionEntity updateTransaction = transactionService.transferToPAFromPA(customerId, transactionToPAFromPARequest);

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Choose PA as sender to transfer money to PA successfully")
                .result(TransactionResponse.fromTransaction(updateTransaction))
                .build());
    }

    // Trans 2: TRANSACTION for TRANSFER MONEY FROM BA TO PA
    @PostMapping("/transferToPaymentAccount/FromBankAccount/{customerId}")
    public ResponseEntity<?> transferToPaymentAccountFromBankAccount(
            @Valid @PathVariable("customerId") Long customerId,
            @Valid @RequestBody TransactionToPAFromBARequest transactionToPAFromBARequest
    ) {
        TransactionEntity updateTransaction = transactionService.transferToPAFromBA(customerId, transactionToPAFromBARequest);

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Choose BA as sender to transfer money to PA successfully")
                .result(TransactionResponse.fromTransaction(updateTransaction))
                .build());
    }

    // Case 2: transfer TO BA
    // Update transactionDateTime, transactionStatus, senderId, transactionSenderType
    // Update balance in table BA and PA
    // Trans 3: TRANSACTION for TRANSFER MONEY FROM PA TO BA
    @PostMapping("/transferToBankAccount/FromPaymentAccount/{customerId}")
    public ResponseEntity<?> transferToBankAccountFromPaymentAccount(
            @Valid @PathVariable("customerId") Long customerId,
            @Valid @RequestBody TransactionToBAFromPARequest transactionToBAFromPARequest
    ) {
        TransactionEntity updateTransaction = transactionService.transferToBAFromPA(customerId, transactionToBAFromPARequest);

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Choose PA as sender to transfer money to BA successfully")
                .result(TransactionResponse.fromTransaction(updateTransaction))
                .build());
    }

    // Trans 4: TRANSACTION for TRANSFER MONEY FROM BA TO BA
    @PostMapping("/transferToBankAccount/FromBankAccount/{customerId}")
    public ResponseEntity<?> transferToBankAccountFromBankAccount(
            @Valid @PathVariable("customerId") Long customerId,
            @Valid @RequestBody TransactionToBAFromBARequest transactionToBAFromBARequest
    ) {
        TransactionEntity updateTransaction = transactionService.transferToBAFromBA(customerId, transactionToBAFromBARequest);

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Choose BA as sender to transfer money to BA successfully")
                .result(TransactionResponse.fromTransaction(updateTransaction))
                .build());
    }

    // Send OTP for transferring money
    @PostMapping("/sendOTPForTransferMoney/{customerId}/{transactionId}")
    public ResponseEntity<?> sendOTPForTransferMoney(
            @Valid @PathVariable("customerId") Long customerId,
            @Valid @PathVariable("transactionId") Long transactionId
    ) {
        // Check transaction of customer or not
        // Update transaction date time for synchronizing with generate OTP
        if(!transactionService.checkTransactionCustomerExist(customerId, transactionId)){
            throw new AppException(ErrorCode.TRANSACTION_OF_CUSTOMER_NOT_FOUND);
        }

        // Customer with transaction exist
        // Get phone number of customer and check customer account IS_ACTIVE
        CustomerEntity existingCustomer = customerService.getCustomerById(customerId);
        if(!existingCustomer.isActive() || existingCustomer.getPhoneNumber().isEmpty() || existingCustomer.getPhoneNumber().isBlank()){
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        // Check transaction exist suitable for transfer money
        TransactionEntity existingTransaction = transactionService.getTransactionById(transactionId);
        if(existingTransaction == null){
            throw new AppException(ErrorCode.TRANSACTION_NOT_FOUND);
        }
        if(!existingTransaction.getTransactionType().equals(TransactionType.TRANSFER_MONEY)){
            throw new AppException(ErrorCode.TRANSACTION_INVALID);
        }
        if(!existingTransaction.getTransactionStatus().equals(TransactionStatus.PENDING)) {
            throw new AppException(ErrorCode.TRANSACTION_INVALID);
        }

        // Check transfer money to receiver
        OTPPurpose otpPurpose = OTPPurpose.SMS_TRANSFER_MONEY_TO_PA;
        String result = "Transfer money to Payment Account. Correct transaction OTP: ";
        if(existingTransaction.getTransactionReceiverType().equals(TransactionReceiverType.BANK_ACCOUNT)){
            otpPurpose = OTPPurpose.SMS_TRANSFER_MONEY_TO_BA;
            result = "Transfer money to Bank Account. Correct transaction OTP: ";
        }

        // Send OTP SMS and validate phone number
        String OTP = otpService.generateOTP("",
                existingCustomer.getPhoneNumber(),
                4,
                otpPurpose);

        String content = "Your OTP is: " + OTP
                + ". The transaction authentication number for the wallet is valid for only 4 minutes. "
                + "DO NOT share your OTP with others to avoid fraud";

        SMSOTPResponse smsotpResponse = smsService.sendSMSOTP(existingCustomer.getPhoneNumber(), content);
        if(!smsotpResponse.getStatus().equals("success")){
            throw new AppException(ErrorCode.SMS_OTP_FAIL);
        }

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Send OTP for transferring money successfully")
                .result(result + OTP)
                .build());
    }

    // SMS OTP Verification (3 times)
    // SUCCESS -> change transactionStatus from PENDING to SUCCESS

    // FAIL -> change transactionStatus from PENDING to FAIL (time 4th)
    // Front end must request at least 4th time when invalid OTP and increment the failTime (0, 1, 2, 3)

    // FAIL TRANSACTION WHEN:
    // Case 1: Choose associated BA as sender and its balance is not enough
    // Case 2: Exceed 3 times SMS OTP verification (in the interval '3 minutes' of the life of SMS OTP)

    // Case 3: When OTP expired (special)
    // If countdown clock of FE exceed 3 minutes with mean that SMS OTP expired
    // Front end call this method again, developer input correct otp (given in method sendOtp above)
    // And input failTime = 3 -> successfully remove OTP out of storage
    @PostMapping("/verifyOTPForTransferMoney/{customerId}/{transactionId}")
    public ResponseEntity<?> verifyOTPForTransferMoney(
            @Valid @PathVariable("customerId") Long customerId,
            @Valid @PathVariable("transactionId") Long transactionId,
            @Valid @RequestParam Integer failTime,
            @Valid @RequestParam String otpTransfer
    ) {
        // Check customer for phone number
        CustomerEntity existingCustomer = customerService.getCustomerById(customerId);

        // Check transaction exist suitable for transfer money
        TransactionEntity existingTransaction = transactionService.getTransactionById(transactionId);

        // Check transfer to WHO
        OTPPurpose otpPurpose = OTPPurpose.SMS_TRANSFER_MONEY_TO_PA;
        if(existingTransaction.getTransactionReceiverType().equals(TransactionReceiverType.BANK_ACCOUNT)){
            otpPurpose = OTPPurpose.SMS_TRANSFER_MONEY_TO_BA;
        }

        // Call service layer to verify OTP via SMS
        boolean isValid = otpService.verifyOTP("",
                existingCustomer.getPhoneNumber(),
                otpTransfer,
                otpPurpose);

        // Return response based on validation result
        if (isValid) {
            // Complete and update transfer money data
            TransactionCustomerEntity transactionCustomerResponse = transactionService.completeTransferMoney(existingCustomer, existingTransaction);
            String message = getMessageForVerification(transactionCustomerResponse);

            // Return a successful response
            return ResponseEntity.ok().body(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message(message)
                    .result(TransactionCustomerResponse.fromTransactionCustomer(transactionCustomerResponse))
                    .build());
        }
        else {
            // Check OTP verification 3 fails
            if(failTime == 3){
                // Set transaction to FAIL
                transactionService.failTransferMoney(existingCustomer, existingTransaction);
                throw new AppException(ErrorCode.TRANSACTION_FAIL);
            }

            return ResponseEntity.badRequest().body(ResponseObject.builder()
                    .status(HttpStatus.UNAUTHORIZED)
                    .message("Invalid OTP. Failed to transfer money")
                    .result("Invalid input OTP: " + otpTransfer)
                    .build());
        }
    }

    // Endpoint for getting a transaction by its ID
    @GetMapping("/getTransactionByTransactionId/{transactionId}")
    public ResponseEntity<?> getTransactionByTransactionId(
            @Valid @PathVariable("transactionId") Long transactionId
    ) {
        TransactionEntity existingTransaction = transactionService.getTransactionById(transactionId);

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get transactions history successfully")
                .result(TransactionResponse.fromTransaction(existingTransaction))
                .build());
    }

    // Endpoint for getting all transaction
    @GetMapping("/getAllTransactions")
    public ResponseEntity<?> getAllTransactions() {
        // Retrieve a list of transaction entities
        List<TransactionCustomerEntity> transactionCustomerEntityList = transactionService.getAllTransactions();

        // Create response
        List<TransactionCustomerResponse> transactionCustomerResponseList = transactionCustomerEntityList.stream()
                .map(TransactionCustomerResponse::fromTransactionCustomer)
                .toList();

        TransactionCustomerListResponse transactionCustomerListResponse = TransactionCustomerListResponse.builder()
                .transactionCustomers(transactionCustomerResponseList)
                .build();

        // Return the response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get list of all transactions successfully")
                .result(transactionCustomerListResponse)
                .build());
    }

    // Endpoint for getting all transactions with pagination
    @GetMapping("/getPaginationListTransaction")
    public ResponseEntity<?> getPaginationListTransaction(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String orderedBy,
            @RequestParam(defaultValue = "false") String isAscending,
            @RequestParam(defaultValue = "") String keyword
    ) {
        Boolean isAsc = Boolean.parseBoolean(isAscending);

        TransactionCustomerListResponse transactionCustomerPaginated = transactionService
                .getPaginationListTransactionCustomer(page, size, orderedBy, isAsc, keyword);

        //return result in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get all paginated transactions of customers successfully")
                .result(transactionCustomerPaginated)
                .build());
    }

    // Endpoint for retrieving transactions history by customer ID
    @GetMapping("/trackTransactionHistory/{id}")
    public ResponseEntity<?> trackTransactionHistory(
            @Valid @PathVariable("id") Long customerId,
            @RequestParam(value = "transactionTypes", required = false) List<TransactionType> transactionTypes,
            @RequestParam(value = "transactionReceiverType", required = false) TransactionReceiverType transactionReceiverType,
            @RequestParam(required = false) Integer size
    ) {
        // Check customer exist
        CustomerEntity existingCustomer = customerService.getCustomerById(customerId);
        if(!existingCustomer.isActive()){
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        List<TransactionEntity> transactions = transactionService.trackTransactionHistory(existingCustomer, transactionTypes, transactionReceiverType, size);

        // Create transaction history response
        List<TransactionResponse> transactionResponses = transactions.stream()
                .map(TransactionResponse::fromTransaction)
                .toList();

        TransactionListResponse transactionListResponse = TransactionListResponse.builder()
                .transactions(transactionResponses)
                .build();

        // Return response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get transactions history successfully")
                .result(transactionListResponse)
                .build());
    }

    @NotNull
    private static String getMessageForVerification(
            TransactionCustomerEntity transactionCustomerResponse
    ) {
        TransactionEntity completeTransaction = transactionCustomerResponse.getTransactionCustomerKey().getTransaction();

        // Default settings
        // From PA to PA
        String sender = String.valueOf(TransactionSenderType.PAYMENT_ACCOUNT);
        String receiver = String.valueOf(TransactionReceiverType.PAYMENT_ACCOUNT);

        // From PA to BA
        if(completeTransaction.getTransactionSenderType().equals(TransactionSenderType.PAYMENT_ACCOUNT)
        && completeTransaction.getTransactionReceiverType().equals(TransactionReceiverType.BANK_ACCOUNT)){
            sender = String.valueOf(TransactionSenderType.PAYMENT_ACCOUNT);
            receiver = String.valueOf(TransactionReceiverType.BANK_ACCOUNT);
        }

        // From BA to PA
        if(completeTransaction.getTransactionSenderType().equals(TransactionSenderType.BANK_ACCOUNT)
                && completeTransaction.getTransactionReceiverType().equals(TransactionReceiverType.PAYMENT_ACCOUNT)){
            sender = String.valueOf(TransactionSenderType.BANK_ACCOUNT);
            receiver = String.valueOf(TransactionReceiverType.PAYMENT_ACCOUNT);
        }

        // From BA to BA
        if(completeTransaction.getTransactionSenderType().equals(TransactionSenderType.BANK_ACCOUNT)
                && completeTransaction.getTransactionReceiverType().equals(TransactionReceiverType.BANK_ACCOUNT)){
            sender = String.valueOf(TransactionSenderType.BANK_ACCOUNT);
            receiver = String.valueOf(TransactionReceiverType.BANK_ACCOUNT);
        }

        return "Transfer money successfully from " + sender + " to " + receiver;
    }

    @GetMapping("/getMonthlyTotalExpenseAmount/{customerId}")
    public ResponseEntity<?> getMonthlyTotalExpenseAmount(
            @PathVariable("customerId") Long customerId,
            @RequestParam TransactionStatus transactionStatus,
            @RequestParam TransactionType transactionType,
            @RequestParam int year) {

        // Check if customer exists
        CustomerEntity existingCustomer = customerService.getCustomerById(customerId);
        if (existingCustomer == null || !existingCustomer.isActive()) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        // Fetch monthly total amounts including zero for months with no transactions
        List<Object[]> monthlyTotalAmounts = transactionService.getMonthlyTotalExpenseAmountForCustomer(
                customerId, transactionStatus, transactionType,year);

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Monthly total amount fetched successfully.")
                .result(monthlyTotalAmounts)
                .build());
    }

    @GetMapping("/getMonthlyTotalIncomeAmount/{customerId}")
    public ResponseEntity<?> getMonthlyTotalIncomeAmount(
            @PathVariable("customerId") Long customerId,
            @RequestParam TransactionStatus transactionStatus,
            @RequestParam TransactionType transactionType,
            @RequestParam int year) {

        // Check if customer exists
        CustomerEntity existingCustomer = customerService.getCustomerById(customerId);
        if (existingCustomer == null || !existingCustomer.isActive()) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        // Fetch monthly total amounts including zero for months with no transactions
        List<Object[]> monthlyTotalAmounts = transactionService.getMonthlyTotalIncomeAmountForCustomer(
                customerId, transactionStatus, transactionType,year);

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Monthly total amount fetched successfully.")
                .result(monthlyTotalAmounts)
                .build());
    }
    @GetMapping("/getMonthlyTotalRewardAmount/{customerId}")
    public ResponseEntity<?> GetMonthlyTotalRewardAmount(
            @PathVariable("customerId") Long customerId,
            @RequestParam TransactionStatus transactionStatus,
            @RequestParam TransactionType transactionType,
            @RequestParam int year) {

        // Check if customer exists
        CustomerEntity existingCustomer = customerService.getCustomerById(customerId);
        if (existingCustomer == null || !existingCustomer.isActive()) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        // Fetch monthly total amounts including zero for months with no transactions
        List<Object[]> monthlyTotalAmounts = transactionService.findTotalRewardAmountForCustomer(
                customerId, transactionStatus, transactionType,year);

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get Total Reward Amount successfully.")
                .result(monthlyTotalAmounts)
                .build());
    }

    @PostMapping("/insertRewardTransaction")
    public ResponseEntity<?> insertRewardTransaction(
            @Valid @RequestBody AccountRewardRequest accountRewardDTO
    ) {
        // Use the requested reward
        TransactionEntity transactionEntity = transactionService.insertRewardTransaction(accountRewardDTO);

        // Return the result in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Insert reward transaction successfully")
                .result(TransactionResponse.fromTransaction(transactionEntity))
                .build());
    }
}
