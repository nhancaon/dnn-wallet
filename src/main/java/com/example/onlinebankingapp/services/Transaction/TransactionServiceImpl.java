package com.example.onlinebankingapp.services.Transaction;

import com.example.onlinebankingapp.dtos.requests.AccountRewardRequest;
import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionToBA.TransactionToBAFromBARequest;
import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionToBA.TransactionToBAFromPARequest;
import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionRequest;
import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionToPA.TransactionToPAFromBARequest;
import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionToPA.TransactionToPAFromPARequest;
import com.example.onlinebankingapp.dtos.responses.BankAccount.BankAccountResponse;
import com.example.onlinebankingapp.dtos.responses.PaymentAccount.PaymentAccountResponse;
import com.example.onlinebankingapp.dtos.responses.Transaction.TransactionResponse;
import com.example.onlinebankingapp.dtos.responses.TransactionCustomer.TransactionCustomerListResponse;
import com.example.onlinebankingapp.dtos.responses.TransactionCustomer.TransactionCustomerResponse;
import com.example.onlinebankingapp.entities.*;
import com.example.onlinebankingapp.enums.*;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import com.example.onlinebankingapp.repositories.*;
import com.example.onlinebankingapp.services.BankAccount.BankAccountServiceImpl;
import com.example.onlinebankingapp.services.Customer.CustomerService;
import com.example.onlinebankingapp.services.PaymentAccount.PaymentAccountServiceImpl;
import com.example.onlinebankingapp.utils.DateTimeUtils;
import com.example.onlinebankingapp.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionCustomerRepository transactionCustomerRepository;
    private final RewardRepository rewardRepository;
    private final CustomerService customerService;
    private final PaymentAccountRepository paymentAccountRepository;
    private final BankAccountRepository bankAccountRepository;

    private final PaymentAccountServiceImpl paymentAccountServiceImpl;
    private final BankAccountServiceImpl bankAccountServiceImpl;

    public static Boolean defaultPAEnough = true;

    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);

    @Override
    public Map<String, Object> getUnexpiredPendingTransferToPA(
            CustomerEntity existingCustomer
    ) {
        Map<String, Object> transferToPA = new HashMap<>();

        // Find transaction and return
        // Transaction: PENDING, TRANSFER_MONEY, does not exceed 5 minutes
        LocalDateTime dateTimeThreshold = DateTimeUtils.getVietnamCurrentDateTime().minusMinutes(5);
        List<TransactionEntity> unexpiredPendingTransfers = transactionRepository
                .findUnexpiredPendingTransactions(TransactionType.TRANSFER_MONEY,
                        TransactionStatus.PENDING, dateTimeThreshold);

        if(unexpiredPendingTransfers.isEmpty()){
            throw new AppException(ErrorCode.UNEXPIRED_TRANSFER_NOT_EXIST);
        }
        if(unexpiredPendingTransfers.size() > 1){
            throw new AppException(ErrorCode.UNEXPIRED_TRANSFER_TO_PA_EXIST);
        }

        // Add the transaction entity to the map with a key
        TransactionEntity unexpiredPendingTransfer = unexpiredPendingTransfers.getFirst();
        if(!unexpiredPendingTransfer.getTransactionReceiverType().equals(TransactionReceiverType.PAYMENT_ACCOUNT)){
            throw new AppException(ErrorCode.UNEXPIRED_TRANSFER_INVALID);
        }

        transferToPA.put("transaction", TransactionResponse.fromTransaction(unexpiredPendingTransfer));

        // Find payment account and return
        PaymentAccountEntity receiverPendingTransfer = paymentAccountServiceImpl
                .getPaymentAccountById(unexpiredPendingTransfer.getReceiverId());
        transferToPA.put("receiverPaymentAccount", PaymentAccountResponse.fromPaymentAccount(receiverPendingTransfer));

        transferToPA.put("receiver_name", receiverPendingTransfer.getCustomer().getName());
        return transferToPA;
    }

    @Override
    public Map<String, Object> getUnexpiredPendingTransferToBA(
            CustomerEntity existingCustomer
    ) {
        Map<String, Object> transferToBA = new HashMap<>();

        // Find transaction and return
        // Transaction: PENDING, TRANSFER_MONEY, does not exceed 5 minutes
        LocalDateTime dateTimeThreshold = DateTimeUtils.getVietnamCurrentDateTime().minusMinutes(5);
        List<TransactionEntity> unexpiredPendingTransfers = transactionRepository
                .findUnexpiredPendingTransactions(TransactionType.TRANSFER_MONEY,
                        TransactionStatus.PENDING, dateTimeThreshold);

        if(unexpiredPendingTransfers.isEmpty()){
            throw new AppException(ErrorCode.UNEXPIRED_TRANSFER_NOT_EXIST);
        }
        if(unexpiredPendingTransfers.size() > 1){
            throw new AppException(ErrorCode.UNEXPIRED_TRANSFER_TO_BA_EXIST);
        }

        // Add the transaction entity to the map with a key
        TransactionEntity unexpiredPendingTransfer = unexpiredPendingTransfers.getFirst();
        if(!unexpiredPendingTransfer.getTransactionReceiverType().equals(TransactionReceiverType.BANK_ACCOUNT)){
            throw new AppException(ErrorCode.UNEXPIRED_TRANSFER_INVALID);
        }

        transferToBA.put("transaction", TransactionResponse.fromTransaction(unexpiredPendingTransfer));

        // Find payment account and return
        BankAccountEntity receiverPendingTransfer = bankAccountServiceImpl
                .getBankAccountById(unexpiredPendingTransfer.getReceiverId());
        transferToBA.put("receiverBankAccount", BankAccountResponse.fromBankAccount(receiverPendingTransfer));

        return transferToBA;
    }

    // AIM: Trans 1, 2, 3, 4 -> Transfer MONEY
    // Force to check transaction amount in FE
    // Always prior to transfer via default PA (e-wallet)
    // Insert a new PENDING transaction to either PA or BA
    @Override
    public TransactionCustomerEntity checkDefaultPAForTransfer(
            CustomerEntity existingCustomer,
            Long oldUnexpiredPendingTransactionId,
            TransactionRequest transactionRequest
    ) {
        // Get DEFAULT payment account of existingCustomer
        PaymentAccountEntity defaultPaymentAccount = paymentAccountServiceImpl.getDefaultPaymentAccount(existingCustomer.getId());

        // Response for FE to hide default PA option
        if(transactionRequest.getAmount() > defaultPaymentAccount.getCurrentBalance()){
            defaultPAEnough = false;
        }
        else {
            defaultPAEnough = true;
        }

        Long receiverId = 0L;

        if ("BANK_ACCOUNT".equals(transactionRequest.getTransactionReceiverType().toString())) {
            Optional<BankAccountEntity> bankAccountOpt = bankAccountRepository.findById(transactionRequest.getReceiverId());
            CustomerEntity receiver = bankAccountOpt
                    .map(BankAccountEntity::getPaymentAccount)
                    .map(PaymentAccountEntity::getCustomer)
                    .orElse(null);
            receiverId = receiver != null ? receiver.getId() : 0L;
        } else if ("PAYMENT_ACCOUNT".equals(transactionRequest.getTransactionReceiverType().toString())) {
            CustomerEntity receiver = paymentAccountRepository.findById(transactionRequest.getReceiverId()).get().getCustomer();
            receiverId = receiver != null ? receiver.getId() : 0L;
        }

        // Get/ Create new aiming transaction
        TransactionCustomerEntity aimTransactionCustomer = new TransactionCustomerEntity();

        // Check transfer money in oldUnexpiredPendingTransaction
        if(oldUnexpiredPendingTransactionId != null){
            // No need to delete pending transaction of customer exceeded 5 minutes from initialization
            // When check SMS otp handle already
            TransactionEntity oldUnexpiredPendingTransaction = getTransactionById(oldUnexpiredPendingTransactionId);
            oldUnexpiredPendingTransaction.setTransactionDateTime(DateTimeUtils.getVietnamCurrentDateTime());
            transactionRepository.save(oldUnexpiredPendingTransaction);

            TransactionCustomerEntity.TransactionCustomer oldTransactionCustomerKey =
                    new TransactionCustomerEntity.TransactionCustomer(oldUnexpiredPendingTransaction, existingCustomer, receiverId);

            // Check a transaction of a customer exist
            if(!transactionCustomerRepository.existsTransactionCustomerEntityByTransactionCustomerKey(oldTransactionCustomerKey)){
                throw new AppException(ErrorCode.TRANSACTION_OF_CUSTOMER_NOT_FOUND);
            }

            aimTransactionCustomer.setTransactionCustomerKey(oldTransactionCustomerKey);
        }
        // Check transfer money in newTransaction
        else{
            // Delete pending transaction of customer
            // Exceed 5 minutes from initialization
            deleteExpiredPendingTransaction(existingCustomer, TransactionType.TRANSFER_MONEY);

            // Insert a new PENDING transaction from DEFAULT PA
            aimTransactionCustomer = insertPendingTransaction(transactionRequest, existingCustomer,receiverId);
        }

        return aimTransactionCustomer;
    }
    
    @Override
    public TransactionCustomerEntity insertPendingTransaction(
            TransactionRequest transactionRequest,
            CustomerEntity customerEntity,
            Long receiverId
    ) {
        if(!ValidationUtils.isValidEnum(transactionRequest.getTransactionType().toUpperCase(), TransactionType.class)){
            throw new AppException(ErrorCode.INVALID_TRANSACTION_TYPE);
        }

        if(!ValidationUtils.isValidEnum(transactionRequest.getAmountType().toUpperCase(), AmountType.class)){
            throw new AppException(ErrorCode.INVALID_AMOUNT_TYPE);
        }

        if(!ValidationUtils.isValidEnum(transactionRequest.getTransactionSenderType().toUpperCase(), TransactionSenderType.class)){
            throw new AppException(ErrorCode.INVALID_TRANSACTION_SENDER_TYPE);
        }

        if(!ValidationUtils.isValidEnum(transactionRequest.getTransactionReceiverType().toUpperCase(), TransactionReceiverType.class)){
            throw new AppException(ErrorCode.INVALID_TRANSACTION_RECEIVER_TYPE);
        }

        // Check data for inserting new PENDING transaction
        TransactionType transactionType = TransactionType.valueOf(transactionRequest.getTransactionType().toUpperCase());
        AmountType amountType = AmountType.valueOf(transactionRequest.getAmountType().toUpperCase());
        TransactionSenderType transactionSenderType = TransactionSenderType.valueOf(transactionRequest.getTransactionSenderType().toUpperCase());
        TransactionReceiverType transactionReceiverType = TransactionReceiverType.valueOf(transactionRequest.getTransactionReceiverType().toUpperCase());
        
        // Insert a new PENDING transaction
        TransactionEntity newPendingTransaction = TransactionEntity.builder()
                .transactionType(transactionType)
                .amountType(amountType)
                .amount(transactionRequest.getAmount())
                .transactionRemark(transactionRequest.getTransactionRemark())
                .senderId(transactionRequest.getSenderId())
                .transactionSenderType(transactionSenderType)
                .receiverId(transactionRequest.getReceiverId())
                .transactionReceiverType(transactionReceiverType)
                .build();
        transactionRepository.save(newPendingTransaction);

        // Create a relationship key for the transaction-customer relationship
        TransactionCustomerEntity.TransactionCustomer transactionCustomerKey =
                new TransactionCustomerEntity.TransactionCustomer(newPendingTransaction, customerEntity, receiverId);

        // Create a new TransactionCustomerEntity
        TransactionCustomerEntity newTransactionCustomer = TransactionCustomerEntity.builder()
                .transactionCustomerKey(transactionCustomerKey)
                .build();

        return transactionCustomerRepository.save(newTransactionCustomer);
    }

    // Method for choosing Payment Account to transfer money to Payment Account
    @Override
    public TransactionEntity transferToPAFromPA(
            long customerId,
            TransactionToPAFromPARequest toPAFromPARequest
    ) {
        // Check if transaction exist via transactionId
        Optional<TransactionEntity> optionalTransaction = transactionRepository
                .findById(toPAFromPARequest.getTransactionId());
        if (optionalTransaction.isEmpty()){
            throw new AppException(ErrorCode.TRANSACTION_NOT_FOUND);
        }

        // Check if customer has this transaction
        boolean existingTransactionCustomer = checkTransactionCustomerExist(customerId, toPAFromPARequest.getTransactionId());
        if(!existingTransactionCustomer){
            throw new AppException(ErrorCode.TRANSACTION_OF_CUSTOMER_NOT_FOUND);
        }

        // Check if customer has that payment account
        PaymentAccountEntity existingSenderPaymentAccount = paymentAccountServiceImpl.getPaymentAccountById(toPAFromPARequest.getSenderPaymentAccountId());

        // Check if payment account existing in DB but not of that customer
        PaymentAccountEntity existingReceiverPaymentAccount = paymentAccountServiceImpl.getPaymentAccountById(toPAFromPARequest.getReceiverPaymentAccountId());
        if(existingReceiverPaymentAccount.getCustomer().getId().equals(customerId)){
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_INVALID);
        }

        // Update money source
        TransactionEntity updateTransaction = updateTransactionEntity(optionalTransaction,
                existingSenderPaymentAccount,
                null,
                existingReceiverPaymentAccount,
                null,
                TransactionType.TRANSFER_MONEY,
                TransactionSenderType.PAYMENT_ACCOUNT,
                TransactionReceiverType.PAYMENT_ACCOUNT);
        return transactionRepository.save(updateTransaction);
    }

    // Method for choosing Bank Account to transfer money to Payment Account
    @Override
    public TransactionEntity transferToPAFromBA(
            long customerId,
            TransactionToPAFromBARequest toPAFromBARequest
    ) {
        // Check if transaction exist via transactionId
        Optional<TransactionEntity> optionalTransaction = transactionRepository
                .findById(toPAFromBARequest.getTransactionId());
        if (optionalTransaction.isEmpty()){
            throw new AppException(ErrorCode.TRANSACTION_NOT_FOUND);
        }

        // Check if customer has this transaction
        boolean existingTransactionCustomer = checkTransactionCustomerExist(customerId, toPAFromBARequest.getTransactionId());
        if(!existingTransactionCustomer){
            throw new AppException(ErrorCode.TRANSACTION_OF_CUSTOMER_NOT_FOUND);
        }

        // Check if customer has that associated bank account
        BankAccountEntity existingSenderBankAccount = bankAccountServiceImpl.getBankAccountById(toPAFromBARequest.getSenderBankAccountId());

        // Check if payment account existing in DB but not of that customer
        PaymentAccountEntity existingReceiverPaymentAccount = paymentAccountServiceImpl.getPaymentAccountById(toPAFromBARequest.getReceiverPaymentAccountId());
        if(existingReceiverPaymentAccount.getCustomer().getId().equals(customerId)){
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_INVALID);
        }

        // Update money source
        TransactionEntity updateTransaction = updateTransactionEntity(optionalTransaction,
                null,
                existingSenderBankAccount,
                existingReceiverPaymentAccount,
                null,
                TransactionType.TRANSFER_MONEY,
                TransactionSenderType.BANK_ACCOUNT,
                TransactionReceiverType.PAYMENT_ACCOUNT);
        return transactionRepository.save(updateTransaction);
    }

    // Method for choosing Payment Account to transfer money to Bank Account
    @Override
    public TransactionEntity transferToBAFromPA(
            long customerId,
            TransactionToBAFromPARequest toBAFromPARequest
    ) {
        // Check if transaction exist via transactionId
        Optional<TransactionEntity> optionalTransaction = transactionRepository
                .findById(toBAFromPARequest.getTransactionId());
        if (optionalTransaction.isEmpty()){
            throw new AppException(ErrorCode.TRANSACTION_NOT_FOUND);
        }

        // Check if customer has this transaction
        boolean existingTransactionCustomer = checkTransactionCustomerExist(customerId, toBAFromPARequest.getTransactionId());
        if(!existingTransactionCustomer){
            throw new AppException(ErrorCode.TRANSACTION_OF_CUSTOMER_NOT_FOUND);
        }

        // Check if customer has that payment account
        PaymentAccountEntity existingSenderPaymentAccount = paymentAccountServiceImpl.getPaymentAccountById(toBAFromPARequest.getSenderPaymentAccountId());

        // Check if receiver BA exist in DB
        BankAccountEntity existingReceiverBankAccount = bankAccountServiceImpl.getBankAccountById(toBAFromPARequest.getReceiverBankAccountId());

        // Update money source
        TransactionEntity updateTransaction = updateTransactionEntity(optionalTransaction,
                existingSenderPaymentAccount,
                null,
                null,
                existingReceiverBankAccount,
                TransactionType.TRANSFER_MONEY,
                TransactionSenderType.PAYMENT_ACCOUNT,
                TransactionReceiverType.BANK_ACCOUNT);
        return transactionRepository.save(updateTransaction);
    }

    // Method for choosing Bank Account to transfer money to Bank Account
    @Override
    public TransactionEntity transferToBAFromBA(
            long customerId,
            TransactionToBAFromBARequest toBAFromBARequest
    ) {
        // Check if transaction exist via transactionId
        Optional<TransactionEntity> optionalTransaction = transactionRepository
                .findById(toBAFromBARequest.getTransactionId());
        if (optionalTransaction.isEmpty()){
            throw new AppException(ErrorCode.TRANSACTION_NOT_FOUND);
        }

        // Check if customer has this transaction
        boolean existingTransactionCustomer = checkTransactionCustomerExist(customerId, toBAFromBARequest.getTransactionId());
        if(!existingTransactionCustomer){
            throw new AppException(ErrorCode.TRANSACTION_OF_CUSTOMER_NOT_FOUND);
        }

        // Check if customer has that associated bank account
        BankAccountEntity existingSenderBankAccount = bankAccountServiceImpl.getBankAccountById(toBAFromBARequest.getSenderBankAccountId());

        // Check if receiver BA exist in DB
        BankAccountEntity existingReceiverBankAccount = bankAccountServiceImpl.getBankAccountById(toBAFromBARequest.getReceiverBankAccountId());

        // Update money source
        TransactionEntity updateTransaction = updateTransactionEntity(optionalTransaction,
                null,
                existingSenderBankAccount,
                null,
                existingReceiverBankAccount,
                TransactionType.TRANSFER_MONEY,
                TransactionSenderType.BANK_ACCOUNT,
                TransactionReceiverType.BANK_ACCOUNT);
        return transactionRepository.save(updateTransaction);
    }

    @NotNull
    private static TransactionEntity updateTransactionEntity(
            Optional<TransactionEntity> optionalTransaction,
            @Nullable PaymentAccountEntity existingSenderPaymentAccount,
            @Nullable BankAccountEntity existingSenderBankAccount,
            @Nullable PaymentAccountEntity existingReceiverPaymentAccount,
            @Nullable BankAccountEntity existingReceiverBankAccount,
            TransactionType transactionType,
            TransactionSenderType transactionSenderType,
            TransactionReceiverType transactionReceiverType
    ) {
        TransactionEntity updateTransaction = optionalTransaction.get();
        updateTransaction.setTransactionType(transactionType);
        updateTransaction.setAmountType(AmountType.MONEY);
        updateTransaction.setTransactionDateTime(DateTimeUtils.getVietnamCurrentDateTime());

        // Set sender based on which entity is provided
        if (existingSenderPaymentAccount != null) {
            updateTransaction.setSenderId(existingSenderPaymentAccount.getId());
        } else if (existingSenderBankAccount != null) {
            updateTransaction.setSenderId(existingSenderBankAccount.getId());
        } else {
            throw new AppException(ErrorCode.SENDER_INVALID);
        }

        // Set receiver based on which entity is provided
        if (existingReceiverPaymentAccount != null) {
            updateTransaction.setReceiverId(existingReceiverPaymentAccount.getId());
        } else if (existingReceiverBankAccount != null) {
            updateTransaction.setReceiverId(existingReceiverBankAccount.getId());
        } else {
            throw new AppException(ErrorCode.RECEIVER_INVALID);
        }

        updateTransaction.setTransactionSenderType(transactionSenderType);
        updateTransaction.setTransactionReceiverType(transactionReceiverType);
        return updateTransaction;
    }

    // Check all PENDING TRANSACTION for TRANSFER_MONEY
    // Expired in 5 minutes for DELETE
    @Override
    public TransactionCustomerEntity completeTransferMoney(
            CustomerEntity existingCustomer,
            TransactionEntity existingTransaction
    ) {
        // Change in PA and BA
        PaymentAccountEntity senderPaymentAccount, receiverPaymentAccount;
        BankAccountEntity senderBankAccount, receiverBankAccount;
        Double amountTransfer = existingTransaction.getAmount();

        // From PA to PA
        // No need to check balance of PA
        // Front end have implement that
        if(existingTransaction.getTransactionSenderType().equals(TransactionSenderType.PAYMENT_ACCOUNT)
                && existingTransaction.getTransactionReceiverType().equals(TransactionReceiverType.PAYMENT_ACCOUNT)){
            senderPaymentAccount = paymentAccountServiceImpl.getPaymentAccountById(existingTransaction.getSenderId());
            receiverPaymentAccount = paymentAccountServiceImpl.getPaymentAccountById(existingTransaction.getReceiverId());

            senderPaymentAccount.setCurrentBalance(senderPaymentAccount.getCurrentBalance() - amountTransfer);
            senderPaymentAccount.setRewardPoint(rewardPointForCompleteTransfer(amountTransfer));
            paymentAccountRepository.save(senderPaymentAccount);

            receiverPaymentAccount.setCurrentBalance(receiverPaymentAccount.getCurrentBalance() + amountTransfer);
            paymentAccountRepository.save(receiverPaymentAccount);
        }

        // From PA to BA
        if(existingTransaction.getTransactionSenderType().equals(TransactionSenderType.PAYMENT_ACCOUNT)
                && existingTransaction.getTransactionReceiverType().equals(TransactionReceiverType.BANK_ACCOUNT)){
            senderPaymentAccount = paymentAccountServiceImpl.getPaymentAccountById(existingTransaction.getSenderId());
            receiverBankAccount = bankAccountServiceImpl.getBankAccountById(existingTransaction.getReceiverId());

            senderPaymentAccount.setCurrentBalance(senderPaymentAccount.getCurrentBalance() - amountTransfer);
            senderPaymentAccount.setRewardPoint(rewardPointForCompleteTransfer(amountTransfer));
            paymentAccountRepository.save(senderPaymentAccount);

            receiverBankAccount.setCurrentBalance(receiverBankAccount.getCurrentBalance() + amountTransfer);
            bankAccountRepository.save(receiverBankAccount);
        }

        // From BA to PA
        if(existingTransaction.getTransactionSenderType().equals(TransactionSenderType.BANK_ACCOUNT)
                && existingTransaction.getTransactionReceiverType().equals(TransactionReceiverType.PAYMENT_ACCOUNT)){
            senderBankAccount = bankAccountServiceImpl.getBankAccountById(existingTransaction.getSenderId());
            receiverPaymentAccount = paymentAccountServiceImpl.getPaymentAccountById(existingTransaction.getReceiverId());

            // Check balance of associated BA
            if(senderBankAccount.getCurrentBalance() < existingTransaction.getAmount()){
                // Return error for that transaction
                failTransferMoney(existingCustomer, existingTransaction);
                throw new AppException(ErrorCode.BA_BALANCE_NOT_ENOUGH);
            }

            senderBankAccount.setCurrentBalance(senderBankAccount.getCurrentBalance() - amountTransfer);
            bankAccountRepository.save(senderBankAccount);

            // Get PA linked with senderBankAccount
            PaymentAccountEntity updateRewardPoint = senderBankAccount.getPaymentAccount();
            updateRewardPoint.setRewardPoint(rewardPointForCompleteTransfer(amountTransfer));
            paymentAccountRepository.save(updateRewardPoint);

            receiverPaymentAccount.setCurrentBalance(receiverPaymentAccount.getCurrentBalance() + amountTransfer);
            paymentAccountRepository.save(receiverPaymentAccount);
        }

        // From BA to BA
        if(existingTransaction.getTransactionSenderType().equals(TransactionSenderType.BANK_ACCOUNT)
                && existingTransaction.getTransactionReceiverType().equals(TransactionReceiverType.BANK_ACCOUNT)){
            senderBankAccount = bankAccountServiceImpl.getBankAccountById(existingTransaction.getSenderId());
            receiverBankAccount = bankAccountServiceImpl.getBankAccountById(existingTransaction.getReceiverId());

            // Check balance of associated BA
            if(senderBankAccount.getCurrentBalance() < existingTransaction.getAmount()){
                // Return error for that transaction
                failTransferMoney(existingCustomer, existingTransaction);
                throw new AppException(ErrorCode.BA_BALANCE_NOT_ENOUGH);
            }

            senderBankAccount.setCurrentBalance(senderBankAccount.getCurrentBalance() - amountTransfer);
            bankAccountRepository.save(senderBankAccount);

            // Get PA linked with senderBankAccount
            PaymentAccountEntity updateRewardPoint = senderBankAccount.getPaymentAccount();
            updateRewardPoint.setRewardPoint(rewardPointForCompleteTransfer(amountTransfer));
            paymentAccountRepository.save(updateRewardPoint);

            receiverBankAccount.setCurrentBalance(receiverBankAccount.getCurrentBalance() + amountTransfer);
            bankAccountRepository.save(receiverBankAccount);
        }

        // Delete pending TRANSFER_MONEY transaction of customer
        // Check pending TRANSFER_MONEY exceed 5 minutes
        deleteExpiredPendingTransaction(existingCustomer, TransactionType.TRANSFER_MONEY);

        Long receiverId = 0L;

        if ("BANK_ACCOUNT".equals(existingTransaction.getTransactionReceiverType().toString())) {
            Optional<BankAccountEntity> bankAccountOpt = bankAccountRepository.findById(existingTransaction.getReceiverId());
            CustomerEntity receiver = bankAccountOpt
                    .map(BankAccountEntity::getPaymentAccount)
                    .map(PaymentAccountEntity::getCustomer)
                    .orElse(null);
            receiverId = receiver != null ? receiver.getId() : 0L;
        } else if ("PAYMENT_ACCOUNT".equals(existingTransaction.getTransactionReceiverType().toString())) {
            CustomerEntity receiver = paymentAccountRepository.findById(existingTransaction.getReceiverId()).get().getCustomer();
            receiverId = receiver != null ? receiver.getId() : 0L;
        }

        // Return transaction of customer
        // Create a relationship key for the transaction-customer relationship
        TransactionCustomerEntity.TransactionCustomer transactionCustomerKey =
                new TransactionCustomerEntity.TransactionCustomer(existingTransaction, existingCustomer,receiverId);

        TransactionCustomerEntity completeTransactionCustomer = TransactionCustomerEntity.builder()
                .transactionCustomerKey(transactionCustomerKey)
                .build();

        // Check a transaction of a customer exist
        if(!transactionCustomerRepository.existsTransactionCustomerEntityByTransactionCustomerKey(transactionCustomerKey)){
            throw new AppException(ErrorCode.TRANSACTION_OF_CUSTOMER_NOT_FOUND);
        }

        // Change in transaction
        existingTransaction.setTransactionDateTime(DateTimeUtils.getVietnamCurrentDateTime());
        existingTransaction.setTransactionStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(existingTransaction);

        return completeTransactionCustomer;
    }

    // FAIL TRANSACTION WHEN:
    // Case 1: Choose associated BA as sender and its balance is not enough
    // Case 2: Exceed 3 times SMS OTP verification (in the interval '3 minutes' of the life of SMS OTP)
    // Case 3: When OTP expired
    @Override
    public void failTransferMoney(
            CustomerEntity existingCustomer,
            TransactionEntity existingTransaction
    ) {
        Long receiverId = 0L;

        if ("BANK_ACCOUNT".equals(existingTransaction.getTransactionReceiverType().toString())) {
            Optional<BankAccountEntity> bankAccountOpt = bankAccountRepository.findById(existingTransaction.getReceiverId());
            CustomerEntity receiver = bankAccountOpt
                    .map(BankAccountEntity::getPaymentAccount)
                    .map(PaymentAccountEntity::getCustomer)
                    .orElse(null);
            receiverId = receiver != null ? receiver.getId() : 0L;
        } else if ("PAYMENT_ACCOUNT".equals(existingTransaction.getTransactionReceiverType().toString())) {
            CustomerEntity receiver = paymentAccountRepository.findById(existingTransaction.getReceiverId()).get().getCustomer();
            receiverId = receiver != null ? receiver.getId() : 0L;
        }
        // Return transaction of customer
        // Create a relationship key for the transaction-customer relationship
        TransactionCustomerEntity.TransactionCustomer transactionCustomerKey = new TransactionCustomerEntity.TransactionCustomer(existingTransaction, existingCustomer,receiverId);

        // Check a transaction of a customer exist
        if(!transactionCustomerRepository.existsTransactionCustomerEntityByTransactionCustomerKey(transactionCustomerKey)){
            throw new AppException(ErrorCode.TRANSACTION_OF_CUSTOMER_NOT_FOUND);
        }

        // Change in transaction
        existingTransaction.setTransactionDateTime(DateTimeUtils.getVietnamCurrentDateTime());
        existingTransaction.setTransactionStatus(TransactionStatus.FAILED);
        transactionRepository.save(existingTransaction);

        // Delete pending TRANSFER_MONEY transaction of customer
        // Check pending TRANSFER_MONEY exceed 5 minutes
        deleteExpiredPendingTransaction(existingCustomer, TransactionType.TRANSFER_MONEY);
    }

    @Override
    public TransactionEntity completeAddMoneyFromBAToPA(
            CustomerEntity existingCustomer,
            TransactionEntity existingTransaction
    ) {
        Long receiverId = 0L;

        if ("BANK_ACCOUNT".equals(existingTransaction.getTransactionReceiverType().toString())) {
            Optional<BankAccountEntity> bankAccountOpt = bankAccountRepository.findById(existingTransaction.getReceiverId());
            CustomerEntity receiver = bankAccountOpt
                    .map(BankAccountEntity::getPaymentAccount)
                    .map(PaymentAccountEntity::getCustomer)
                    .orElse(null);
            receiverId = receiver != null ? receiver.getId() : 0L;
        } else if ("PAYMENT_ACCOUNT".equals(existingTransaction.getTransactionReceiverType().toString())) {
            CustomerEntity receiver = paymentAccountRepository.findById(existingTransaction.getReceiverId()).get().getCustomer();
            receiverId = receiver != null ? receiver.getId() : 0L;
        }
        // Return transaction of customer
        // Create a relationship key for the transaction-customer relationship
        TransactionCustomerEntity.TransactionCustomer transactionCustomerKey = new TransactionCustomerEntity.TransactionCustomer(existingTransaction, existingCustomer,receiverId);

        // Check a transaction of a customer exist
        if(!transactionCustomerRepository.existsTransactionCustomerEntityByTransactionCustomerKey(transactionCustomerKey)){
            throw new AppException(ErrorCode.TRANSACTION_OF_CUSTOMER_NOT_FOUND);
        }

        // Update datetime and status -> save
        existingTransaction.setTransactionDateTime(DateTimeUtils.getVietnamCurrentDateTime());
        existingTransaction.setTransactionStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(existingTransaction);

        // Delete pending ADD_FROM_BA_TO_PA transaction of customer
        // Check pending ADD_FROM_BA_TO_PA exceed 5 minutes
        deleteExpiredPendingTransaction(existingCustomer, TransactionType.ADD_FROM_BA_TO_PA);

        return existingTransaction;
    }

    @Override
    public TransactionEntity failAddMoneyToPA(
            CustomerEntity existingCustomer,
            TransactionEntity existingTransaction
    ) {
        Long receiverId = 0L;

        if ("BANK_ACCOUNT".equals(existingTransaction.getTransactionReceiverType().toString())) {
            Optional<BankAccountEntity> bankAccountOpt = bankAccountRepository.findById(existingTransaction.getReceiverId());
            CustomerEntity receiver = bankAccountOpt
                    .map(BankAccountEntity::getPaymentAccount)
                    .map(PaymentAccountEntity::getCustomer)
                    .orElse(null);
            receiverId = receiver != null ? receiver.getId() : 0L;
        } else if ("PAYMENT_ACCOUNT".equals(existingTransaction.getTransactionReceiverType().toString())) {
            CustomerEntity receiver = paymentAccountRepository.findById(existingTransaction.getReceiverId()).get().getCustomer();
            receiverId = receiver != null ? receiver.getId() : 0L;
        }
        // Return transaction of customer
        // Create a relationship key for the transaction-customer relationship
        TransactionCustomerEntity.TransactionCustomer transactionCustomerKey = new TransactionCustomerEntity.TransactionCustomer(existingTransaction, existingCustomer,receiverId);

        // Check a transaction of a customer exist
        if(!transactionCustomerRepository.existsTransactionCustomerEntityByTransactionCustomerKey(transactionCustomerKey)){
            throw new AppException(ErrorCode.TRANSACTION_OF_CUSTOMER_NOT_FOUND);
        }

        // Change in transaction
        existingTransaction.setTransactionDateTime(DateTimeUtils.getVietnamCurrentDateTime());
        existingTransaction.setTransactionStatus(TransactionStatus.FAILED);
        transactionRepository.save(existingTransaction);

        // Delete pending ADD_FROM_BA_TO_PA transaction of customer
        // Check pending ADD_FROM_BA_TO_PA exceed 5 minutes
        deleteExpiredPendingTransaction(existingCustomer, TransactionType.ADD_FROM_BA_TO_PA);

        return existingTransaction;
    }

    @Override
    public TransactionEntity completeAddMoneyFromPAToSA(
            CustomerEntity existingCustomer,
            TransactionEntity existingTransaction
    ) {
        Long receiverId = 0L;

        if ("BANK_ACCOUNT".equals(existingTransaction.getTransactionReceiverType().toString())) {
            Optional<BankAccountEntity> bankAccountOpt = bankAccountRepository.findById(existingTransaction.getReceiverId());
            CustomerEntity receiver = bankAccountOpt
                    .map(BankAccountEntity::getPaymentAccount)
                    .map(PaymentAccountEntity::getCustomer)
                    .orElse(null);
            receiverId = receiver != null ? receiver.getId() : 0L;
        } else if ("PAYMENT_ACCOUNT".equals(existingTransaction.getTransactionReceiverType().toString())) {
            CustomerEntity receiver = paymentAccountRepository.findById(existingTransaction.getReceiverId()).get().getCustomer();
            receiverId = receiver != null ? receiver.getId() : 0L;
        }
        // Return transaction of customer
        // Create a relationship key for the transaction-customer relationship
        TransactionCustomerEntity.TransactionCustomer transactionCustomerKey = new TransactionCustomerEntity.TransactionCustomer(existingTransaction, existingCustomer,receiverId);

        // Check a transaction of a customer exist
        if(!transactionCustomerRepository.existsTransactionCustomerEntityByTransactionCustomerKey(transactionCustomerKey)){
            throw new AppException(ErrorCode.TRANSACTION_OF_CUSTOMER_NOT_FOUND);
        }

        // Change in transaction
        existingTransaction.setTransactionDateTime(DateTimeUtils.getVietnamCurrentDateTime());
        existingTransaction.setTransactionStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(existingTransaction);

        // Delete pending ADD_FROM_PA_TO_SA or ADD_FROM_BA_TO_SA transaction of customer
        // Check pending ADD_FROM_PA_TO_SA or ADD_FROM_BA_TO_SA exceed 5 minutes
        deleteExpiredPendingTransaction(existingCustomer, TransactionType.ADD_FROM_PA_TO_SA);

        return existingTransaction;
    }

    @Override
    public TransactionEntity failAddMoneyToSA(
            CustomerEntity existingCustomer,
            TransactionEntity existingTransaction) {
        Long receiverId = 0L;

        if ("BANK_ACCOUNT".equals(existingTransaction.getTransactionReceiverType().toString())) {
            Optional<BankAccountEntity> bankAccountOpt = bankAccountRepository.findById(existingTransaction.getReceiverId());
            CustomerEntity receiver = bankAccountOpt
                    .map(BankAccountEntity::getPaymentAccount)
                    .map(PaymentAccountEntity::getCustomer)
                    .orElse(null);
            receiverId = receiver != null ? receiver.getId() : 0L;
        } else if ("PAYMENT_ACCOUNT".equals(existingTransaction.getTransactionReceiverType().toString())) {
            CustomerEntity receiver = paymentAccountRepository.findById(existingTransaction.getReceiverId()).get().getCustomer();
            receiverId = receiver != null ? receiver.getId() : 0L;
        }
        // Return transaction of customer
        // Create a relationship key for the transaction-customer relationship
        TransactionCustomerEntity.TransactionCustomer transactionCustomerKey = new TransactionCustomerEntity.TransactionCustomer(existingTransaction, existingCustomer,receiverId);

        // Check a transaction of a customer exist
        if(!transactionCustomerRepository.existsTransactionCustomerEntityByTransactionCustomerKey(transactionCustomerKey)){
            throw new AppException(ErrorCode.TRANSACTION_OF_CUSTOMER_NOT_FOUND);
        }

        // Change in transaction
        existingTransaction.setTransactionDateTime(DateTimeUtils.getVietnamCurrentDateTime());
        existingTransaction.setTransactionStatus(TransactionStatus.FAILED);
        transactionRepository.save(existingTransaction);

        // Delete pending ADD_FROM_PA_TO_SA transaction of customer
        // Check pending ADD_FROM_PA_TO_SA exceed 5 minutes
        deleteExpiredPendingTransaction(existingCustomer, TransactionType.ADD_FROM_PA_TO_SA);

        return existingTransaction;
    }

    // Method for deleting all pending transaction of a customer
    // Expiration time: in 5 minutes from NOW of Vietnam time zone
    // Must check transaction TYPE
    @Override
    public void deleteExpiredPendingTransaction(
            CustomerEntity existingCustomer,
            TransactionType transactionType
    ) {
        LocalDateTime dateTimeThreshold = DateTimeUtils.getVietnamCurrentDateTime().minusMinutes(5);
        List<TransactionEntity> pendingTransactions  = transactionRepository
                .findExpiredPendingTransactions(transactionType, TransactionStatus.PENDING, dateTimeThreshold);

        if(!pendingTransactions.isEmpty()){
            for (TransactionEntity pendingTransaction : pendingTransactions) {
                Long customerId = transactionCustomerRepository.findByTransactionCustomerKey_Transaction_transactionId(pendingTransaction.getId()).getTransactionCustomerKey().getCustomer().getId();
                if(customerId == existingCustomer.getId()){
                    Long receiverId = 0L;

                    if ("BANK_ACCOUNT".equals(pendingTransaction.getTransactionReceiverType().toString())) {
                        Optional<BankAccountEntity> bankAccountOpt = bankAccountRepository.findById(pendingTransaction.getReceiverId());
                        CustomerEntity receiver = bankAccountOpt
                                .map(BankAccountEntity::getPaymentAccount)
                                .map(PaymentAccountEntity::getCustomer)
                                .orElse(null);
                        receiverId = receiver != null ? receiver.getId() : 0L;
                    } else if ("PAYMENT_ACCOUNT".equals(pendingTransaction.getTransactionReceiverType().toString())) {
                        CustomerEntity receiver = paymentAccountRepository.findById(pendingTransaction.getReceiverId()).get().getCustomer();
                        receiverId = receiver != null ? receiver.getId() : 0L;
                    }

                    // Create the relationship key for each pending transaction
                    TransactionCustomerEntity.TransactionCustomer pendingTransactionCustomer =
                            new TransactionCustomerEntity.TransactionCustomer(pendingTransaction, existingCustomer, receiverId);

                    // Delete the TransactionCustomerEntity first
                    transactionCustomerRepository.deleteTransactionCustomerEntityByTransactionCustomerKey(pendingTransactionCustomer);

                    // Now delete the TransactionEntity if needed
                    transactionRepository.delete(pendingTransaction);
                }

            }
        }
    }

    // Method to get transaction by ID
    @Override
    public TransactionEntity getTransactionById(long transactionId) {
        Optional<TransactionEntity> optionalTransaction = transactionRepository.findById(transactionId);
        if(optionalTransaction.isPresent()) {
            return optionalTransaction.get();
        }
        throw new AppException(ErrorCode.TRANSACTION_NOT_FOUND);
    }

    // Method to get all transactions
    @Override
    public List<TransactionCustomerEntity> getAllTransactions() {
        return transactionCustomerRepository.findAll();
    }

    @Override
    public TransactionCustomerListResponse getPaginationListTransactionCustomer(
            Integer page, Integer size, String orderedBy, Boolean isAscending, String keyword
    ) {
        Long totalQuantity;
        Page<TransactionCustomerEntity> transactionCustomerPage;

        // Add prefix to differentiate sort by fields in table transactions or customers
        String prefixOrder = "t.";
        if(orderedBy.equals("customerId")){
            prefixOrder = "c.id"; // sort id (table customers) by customer_id of table transactions_of_customers
        }
        else{
            prefixOrder += orderedBy; // sort by all fields (table transactions) of table transactions_of_customers
        }

        // Get ascending or descending sort
        Sort sort = Boolean.TRUE.equals(isAscending)
                ? Sort.by(prefixOrder).ascending()
                : Sort.by(prefixOrder).descending();

        try {
            transactionCustomerPage = transactionCustomerRepository.findByTransactionType(
                    keyword, PageRequest.of(page - 1, size, sort));
            totalQuantity = transactionCustomerPage.getTotalElements();
        }
        catch (Exception e){
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        List<TransactionCustomerResponse> transactionCustomerResponses = transactionCustomerPage.stream()
                .map(TransactionCustomerResponse::fromTransactionCustomer)
                .toList();

        return TransactionCustomerListResponse.builder()
                .transactionCustomers(transactionCustomerResponses)
                .totalQuantity(totalQuantity)
                .build();
    }

    @Override
    public List<TransactionEntity> trackTransactionHistory(
            CustomerEntity existingCustomer,
            List<TransactionType> transactionTypes,
            TransactionReceiverType transactionReceiverType,
            Integer size
    ) {
        List<TransactionStatus> transactionStatusHistory = List.of(TransactionStatus.COMPLETED, TransactionStatus.FAILED);

        PageRequest pageRequest = (size != null) ? PageRequest.of(0, size) : PageRequest.of(0, Integer.MAX_VALUE);

        List<TransactionEntity> transactions;
        if (transactionTypes != null && !transactionTypes.isEmpty()) {
            transactions = transactionRepository.findTransactionsByTypeAndCustomer(transactionStatusHistory, existingCustomer, existingCustomer.getId(), transactionTypes, transactionReceiverType, pageRequest);
        } else {
            transactions = transactionRepository.findNotPendingTransactionsOfCustomer(transactionStatusHistory, existingCustomer,existingCustomer.getId(), pageRequest);
        }

        if (transactions.isEmpty()) {
            throw new AppException(ErrorCode.TRANSACTION_NOT_FOUND);
        }

        return transactions;
    }

    @Override
    public boolean checkTransactionCustomerExist(
            long customerId,
            long transactionId
    ) {
        TransactionEntity existingTransaction = getTransactionById(transactionId);
        CustomerEntity existingCustomer = customerService.getCustomerById(customerId);

        Long receiverId = 0L;

        if ("BANK_ACCOUNT".equals(existingTransaction.getTransactionReceiverType().toString())) {
            Optional<BankAccountEntity> bankAccountOpt = bankAccountRepository.findById(existingTransaction.getReceiverId());
            CustomerEntity receiver = bankAccountOpt
                    .map(BankAccountEntity::getPaymentAccount)
                    .map(PaymentAccountEntity::getCustomer)
                    .orElse(null);
            receiverId = receiver != null ? receiver.getId() : 0L;
        } else if ("PAYMENT_ACCOUNT".equals(existingTransaction.getTransactionReceiverType().toString())) {
            CustomerEntity receiver = paymentAccountRepository.findById(existingTransaction.getReceiverId()).get().getCustomer();
            receiverId = receiver != null ? receiver.getId() : 0L;
        }

        // Construct transactionCustomerKey
        TransactionCustomerEntity.TransactionCustomer transactionCustomerKey =
                new TransactionCustomerEntity.TransactionCustomer(existingTransaction, existingCustomer,receiverId);

        // Check a transaction of a customer exist
        if(!transactionCustomerRepository.existsTransactionCustomerEntityByTransactionCustomerKey(transactionCustomerKey)){
            return false;
        }

        // Update transaction date time for synchronizing with generate OTP
        // For generate and send OTP moment
        existingTransaction.setTransactionDateTime(DateTimeUtils.getVietnamCurrentDateTime());
        transactionRepository.save(existingTransaction);

        return true;
    }

    // Method to give reward point to PA of customer = 10% of completed transaction amount
    private Integer rewardPointForCompleteTransfer(Double amountTransfer){
        // Calculate 10% of the transferred amount
        double rewardPoints = amountTransfer * 0.1;

        // Cast the result to Integer (rounding down if necessary) and return
        return (int) Math.round(rewardPoints);
    }

    @Override
    public List<Object[]> getMonthlyTotalExpenseAmountForCustomer(Long customerId, TransactionStatus transactionStatus, TransactionType transactionType, int year) {
        List<Object[]> results = transactionRepository.findMonthlyTotalExpenseAmountForCustomer(
                customerId, transactionStatus, transactionType, year);

        // Initialize a map with each month and a total of 0
        Map<Integer, Double> monthlyTotals = new HashMap<>();
        for (int i = 1; i <= 12; i++) {
            monthlyTotals.put(i, 0.0);
        }

        for (Object[] result : results) {
            Integer month = ((Number) result[0]).intValue();
            Double totalAmount = ((Number) result[1]).doubleValue();
            monthlyTotals.put(month, totalAmount);
        }

        List<Object[]> finalResults = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : monthlyTotals.entrySet()) {
            finalResults.add(new Object[]{entry.getKey(), entry.getValue()});
        }

        finalResults.sort(Comparator.comparingInt(o -> (Integer) o[0]));
        return finalResults;
    }

    @Override
    public List<Object[]> getMonthlyTotalIncomeAmountForCustomer(Long customerId, TransactionStatus transactionStatus, TransactionType transactionType, int year) {
        List<Object[]> results = transactionRepository.findMonthlyTotalIncomeAmountForCustomer(
                customerId, transactionStatus, transactionType, year);

        // Initialize a map with each month and a total of 0
        Map<Integer, Double> monthlyTotals = new HashMap<>();
        for (int i = 1; i <= 12; i++) {
            monthlyTotals.put(i, 0.0);
        }

        for (Object[] result : results) {
            Integer month = ((Number) result[0]).intValue();
            Double totalAmount = ((Number) result[1]).doubleValue();
            monthlyTotals.put(month, totalAmount);
        }

        List<Object[]> finalResults = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : monthlyTotals.entrySet()) {
            finalResults.add(new Object[]{entry.getKey(), entry.getValue()});
        }

        finalResults.sort(Comparator.comparingInt(o -> (Integer) o[0]));
        return finalResults;
    }
    @Override
    public List<Object[]> findTotalRewardAmountForCustomer(Long customerId, TransactionStatus transactionStatus,
                                                           TransactionType transactionType, int year) {
        List<Object[]> results = transactionRepository.findTotalRewardAmountForCustomer(
                customerId, transactionStatus, transactionType, year);

        List<String> allRewardTypes = List.of("CULINARY", "ENTERTAINMENT", "SHOPPING");

        Map<String, Double> rewardTypeToAmountMap = new HashMap<>();
        for (String rewardType : allRewardTypes) {
            rewardTypeToAmountMap.put(rewardType, 0.0);
        }

        for (Object[] result : results) {
            String rewardType = result[0].toString();
            Double totalAmount = ((Number) result[1]).doubleValue();
            rewardTypeToAmountMap.put(rewardType, totalAmount);
        }

        List<Object[]> finalResults = new ArrayList<>();
        for (Map.Entry<String, Double> entry : rewardTypeToAmountMap.entrySet()) {
            finalResults.add(new Object[]{entry.getKey(), entry.getValue()});
        }

        return finalResults;
    }

    @Override
    public TransactionEntity insertRewardTransaction(AccountRewardRequest accountRewardDTO) {

        RewardEntity rewardEntity = rewardRepository.findById(accountRewardDTO.getRewardId())
                .orElseThrow(() -> new AppException(ErrorCode.REWARD_NOT_FOUND));

        PaymentAccountEntity paymentAccountEntity = paymentAccountRepository.findById(accountRewardDTO.getPaymentAccountId())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_FOUND));

        TransactionEntity newTransactionEntity = TransactionEntity.builder()
                .amount(rewardEntity.getCostPoint().doubleValue())
                .receiverId(accountRewardDTO.getRewardId())
                .senderId(accountRewardDTO.getPaymentAccountId())
                .transactionDateTime(DateTimeUtils.getVietnamCurrentDateTime())
                .amountType(AmountType.REWARD_POINT)
                .transactionReceiverType(TransactionReceiverType.REWARD_SYSTEM)
                .transactionRemark("use")
                .transactionSenderType(TransactionSenderType.PAYMENT_ACCOUNT)
                .transactionStatus(TransactionStatus.COMPLETED)
                .transactionType(TransactionType.REDEEM_REWARD)
                .build();

        newTransactionEntity = transactionRepository.save(newTransactionEntity);

        TransactionCustomerEntity.TransactionCustomer transactionCustomerKey =
                new TransactionCustomerEntity.TransactionCustomer(newTransactionEntity, paymentAccountEntity.getCustomer(), 0L);

        TransactionCustomerEntity newTransactionCustomer = TransactionCustomerEntity.builder()
                .transactionCustomerKey(transactionCustomerKey)
                .build();

        transactionCustomerRepository.save(newTransactionCustomer);

        return newTransactionEntity;
    }
}