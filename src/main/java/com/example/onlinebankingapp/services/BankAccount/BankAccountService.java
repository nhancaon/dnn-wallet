package com.example.onlinebankingapp.services.BankAccount;

import com.example.onlinebankingapp.dtos.requests.BankAccount.BankAccountRequest;
import com.example.onlinebankingapp.dtos.requests.PaymentAccount.PaymentAccountRequest;
import com.example.onlinebankingapp.entities.BankAccountEntity;

import java.util.List;

public interface BankAccountService {
    BankAccountEntity checkBankAccountExist(BankAccountRequest bankAccountRequest, String bankName);
    BankAccountEntity insertAssociationToPaymentAccount(BankAccountEntity existingBankAccount, PaymentAccountRequest paymentAccountRequest, String bankName);
    BankAccountEntity deleteAssociationFromPaymentAccount(BankAccountEntity existingBankAccount, PaymentAccountRequest paymentAccountRequest, String bankName);

    BankAccountEntity getBankAccountByBankAccountId(Long bankAccountId);

    List<BankAccountEntity> getBankAccountsByPaymentAccountId(Long paymentAccountId);
    BankAccountEntity getBankAccountById(long bankAccountId);
    List<BankAccountEntity> getAllAssociatedBankAccountByCustomerId(long customerId);

    BankAccountEntity getBankAccountByAccountNumber(String bankAccountNumber, String bankName);

    BankAccountEntity getByBankAccountNumber(String bankAccountNumber);
}
