package com.example.onlinebankingapp.services.SavingAccount;

import com.example.onlinebankingapp.dtos.requests.SavingAccountRequest;
import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionRequest;
import com.example.onlinebankingapp.dtos.responses.SavingAccount.SavingAccountListResponse;
import com.example.onlinebankingapp.dtos.responses.SavingAccount.SavingAccountResponse;
import com.example.onlinebankingapp.entities.*;
import com.example.onlinebankingapp.enums.AccountStatus;
import com.example.onlinebankingapp.enums.AccountType;
import com.example.onlinebankingapp.enums.RewardType;
import com.example.onlinebankingapp.enums.TransactionStatus;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import com.example.onlinebankingapp.repositories.PaymentAccountRepository;
import com.example.onlinebankingapp.repositories.SavingAccountRepository;
import com.example.onlinebankingapp.repositories.TransactionRepository;
import com.example.onlinebankingapp.services.InterestRate.InterestRateService;
import com.example.onlinebankingapp.services.PaymentAccount.PaymentAccountService;
import com.example.onlinebankingapp.services.Transaction.TransactionService;
import com.example.onlinebankingapp.utils.DateTimeUtils;
import com.example.onlinebankingapp.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SavingAccountServiceImpl implements SavingAccountService {
    private final SavingAccountRepository savingAccountRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final TransactionRepository transactionRepository;

    private final PaymentAccountService paymentAccountService;
    private final InterestRateService interestRateService;
    private static final Double zeroAmount = 0.0;

    private TransactionService transactionService;

    @Autowired
    public void setTransactionService(@Lazy TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Method to insert a new saving account
    @Override
    public SavingAccountEntity insertSavingAccount(
            SavingAccountRequest savingAccountRequest
    ) {
        // Validate saving account request
        isSavingAccountRequestValid(savingAccountRequest);

        // Retrieve payment account and interest rate references
        PaymentAccountEntity existingPaymentAccount = paymentAccountService.getPaymentAccountById(savingAccountRequest.getPaymentAccountId());
        InterestRateEntity existingInterestRate = interestRateService.getInterestRateById(savingAccountRequest.getInterestRateId());

        // Calculate the dateClose based on interest rate term
        LocalDateTime dateClosed = DateTimeUtils.getVietnamCurrentDateTime().plusMonths(existingInterestRate.getTerm());

        // Insert a new saving account entity
        SavingAccountEntity newSavingAccountEntity = SavingAccountEntity.builder()
                .accountNumber(generateRandomSavingAccountNumber())
                .dateClosed(dateClosed)
                .savingInitialAmount(zeroAmount)
                .paymentAccount(existingPaymentAccount)
                .interestRate(existingInterestRate)
                .build();

        return savingAccountRepository.save(newSavingAccountEntity);
    }

    // Method to insert new saving account used by employee
    @Override
    public SavingAccountEntity insertSavingAccountByEmployee(
            SavingAccountRequest savingAccountRequest
    ) {
        // Validate saving account request
        isSavingAccountRequestValid(savingAccountRequest);

        // Retrieve payment account and interest rate references
        PaymentAccountEntity existingPaymentAccount = paymentAccountService.getPaymentAccountById(savingAccountRequest.getPaymentAccountId());
        InterestRateEntity existingInterestRate = interestRateService.getInterestRateById(savingAccountRequest.getInterestRateId());

        // Calculate the dateClose based on interest rate term
        LocalDateTime dateClosed = DateTimeUtils.getVietnamCurrentDateTime().plusMonths(existingInterestRate.getTerm());

        // Insert a new saving account entity
        SavingAccountEntity newSavingAccountEntity = SavingAccountEntity.builder()
                .accountNumber(generateRandomSavingAccountNumber())
                .accountType(AccountType.valueOf(savingAccountRequest.getAccountType()))
                .dateClosed(dateClosed)
                .savingInitialAmount(savingAccountRequest.getSavingInitialAmount())
                .savingCurrentAmount(savingAccountRequest.getSavingInitialAmount())
                .paymentAccount(existingPaymentAccount)
                .interestRate(existingInterestRate)
                .build();

        existingPaymentAccount.setCurrentBalance(existingPaymentAccount.getCurrentBalance() - savingAccountRequest.getSavingInitialAmount());
        paymentAccountRepository.save(existingPaymentAccount);

        return savingAccountRepository.save(newSavingAccountEntity);
    }

    // Method to insert pending transaction for add money from PA to SA
    @Override
    public TransactionCustomerEntity insertTransactionForAddMoneyToSA(
            Long paymentAccountId,
            Long savingAccountId,
            Double amount
    ) {
        // Check paymentAccountId
        PaymentAccountEntity existingPaymentAccountSender = paymentAccountService.getPaymentAccountById(paymentAccountId);

        // Insert new transaction for add money to existing SA from PA
        TransactionRequest transactionAddToSA = new TransactionRequest();
        transactionAddToSA.setTransactionType("ADD_FROM_PA_TO_SA");
        transactionAddToSA.setAmountType("MONEY");
        transactionAddToSA.setAmount(amount);
        transactionAddToSA.setTransactionRemark("Add money from PA to SA");
        transactionAddToSA.setSenderId(paymentAccountId);
        transactionAddToSA.setTransactionSenderType("PAYMENT_ACCOUNT");
        transactionAddToSA.setReceiverId(savingAccountId);
        transactionAddToSA.setTransactionReceiverType("SAVING_ACCOUNT");

        // Find customer own this PA
        CustomerEntity existingCustomer = existingPaymentAccountSender.getCustomer();

        // Create to get response
        return transactionService.insertPendingTransaction(transactionAddToSA, existingCustomer,0L);
    }

    @Override
    public SavingAccountEntity addMoneyToSavingAccount(
            Long savingAccountId,
            TransactionEntity existingTransaction
    ) {
        // Get existingSavingAccount
        SavingAccountEntity existingSavingAccount = getSavingAccountById(savingAccountId);

        // Get add money to SA
        double amountAddToSA = existingTransaction.getAmount();

        // Get senderPaymentAccount
        PaymentAccountEntity senderPaymentAccount = existingSavingAccount.getPaymentAccount();

        senderPaymentAccount.setCurrentBalance(senderPaymentAccount.getCurrentBalance() - amountAddToSA);
        paymentAccountRepository.save(senderPaymentAccount);

        existingSavingAccount.setSavingInitialAmount(existingSavingAccount.getSavingCurrentAmount() + amountAddToSA);
        existingSavingAccount.setSavingCurrentAmount(existingSavingAccount.getSavingCurrentAmount() + amountAddToSA);
        savingAccountRepository.save(existingSavingAccount);

        return existingSavingAccount;
    }

    // Method to withdraw balance from SA to PA
    @Transactional(rollbackFor = {Exception.class, Throwable.class})
    @Override
    public SavingAccountEntity withdrawFromSavingAccount(
            Long savingAccountId
    ) {
        // Get existingSavingAccount
        SavingAccountEntity existingSavingAccount = getSavingAccountById(savingAccountId);

        // Check if the account is active or not
        if (existingSavingAccount.getAccountStatus().equals(AccountStatus.INACTIVE)) {
            throw new AppException(ErrorCode.INACTIVE_WITHDRAW_INVALID);
        }

        // Get existingPaymentAccount
        PaymentAccountEntity existingPaymentAccount = existingSavingAccount.getPaymentAccount();
        Double transferCurrentAmount = existingSavingAccount.getSavingCurrentAmount();

        // Insert new transaction
        TransactionRequest transactionWithdraw = new TransactionRequest();
        transactionWithdraw.setTransactionType("WITHDRAW_FROM_SA_TO_PA");
        transactionWithdraw.setAmountType("MONEY");
        transactionWithdraw.setAmount(transferCurrentAmount);
        transactionWithdraw.setTransactionRemark("Withdraw money from SA to PA");
        transactionWithdraw.setSenderId(existingSavingAccount.getId());
        transactionWithdraw.setTransactionSenderType("SAVING_ACCOUNT");
        transactionWithdraw.setReceiverId(existingPaymentAccount.getId());
        transactionWithdraw.setTransactionReceiverType("PAYMENT_ACCOUNT");

        // Find customer own this PA
        CustomerEntity existingCustomer = existingPaymentAccount.getCustomer();

        // Create to get response
        TransactionCustomerEntity insertedTransactionCustomer = transactionService
                .insertPendingTransaction(transactionWithdraw, existingCustomer,0L);

        existingPaymentAccount.setCurrentBalance(existingPaymentAccount.getCurrentBalance() + transferCurrentAmount);
        paymentAccountRepository.save(existingPaymentAccount);

        // Update account status and balance
        existingSavingAccount.setAccountStatus(AccountStatus.INACTIVE);
        existingSavingAccount.setDateClosed(DateTimeUtils.getVietnamCurrentDateTime());
        existingSavingAccount.setSavingInitialAmount(zeroAmount);
        existingSavingAccount.setSavingCurrentAmount(zeroAmount);
        savingAccountRepository.save(existingSavingAccount);

        // Update datetime and status -> save
        TransactionEntity completeWithdraw = insertedTransactionCustomer.getTransactionCustomerKey().getTransaction();
        completeWithdraw.setTransactionDateTime(DateTimeUtils.getVietnamCurrentDateTime());
        completeWithdraw.setTransactionStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(completeWithdraw);

        return existingSavingAccount;
    }

    // Method to get a saving account by its id
    @Override
    public SavingAccountEntity getSavingAccountById(
            Long savingAccountId
    ) {
        // Retrieve the saving account by ID
        Optional<SavingAccountEntity> optionalSavingAccount = savingAccountRepository.findById(savingAccountId);
        if (optionalSavingAccount.isPresent()) {
            return optionalSavingAccount.get();
        }
        throw new AppException(ErrorCode.SAVING_ACCOUNT_NOT_FOUND);
    }

    // Method to get all saving accounts of a customer
    @Override
    public List<SavingAccountEntity> getSavingAccountsOfCustomer(
            Long customerId
    ) {
        // Retrieve payment accounts for the user
        List<PaymentAccountEntity> userPaymentAccountsList = paymentAccountService.getPaymentAccountsByCustomerId(customerId);
        List<SavingAccountEntity> userSavingAccountsList = new ArrayList<>();

        // For each payment account, find associated saving accounts
        for (PaymentAccountEntity paymentAccount : userPaymentAccountsList) {
            List<SavingAccountEntity> savingAccountsOfPaymentAccount
                    = savingAccountRepository.findSavingAccountEntitiesByPaymentAccount(paymentAccount);
            userSavingAccountsList.addAll(savingAccountsOfPaymentAccount);
        }

        return userSavingAccountsList;
    }

    // Method to get all saving accounts (admin/ staff)
    @Override
    public List<SavingAccountEntity> getAllSavingAccounts() {
        return savingAccountRepository.findAll();
    }

    @Override
    public SavingAccountListResponse getPaginationListSavingAccount(
            Integer page, Integer size, String orderedBy, Boolean isAscending, String keyword
    ) {
        Long totalQuantity;
        Page<SavingAccountEntity> savingAccountPage;

        // Get ascending or descending sort
        Sort sort = Boolean.TRUE.equals(isAscending)
                ? Sort.by(orderedBy).ascending()
                : Sort.by(orderedBy).descending();

        try {
            savingAccountPage = savingAccountRepository.findByAccountNumberContainingIgnoreCase(
                    keyword, PageRequest.of(page - 1, size, sort));
            totalQuantity = savingAccountPage.getTotalElements();
        }
        catch (Exception e){
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        List<SavingAccountResponse> savingAccountResponses = savingAccountPage.stream()
                .map(SavingAccountResponse::fromSavingAccount)
                .toList();

        return SavingAccountListResponse.builder()
                .savingAccounts(savingAccountResponses)
                .totalQuantity(totalQuantity)
                .build();
    }

    @Override
    public List<SavingAccountEntity> getSavingAccountsOfPaymentAccount(
            Long paymentAccountId
    ) {
        // Retrieve payment account of the user
        PaymentAccountEntity userPaymentAccount = paymentAccountService.getPaymentAccountById(paymentAccountId);
        return savingAccountRepository.findSavingAccountEntitiesByPaymentAccount(userPaymentAccount);
    }

    // Method to update saving account info
    // Only allow changes in saving initial amount (only for end of each month), PA entity and Interest rates entity
    @Override
    public SavingAccountEntity updateSavingAccountByEmployee(
            Long savingAccountId,
            SavingAccountRequest savingAccountRequest) {
        // Validate the update data
//        isSavingAccountRequestValid(savingAccountRequest);

        // Find the saving account entity by ID and update its fields
        Optional<SavingAccountEntity> optionalSavingAccount = savingAccountRepository.findById(savingAccountId);
        if(optionalSavingAccount.isEmpty()) {
            throw new AppException(ErrorCode.SAVING_ACCOUNT_NOT_FOUND);
        }

        PaymentAccountEntity existingPaymentAccount = paymentAccountService.getPaymentAccountById(savingAccountRequest.getPaymentAccountId());
        InterestRateEntity existingInterestRate = interestRateService.getInterestRateById(savingAccountRequest.getInterestRateId());

        SavingAccountEntity updateSavingAccount = optionalSavingAccount.get();
        updateSavingAccount.setAccountNumber(savingAccountRequest.getAccountNumber());
        updateSavingAccount.setAccountStatus(AccountStatus.valueOf(savingAccountRequest.getAccountStatus()));
        updateSavingAccount.setAccountType(AccountType.valueOf(savingAccountRequest.getAccountType()));
        updateSavingAccount.setSavingInitialAmount(savingAccountRequest.getSavingInitialAmount());
        updateSavingAccount.setSavingCurrentAmount(savingAccountRequest.getSavingCurrentAmount());
        updateSavingAccount.setPaymentAccount(existingPaymentAccount);
        updateSavingAccount.setInterestRate(existingInterestRate);

        // Save and return the updated saving account entity
        return savingAccountRepository.save(updateSavingAccount);
    }

    // Method to delete saving account by its ID
    @Override
    public void deleteSavingAccount(
            Long savingAccountId
    ) {
        // Find the saving account via savingAccountId
        SavingAccountEntity deleteSavingAccountEntity = getSavingAccountById(savingAccountId);

        // Check if saving account current amount has been withdrawn all
        if(deleteSavingAccountEntity.getSavingCurrentAmount() > 0){
            throw new AppException(ErrorCode.SAVING_AMOUNT_GREATER_THAN_ZERO);
        }

        savingAccountRepository.delete(deleteSavingAccountEntity);
    }

    // Method to check if the saving account term has ended
    @Override
    public boolean isEndOfTerm(
            SavingAccountEntity savingAccount
    ) {
        // Get the current date
        LocalDate todayLocalDate = LocalDate.now();

        // Calculate the end date of the term
        LocalDate endTermLocalDate = savingAccount.getDateClosed().toLocalDate();

        // Check if today is the end of the term or after the end date
        return (todayLocalDate.isEqual(endTermLocalDate) || todayLocalDate.isAfter(endTermLocalDate));
    }

    // Method to update the daily current balance of a saving account
    // Must control compound interest
    @Override
    public void updateDailyCurrentBalance(
            SavingAccountEntity savingAccount
    ) {
        // Get the interest rate entity for the account
        InterestRateEntity accountInterestRate = savingAccount.getInterestRate();

        // Get the current year and determine if it's a leap year (365 or 366 days)
        int yearDays = DateTimeUtils.getDaysInYear(LocalDateTime.now().getYear());

        // Calculate the daily interest rate
        // Formula: annual interest rate (already in %) / days in year
        double dailyInterestRate = accountInterestRate.getInterestRate() / 100 / yearDays;

        // Calculate the daily earned interest based on the current balance
        // Formula: daily earned interest = current balance * daily interest rate
        double dailyEarnedInterest = savingAccount.getSavingCurrentAmount() * dailyInterestRate;

        // Update the current balance with the daily earned interest
        savingAccount.setSavingCurrentAmount(savingAccount.getSavingCurrentAmount() + dailyEarnedInterest);
        savingAccountRepository.save(savingAccount);
    }

    // Method to deactivate a saving account and transfer its balance to a payment account
    @Override
    public void deactivateAndWithdrawCurrentAmountToPA(
            SavingAccountEntity savingAccount
    ) {
        // Get the associated payment account
        PaymentAccountEntity associatedPaymentAccount = savingAccount.getPaymentAccount();

        // Get the current balance of the saving account
        Double transferCurrentAmount = savingAccount.getSavingCurrentAmount();

        // Calculate the reward points based on the initial amount (assuming 1 point per 10000 units)
        int transferRewardPoints = (int) (savingAccount.getSavingInitialAmount() / 10000);

        // Set the saving account balance to zero
        savingAccount.setSavingCurrentAmount(zeroAmount);

        // Insert new transaction
        TransactionRequest transactionWithdraw = new TransactionRequest();
        transactionWithdraw.setTransactionType("WITHDRAW_FROM_SA_TO_PA");
        transactionWithdraw.setAmountType("MONEY");
        transactionWithdraw.setAmount(transferCurrentAmount);
        transactionWithdraw.setTransactionRemark("Term-ending auto withdraw money from SA to PA");
        transactionWithdraw.setSenderId(savingAccount.getId());
        transactionWithdraw.setTransactionSenderType("SAVING_ACCOUNT");
        transactionWithdraw.setReceiverId(associatedPaymentAccount.getId());
        transactionWithdraw.setTransactionReceiverType("PAYMENT_ACCOUNT");

        // Find customer own this PA
        CustomerEntity existingCustomer = associatedPaymentAccount.getCustomer();

        // Create to get response
        TransactionCustomerEntity insertedTransactionCustomer = transactionService
                .insertPendingTransaction(transactionWithdraw, existingCustomer,0L);

        // Transfer the balance and reward points to the payment account
        associatedPaymentAccount.setCurrentBalance(associatedPaymentAccount.getCurrentBalance() + transferCurrentAmount);
        associatedPaymentAccount.setRewardPoint(associatedPaymentAccount.getRewardPoint() + transferRewardPoints);
        paymentAccountRepository.save(associatedPaymentAccount);

        // Set the saving account status to inactive and record the closing date
        savingAccount.setAccountStatus(AccountStatus.INACTIVE);
        savingAccount.setSavingInitialAmount(zeroAmount);
        savingAccount.setSavingCurrentAmount(zeroAmount);
        savingAccountRepository.save(savingAccount);

        // Update datetime and status -> save
        TransactionEntity completeWithdraw = insertedTransactionCustomer.getTransactionCustomerKey().getTransaction();
        completeWithdraw.setTransactionDateTime(DateTimeUtils.getVietnamCurrentDateTime());
        completeWithdraw.setTransactionStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(completeWithdraw);
    }

    // Method to generate a random saving account number
    private String generateRandomSavingAccountNumber() {
        Random random = new Random();
        StringBuilder savingAccountNumber;
        // Generate a unique account number that does not already exist in the repository
        do {
            savingAccountNumber = new StringBuilder("SA"); // Prefix for saving account
            // Append 8 random digits to the prefix
            for (int i = 0; i < 8; i++) {
                Integer randomNum = random.nextInt(10); // Generate a random digit (0-9)
                savingAccountNumber.append(randomNum); // Append the digit
            }
        } while (savingAccountRepository.existsByAccountNumber(savingAccountNumber.toString()));

        // Return the generated account number
        return savingAccountNumber.toString();
    }

    // Method to validate a saving account request
    private void isSavingAccountRequestValid(
            SavingAccountRequest savingAccountRequest
    ) {
        // Validate empty/ blank/ null
        if(ValidationUtils.isNullOrEmptyOrBlank(savingAccountRequest.getSavingInitialAmount().toString())
                || ValidationUtils.isNullOrEmptyOrBlank(savingAccountRequest.getPaymentAccountId().toString())
                || ValidationUtils.isNullOrEmptyOrBlank(savingAccountRequest.getInterestRateId().toString())){
            throw new AppException(ErrorCode.SAVING_PARAMETER_MISSING);
        }

        // Check if payment account exist
        PaymentAccountEntity chosePaymentAccount = paymentAccountService.getPaymentAccountById(savingAccountRequest.getPaymentAccountId());

        // Check if payment account balance has enough money
        if(chosePaymentAccount.getCurrentBalance() < savingAccountRequest.getSavingInitialAmount()){
            throw new AppException(ErrorCode.BALANCE_AMOUNT_INVALID);
        }

        // Check if interest rate exist
        InterestRateEntity choseInterestRate = interestRateService
                .getInterestRateById(savingAccountRequest.getInterestRateId());

        // Get the initial amount from the request
        Double savingInitialAmount = savingAccountRequest.getSavingInitialAmount();

        // Validate that the initial amount is greater than 0
        if (savingInitialAmount <= 0) {
            throw new AppException(ErrorCode.SAVING_AMOUNT_SMALLER_THAN_ZERO);
        }

        // Compare required min_balance of interest rate term with saving initial amount
        if(savingInitialAmount < choseInterestRate.getMinBalance()){
            throw new AppException(ErrorCode.SAVING_AMOUNT_INITIAL_INVALID);
        }

        // Validate that the account type is a valid enum value
        if(!ValidationUtils.isValidEnum(savingAccountRequest.getAccountType(), AccountType.class)){
            throw new AppException(ErrorCode.SAVING_TYPE_INVALID);
        }
    }
}
