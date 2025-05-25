package com.example.onlinebankingapp.services.PaymentAccount;

import com.example.onlinebankingapp.dtos.requests.PaymentAccount.AmountOperationRequest;
import com.example.onlinebankingapp.dtos.requests.BankAccount.BankAccountRequest;
import com.example.onlinebankingapp.dtos.requests.PaymentAccount.PaymentAccountRequest;
import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionRequest;
import com.example.onlinebankingapp.dtos.responses.BankAccount.BankAccountResponse;
import com.example.onlinebankingapp.dtos.responses.PaymentAccount.PaymentAccountListResponse;
import com.example.onlinebankingapp.dtos.responses.PaymentAccount.PaymentAccountResponse;
import com.example.onlinebankingapp.dtos.responses.Transaction.TransactionResponse;
import com.example.onlinebankingapp.entities.*;
import com.example.onlinebankingapp.enums.AccountStatus;
import com.example.onlinebankingapp.enums.AccountType;
import com.example.onlinebankingapp.enums.TransactionStatus;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import com.example.onlinebankingapp.repositories.BankAccountRepository;
import com.example.onlinebankingapp.repositories.CustomerRepository;
import com.example.onlinebankingapp.repositories.PaymentAccountRepository;
import com.example.onlinebankingapp.repositories.TransactionRepository;
import com.example.onlinebankingapp.services.BankAccount.BankAccountServiceImpl;
import com.example.onlinebankingapp.services.Transaction.TransactionService;
import com.example.onlinebankingapp.utils.DateTimeUtils;
import com.example.onlinebankingapp.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentAccountServiceImpl implements PaymentAccountService {
    private final PaymentAccountRepository paymentAccountRepository;
    private final CustomerRepository customerRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;

    private final BankAccountServiceImpl bankAccountServiceImpl;
    private TransactionService transactionService;

    @Autowired
    public void setTransactionService(@Lazy TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Method to insert a new payment account
    // P/s: phone number is first payment account number -> created after sign-up successfully
    @Override
    public PaymentAccountEntity insertPaymentAccount(
            String newAccountNumber,
            Long customerId
    ) {
        // Check if the account number already exists
        if(paymentAccountRepository.existsByAccountNumber(newAccountNumber)){
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_EXISTS);
        }

        // Check if customer is existed
        Optional<CustomerEntity> optionalCustomer = customerRepository.findById(customerId);
        // If customer exists, return it, otherwise throw an exception
        if(optionalCustomer.isEmpty()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        CustomerEntity existingCustomer = optionalCustomer.get();
        // Check active customer
        if(!existingCustomer.isActive()){
            throw new DataIntegrityViolationException("This customer has not completed sign-up process");
        }

        // Check if DEFAULT account number (phone number) -> first time (after sign-up successfully)
        if(existingCustomer.getPhoneNumber().equals(newAccountNumber)){
            // Create a first Payment Account with account number is phone number
            PaymentAccountEntity firstPaymentAccount = PaymentAccountEntity.builder()
                    .accountNumber(newAccountNumber)
                    .accountStatus(AccountStatus.DEFAULT)
                    .accountType(AccountType.CLASSIC)
                    .dateOpened(DateTimeUtils.getVietnamCurrentDateTime())
                    .dateClosed(null)
                    .customer(existingCustomer)
                    .build();

            return  paymentAccountRepository.save(firstPaymentAccount);
        }
        else {
            // Create a new PaymentAccountEntity -> already has DEFAULT
            PaymentAccountEntity newPaymentAccount = PaymentAccountEntity.builder()
                    .accountNumber(newAccountNumber)
                    .build();

            // DEFAULT account exist -> want to create more
            boolean isExistingDefaultAccount = paymentAccountRepository
                    .checkExistingByStatus(existingCustomer.getId(), AccountStatus.DEFAULT);

            // If no default account exists, set the new account as default
            if(!isExistingDefaultAccount) {
                newPaymentAccount.setAccountStatus(AccountStatus.DEFAULT);
            }

            newPaymentAccount.setCustomer(existingCustomer);
            return paymentAccountRepository.save(newPaymentAccount);
        }
    }

    // Method to set an account as the default payment account
    @Override
    public void setDefaultPaymentAccount(
            Long customerId,
            String accountNumber
    ) {
        // Get the current default payment account and set its status to ACTIVE
        PaymentAccountEntity existingDefaultPaymentAccount = getDefaultPaymentAccount(customerId);

        // Get the payment account to be set as default and set its status to DEFAULT
        PaymentAccountEntity paymentAccountToSetDefault = paymentAccountRepository.getPaymentAccountByAccountNumber(accountNumber);

        // Set account status
        existingDefaultPaymentAccount.setAccountStatus(AccountStatus.ACTIVE);
        paymentAccountToSetDefault.setAccountStatus(AccountStatus.DEFAULT);

        // Save the updated entities
        paymentAccountRepository.save(existingDefaultPaymentAccount);
        paymentAccountRepository.save(paymentAccountToSetDefault);
    }

    // Method to get all payment accounts
    @Override
    public List<PaymentAccountEntity> getAllPaymentAccounts() {
        return paymentAccountRepository.findAll();
    }

    // Method to get a payment account by its ID
    @Override
    public PaymentAccountEntity getPaymentAccountById(
            Long paymentAccountId
    ) {
        // Get the payment account by its id
        Optional<PaymentAccountEntity> optionalPaymentAccount = paymentAccountRepository.findById(paymentAccountId);
        if(optionalPaymentAccount.isPresent()) {
            return optionalPaymentAccount.get();
        }
        throw new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_FOUND);
    }

    @Override
    public PaymentAccountListResponse getPaginationListPaymentAccount(
            Integer page, Integer size, String orderedBy, Boolean isAscending, String keyword
    ) {
        Long totalQuantity;
        Page<PaymentAccountEntity> paymentAccountPage;

        // Get ascending or descending sort
        Sort sort = Boolean.TRUE.equals(isAscending)
                ? Sort.by(orderedBy).ascending()
                : Sort.by(orderedBy).descending();

        try {
            paymentAccountPage = paymentAccountRepository.findByAccountNumberContainingIgnoreCase(
                    keyword, PageRequest.of(page - 1, size, sort));
            totalQuantity = paymentAccountPage.getTotalElements();
        }
        catch (Exception e){
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        List<PaymentAccountResponse> paymentAccountResponses = paymentAccountPage.stream()
                .map(PaymentAccountResponse::fromPaymentAccount)
                .toList();

        return PaymentAccountListResponse.builder()
                .paymentAccounts(paymentAccountResponses)
                .totalQuantity(totalQuantity)
                .build();
    }

    @Override
    public List<PaymentAccountEntity> searchPaymentAccountByAccountNumber(String accountNumber) {
        Optional<List<PaymentAccountEntity>> optionalPaymentAccount = Optional.ofNullable(paymentAccountRepository.searchPaymentAccountByAccountNumber(accountNumber));
        if(optionalPaymentAccount.isPresent()) {
            return optionalPaymentAccount.get();
        }
        throw new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_FOUND);
    }
    // Method to get a payment account by its account number
    @Override
    public PaymentAccountEntity getPaymentAccountByAccountNumber(
            String accountNumber
    ) {
        //get the payment account by the account number
        PaymentAccountEntity paymentAccount = paymentAccountRepository.getPaymentAccountByAccountNumber(accountNumber);
        if(paymentAccount != null) {
            return paymentAccount;
        }
        throw new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_FOUND);
    }

    // Method to get the default payment account for a customer
    @Override
    public PaymentAccountEntity getDefaultPaymentAccount(
            Long customerId
    ) {
        //find the default account of a customer
        Optional<PaymentAccountEntity> optionalPaymentAccount = paymentAccountRepository.getPaymentAccountByStatus(customerId, AccountStatus.DEFAULT);
        if(optionalPaymentAccount.isPresent()) {
            return optionalPaymentAccount.get();
        }
        throw new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_FOUND);
    }

    // Method to get all payment accounts for a specific customer
    @Override
    public List<PaymentAccountEntity> getPaymentAccountsByCustomerId(
            Long customerId
    ) {
        //find all payment accounts of a customer
        List<PaymentAccountEntity> paymentAccounts = paymentAccountRepository.getPaymentAccountsByCustomerId(customerId);
        if (paymentAccounts.isEmpty()) {
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_FOUND);
        }

        // Sort the payment accounts by balance in descending order
        paymentAccounts.sort(Comparator.comparing(PaymentAccountEntity::getCurrentBalance).reversed());

        return paymentAccounts;
    }

    // Method to insert pending transaction for adding money from BA to PA
    @Override
    public TransactionCustomerEntity insertTransactionForAddMoneyToPA(
            Long bankAccountId,
            Long paymentAccountId,
            Double amount
    ) {
        // Check paymentAccountId
        PaymentAccountEntity existingPaymentAccountReceiver = getPaymentAccountById(paymentAccountId);
        if(existingPaymentAccountReceiver == null){
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_FOUND);
        }

        // Insert new transaction for Add money
        TransactionRequest transactionAdd = new TransactionRequest();
        transactionAdd.setTransactionType("ADD_FROM_BA_TO_PA");
        transactionAdd.setAmountType("MONEY");
        transactionAdd.setAmount(amount);
        transactionAdd.setTransactionRemark("Add money from associated BA to PA");
        transactionAdd.setSenderId(bankAccountId);
        transactionAdd.setTransactionSenderType("BANK_ACCOUNT");
        transactionAdd.setReceiverId(paymentAccountId);
        transactionAdd.setTransactionReceiverType("PAYMENT_ACCOUNT");

        // Find customer own this PA
        CustomerEntity existingCustomer = existingPaymentAccountReceiver.getCustomer();

        Long receiverId = 0L;

        if ("BANK_ACCOUNT".equals(transactionAdd.getTransactionReceiverType().toString())) {
            CustomerEntity receiver = bankAccountRepository.findById(transactionAdd.getReceiverId()).get().getPaymentAccount().getCustomer();
            receiverId = receiver != null ? receiver.getId() : 0L;
        } else if ("PAYMENT_ACCOUNT".equals(transactionAdd.getTransactionReceiverType().toString())) {
            CustomerEntity receiver = paymentAccountRepository.findById(transactionAdd.getReceiverId()).get().getCustomer();
            receiverId = receiver != null ? receiver.getId() : 0L;
        }


        // Create to get response
        return transactionService.insertPendingTransaction(transactionAdd, existingCustomer,receiverId);
    }

    // Method to add money from bank account to payment account
    @Transactional(rollbackFor = {Exception.class, Throwable.class})
    @Override
    public PaymentAccountEntity addMoneyToPaymentAccount(
            Long paymentAccountId,
            String bankName,
            BankAccountRequest bankAccountRequest,
            AmountOperationRequest amountDTO
    ) {
        // Request top-up money
        double amountTopUp = amountDTO.getAmount();

        // Get existing payment account
        PaymentAccountEntity existingPaymentAccountAddMoney = getPaymentAccountById(paymentAccountId);
        double paCurrentBalance = existingPaymentAccountAddMoney.getCurrentBalance(); // Current balance of PaymentAcc

        // Check if bank account exist
        BankAccountEntity existingBankAccount = bankAccountServiceImpl.checkBankAccountExist(bankAccountRequest, bankName);
        double baCurrentBalance = existingBankAccount.getCurrentBalance(); // Current balance of BankAcc

        // Decrease current balance in bankAcc
        double decreaseMoneyInBA = baCurrentBalance - amountTopUp;
        existingBankAccount.setCurrentBalance(decreaseMoneyInBA);
        bankAccountRepository.save(existingBankAccount);

        // Increase and update current balance in paymentAcc
        double increaseMoneyInPA = paCurrentBalance + amountTopUp;
        existingPaymentAccountAddMoney.setCurrentBalance(increaseMoneyInPA);

        return paymentAccountRepository.save(existingPaymentAccountAddMoney);
    }

    // Method to withdraw money from a payment account to bank account
    @Transactional(rollbackFor = {Exception.class, Throwable.class})
    @Override
    public Map<String, Object> withdrawFromPaymentAccount(
            Long paymentAccountId,
            String bankName,
            BankAccountRequest bankAccountRequest,
            AmountOperationRequest amountDTO
    ) {
        // Get existing payment account
        PaymentAccountEntity existingPaymentAccountWithdraw = getPaymentAccountById(paymentAccountId);
        double paCurrentBalance = existingPaymentAccountWithdraw.getCurrentBalance(); // Current balance of PaymentAcc

        // Check if bank account exist
        BankAccountEntity existingBankAccount = bankAccountServiceImpl.checkBankAccountExist(bankAccountRequest, bankName);
        double baCurrentBalance = existingBankAccount.getCurrentBalance(); // Current balance of BankAcc

        // Insert new transaction
        TransactionRequest transactionWithdraw = new TransactionRequest();
        transactionWithdraw.setTransactionType("WITHDRAW_FROM_PA_TO_BA");
        transactionWithdraw.setAmountType("MONEY");
        transactionWithdraw.setAmount(amountDTO.getAmount());
        transactionWithdraw.setTransactionRemark("Withdraw money from PA to associated BA");
        transactionWithdraw.setSenderId(existingPaymentAccountWithdraw.getId());
        transactionWithdraw.setTransactionSenderType("PAYMENT_ACCOUNT");
        transactionWithdraw.setReceiverId(existingBankAccount.getId());
        transactionWithdraw.setTransactionReceiverType("BANK_ACCOUNT");

        // Find customer own this PA
        CustomerEntity existingCustomer = existingPaymentAccountWithdraw.getCustomer();
        Long receiverId = 0L;

        if ("BANK_ACCOUNT".equals(transactionWithdraw.getTransactionReceiverType().toString())) {
            CustomerEntity receiver = bankAccountRepository.findById(transactionWithdraw.getReceiverId()).get().getPaymentAccount().getCustomer();
            receiverId = receiver != null ? receiver.getId() : 0L;
        } else if ("PAYMENT_ACCOUNT".equals(transactionWithdraw.getTransactionReceiverType().toString())) {
            CustomerEntity receiver = paymentAccountRepository.findById(transactionWithdraw.getReceiverId()).get().getCustomer();
            receiverId = receiver != null ? receiver.getId() : 0L;
        }

        // Create to get response
        TransactionCustomerEntity insertedTransactionCustomer = transactionService
                .insertPendingTransaction(transactionWithdraw, existingCustomer,receiverId);

        // Compare request withdraw money with current balance in paymentAcc
        if(amountDTO.getAmount() > paCurrentBalance){
            throw new AppException(ErrorCode.WITHDRAW_MONEY_EXCEED);
        }

        // Increase and update current balance in bankAcc
        double increaseMoneyInBA = baCurrentBalance + amountDTO.getAmount();
        existingBankAccount.setCurrentBalance(increaseMoneyInBA);
        bankAccountRepository.save(existingBankAccount);

        // Decrease current balance in paymentAcc
        double decreaseMoneyInPA = paCurrentBalance - amountDTO.getAmount();
        existingPaymentAccountWithdraw.setCurrentBalance(decreaseMoneyInPA);

        // Update datetime and status -> save
        TransactionEntity completeWithdraw = insertedTransactionCustomer.getTransactionCustomerKey().getTransaction();
        completeWithdraw.setTransactionDateTime(DateTimeUtils.getVietnamCurrentDateTime());
        completeWithdraw.setTransactionStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(completeWithdraw);

        // Save existingPaymentAccountWithdraw
        paymentAccountRepository.save(existingPaymentAccountWithdraw);

        // Build response
        Map<String, Object> completedWithdrawMoneyFromPAToBA = new HashMap<>();
        completedWithdrawMoneyFromPAToBA.put("completeTransaction", TransactionResponse.fromTransaction(completeWithdraw));
        completedWithdrawMoneyFromPAToBA.put("paymentAccount", PaymentAccountResponse.fromPaymentAccount(existingPaymentAccountWithdraw));
        completedWithdrawMoneyFromPAToBA.put("bankAccount", BankAccountResponse.fromBankAccount(existingBankAccount));

        return completedWithdrawMoneyFromPAToBA;
    }

    // Method to update a payment account by ID
    @Override
    public PaymentAccountEntity updatePaymentAccount(
            Long paymentAccountId,
            PaymentAccountRequest paymentAccountRequest
    ) {
        // Check if payment account valid
        isPaymentAccountRequestValid(paymentAccountRequest);

        // Find the payment account entity by ID and update its fields
        PaymentAccountEntity updatePaymentAccount = getPaymentAccountById(paymentAccountId);
        updatePaymentAccount.setAccountNumber(paymentAccountRequest.getAccountNumber());
        if(!paymentAccountRequest.getAccountStatus().equals(AccountStatus.DEFAULT)){
            updatePaymentAccount.setAccountStatus(AccountStatus.valueOf(paymentAccountRequest.getAccountStatus()));
        }
        updatePaymentAccount.setAccountType(AccountType.valueOf(paymentAccountRequest.getAccountType()));
        updatePaymentAccount.setCurrentBalance(paymentAccountRequest.getCurrentBalance());
        updatePaymentAccount.setRewardPoint(paymentAccountRequest.getRewardPoint());

        return paymentAccountRepository.save(updatePaymentAccount);
    }

    // Method to delete a payment account by ID
    @Override
    public void deletePaymentAccountById(
            Long paymentAccountId
    ) {
        // Find the payment account via paymentAccountId
        PaymentAccountEntity deletePaymentAccountEntity = getPaymentAccountById(paymentAccountId);

        // Check if payment account is DEFAULT -> cannot delete
        if(deletePaymentAccountEntity.getAccountStatus().equals(AccountStatus.DEFAULT)){
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_DEFAULT);
        }

        // Check if payment account have linked to any bank accounts
        List<BankAccountEntity> bankAccountEntityList = bankAccountRepository.findByPaymentAccount(deletePaymentAccountEntity);
        if(!bankAccountEntityList.isEmpty()){
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_LINKED_BANK_ACCOUNT);
        }

        // If not linked or have been removed linked successfully -> Enable to delete payment account
        paymentAccountRepository.delete(deletePaymentAccountEntity);
    }

    // Method to validate a paymentAccountRequest
    private void isPaymentAccountRequestValid(PaymentAccountRequest paymentAccountRequest){
        // Get existing payment account by the account number
        // Check if account number exist (not same customer) -> wrong
        PaymentAccountEntity existingPaymentAccount = paymentAccountRepository.getPaymentAccountByAccountNumber(paymentAccountRequest.getAccountNumber());
        if(existingPaymentAccount != null) {
            if(!existingPaymentAccount.getCustomer().getId().equals(paymentAccountRequest.getCustomerId())){
                throw new AppException(ErrorCode.PAYMENT_ACCOUNT_EXISTS);
            }
        }

        // Check if account type in AccountType class
        if(!ValidationUtils.isValidEnum(paymentAccountRequest.getAccountType(), AccountType.class)){
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_TYPE_INVALID);
        }

        // Check balance
        if(paymentAccountRequest.getCurrentBalance() < 0.0){
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_INVALID);
        }

        // Check reward point
        if(paymentAccountRequest.getRewardPoint() < 0){
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_INVALID);
        }
    }
}
