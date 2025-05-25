package com.example.onlinebankingapp.services.Transaction;

import com.example.onlinebankingapp.dtos.requests.AccountRewardRequest;
import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionRequest;
import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionToBA.TransactionToBAFromBARequest;
import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionToBA.TransactionToBAFromPARequest;
import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionToPA.TransactionToPAFromBARequest;
import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionToPA.TransactionToPAFromPARequest;
import com.example.onlinebankingapp.dtos.responses.TransactionCustomer.TransactionCustomerListResponse;
import com.example.onlinebankingapp.entities.CustomerEntity;
import com.example.onlinebankingapp.entities.TransactionCustomerEntity;
import com.example.onlinebankingapp.entities.TransactionEntity;
import com.example.onlinebankingapp.enums.TransactionReceiverType;
import com.example.onlinebankingapp.enums.TransactionStatus;
import com.example.onlinebankingapp.enums.TransactionType;

import java.util.List;
import java.util.Map;

public interface TransactionService {
    Map<String, Object> getUnexpiredPendingTransferToPA(CustomerEntity existingCustomer);
    Map<String, Object> getUnexpiredPendingTransferToBA(CustomerEntity existingCustomer);
    TransactionCustomerEntity checkDefaultPAForTransfer(CustomerEntity existingCustomer, Long oldUnexpiredPendingTransactionId, TransactionRequest transactionRequest);
    TransactionCustomerEntity insertPendingTransaction(TransactionRequest transactionRequest, CustomerEntity customerEntity,Long receiverId);
    TransactionEntity transferToPAFromPA(long customerId, TransactionToPAFromPARequest toPAFromPARequest);
    TransactionEntity transferToPAFromBA(long customerId, TransactionToPAFromBARequest toPAFromBARequest);
    TransactionEntity transferToBAFromPA(long customerId, TransactionToBAFromPARequest toBAFromPARequest);
    TransactionEntity transferToBAFromBA(long customerId, TransactionToBAFromBARequest toBAFromBARequest);
    boolean checkTransactionCustomerExist(long customerId, long transactionId);
    TransactionCustomerEntity completeTransferMoney(CustomerEntity existingCustomer, TransactionEntity existingTransaction);
    void failTransferMoney(CustomerEntity existingCustomer, TransactionEntity existingTransaction);
    TransactionEntity completeAddMoneyFromBAToPA(CustomerEntity existingCustomer, TransactionEntity existingTransaction);
    TransactionEntity failAddMoneyToPA(CustomerEntity existingCustomer, TransactionEntity existingTransaction);
    TransactionEntity completeAddMoneyFromPAToSA(CustomerEntity existingCustomer, TransactionEntity existingTransaction);
    TransactionEntity failAddMoneyToSA(CustomerEntity existingCustomer, TransactionEntity existingTransaction);
    void deleteExpiredPendingTransaction(CustomerEntity existingCustomer, TransactionType transactionType);
    TransactionEntity getTransactionById(long transactionId);
    List<TransactionCustomerEntity> getAllTransactions();
    TransactionCustomerListResponse getPaginationListTransactionCustomer(Integer page, Integer size, String orderedBy, Boolean isAscending, String keyword);
    List<TransactionEntity> trackTransactionHistory(CustomerEntity existingCustomer, List<TransactionType> transactionTypes, TransactionReceiverType transactionReceiverType, Integer size);
    List<Object[]> getMonthlyTotalExpenseAmountForCustomer(Long customerId, TransactionStatus transactionStatus, TransactionType transactionType, int year);
    List<Object[]> getMonthlyTotalIncomeAmountForCustomer(Long customerId, TransactionStatus transactionStatus, TransactionType transactionType, int year);
    List<Object[]> findTotalRewardAmountForCustomer(Long customerId, TransactionStatus transactionStatus, TransactionType transactionType, int year);
    TransactionEntity insertRewardTransaction(AccountRewardRequest accountRewardDTO);
}
