package com.example.onlinebankingapp.services.PaymentAccount;

import com.example.onlinebankingapp.dtos.requests.PaymentAccount.AmountOperationRequest;
import com.example.onlinebankingapp.dtos.requests.BankAccount.BankAccountRequest;
import com.example.onlinebankingapp.dtos.requests.PaymentAccount.PaymentAccountRequest;
import com.example.onlinebankingapp.dtos.responses.PaymentAccount.PaymentAccountListResponse;
import com.example.onlinebankingapp.entities.PaymentAccountEntity;
import com.example.onlinebankingapp.entities.TransactionCustomerEntity;

import java.util.List;
import java.util.Map;

public interface PaymentAccountService {
    PaymentAccountEntity insertPaymentAccount(String newAccountNumber, Long customerId);
    List<PaymentAccountEntity> getAllPaymentAccounts();
    void setDefaultPaymentAccount(Long customerId, String accountNumber);
    PaymentAccountEntity getPaymentAccountById(Long paymentAccountId);
    PaymentAccountListResponse getPaginationListPaymentAccount(Integer page, Integer size, String orderedBy, Boolean isAscending, String keyword);
    List<PaymentAccountEntity> searchPaymentAccountByAccountNumber(String accountNumber);
    PaymentAccountEntity getPaymentAccountByAccountNumber(String accountNumber);
    PaymentAccountEntity getDefaultPaymentAccount(Long customerId);
    List<PaymentAccountEntity> getPaymentAccountsByCustomerId(Long customerId);
    TransactionCustomerEntity insertTransactionForAddMoneyToPA(Long bankAccountId, Long paymentAccountId, Double amount);
    PaymentAccountEntity addMoneyToPaymentAccount(Long paymentAccountId, String bankName, BankAccountRequest bankAccountRequest, AmountOperationRequest amountDTO);
    Map<String, Object> withdrawFromPaymentAccount(Long paymentAccountId, String bankName, BankAccountRequest bankAccountRequest, AmountOperationRequest amountDTO);
    PaymentAccountEntity updatePaymentAccount(Long paymentAccountId, PaymentAccountRequest paymentAccountRequest);
    void deletePaymentAccountById(Long paymentAccountId);
}
