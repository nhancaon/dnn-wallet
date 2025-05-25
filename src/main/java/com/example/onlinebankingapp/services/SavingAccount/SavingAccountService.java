package com.example.onlinebankingapp.services.SavingAccount;

import com.example.onlinebankingapp.dtos.requests.SavingAccountRequest;
import com.example.onlinebankingapp.dtos.responses.SavingAccount.SavingAccountListResponse;
import com.example.onlinebankingapp.entities.SavingAccountEntity;
import com.example.onlinebankingapp.entities.TransactionCustomerEntity;
import com.example.onlinebankingapp.entities.TransactionEntity;

import java.util.List;

public interface SavingAccountService {
    SavingAccountEntity insertSavingAccount(SavingAccountRequest savingAccountRequest);
    SavingAccountEntity insertSavingAccountByEmployee(SavingAccountRequest savingAccountRequest);
    TransactionCustomerEntity insertTransactionForAddMoneyToSA(Long paymentAccountId, Long savingAccountId, Double amount);
    SavingAccountEntity addMoneyToSavingAccount(Long savingAccountId, TransactionEntity existingTransaction);
    SavingAccountEntity withdrawFromSavingAccount(Long savingAccountId);
    SavingAccountEntity getSavingAccountById(Long savingAccountId);
    List<SavingAccountEntity> getSavingAccountsOfCustomer(Long customerId);
    List<SavingAccountEntity> getAllSavingAccounts();
    SavingAccountListResponse getPaginationListSavingAccount(Integer page, Integer size, String orderedBy, Boolean isAscending, String keyword);
    List<SavingAccountEntity> getSavingAccountsOfPaymentAccount(Long paymentAccountId);
    void deleteSavingAccount(Long savingAccountId);
    SavingAccountEntity updateSavingAccountByEmployee(Long savingAccountId, SavingAccountRequest savingAccountRequest);

    // Update via Scheduled -> in SavingAccountScheduler
    boolean isEndOfTerm(SavingAccountEntity savingAccount);
    void updateDailyCurrentBalance(SavingAccountEntity savingAccount);
    void deactivateAndWithdrawCurrentAmountToPA(SavingAccountEntity savingAccount);
}
