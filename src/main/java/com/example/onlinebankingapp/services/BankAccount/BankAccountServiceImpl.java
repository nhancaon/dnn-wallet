package com.example.onlinebankingapp.services.BankAccount;

import com.example.onlinebankingapp.dtos.requests.BankAccount.BankAccountRequest;
import com.example.onlinebankingapp.dtos.requests.PaymentAccount.PaymentAccountRequest;
import com.example.onlinebankingapp.entities.BankAccountEntity;
import com.example.onlinebankingapp.entities.BankEntity;
import com.example.onlinebankingapp.entities.PaymentAccountEntity;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import com.example.onlinebankingapp.repositories.BankAccountRepository;
import com.example.onlinebankingapp.repositories.BankRepository;
import com.example.onlinebankingapp.repositories.PaymentAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {
    private final BankAccountRepository bankAccountRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final BankRepository bankRepository;

    // Method to check if bank account exist or not
    @Override
    public BankAccountEntity checkBankAccountExist(
            BankAccountRequest bankAccountRequest,
            String bankName
    ) {
        // Find and check if the bank exists
        Optional<BankEntity> optionalBank = bankRepository.findByName(bankName);
        if(optionalBank.isEmpty()){
            throw new AppException(ErrorCode.BANK_NOT_FOUND);
        }

        BankEntity bank = optionalBank.get();

        // Find and check if the bank account exists
        // Step 1: Check bank account number
        Optional<BankAccountEntity> optionalBankAccount = bankAccountRepository
                .findByBankAccountNumberAndBank(bankAccountRequest.getBankAccountNumber(), bank);
        if(optionalBankAccount.isEmpty()){
            throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
        }

        BankAccountEntity existingBankAccount = optionalBankAccount.get();
        // Step 2: Check bank account number fit with bank name
        if(!existingBankAccount.getBank().equals(optionalBank.get())){
            throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
        }

        // Step 3: Check citizen ID
        if(!existingBankAccount.getCitizenId().equals(bankAccountRequest.getCitizenId())){
            throw new AppException(ErrorCode.CITIZEN_ID_INVALID);
        }

        // Step 4: Check owner name
        if(!existingBankAccount.getName().equals(bankAccountRequest.getName())){
            throw new AppException(ErrorCode.OWNER_NAME_INVALID);
        }

        return existingBankAccount;
    }

    // Check if bank_account have linked with any payment_account or not
    // P/s: only use for insert association
    private BankAccountEntity checkBankAccountNotLinked(
            BankAccountEntity existingBankAccount
    ) throws AppException {
        if(existingBankAccount.getPaymentAccount() != null){
            throw new AppException(ErrorCode.BANK_ACCOUNT_LINKED);
        }
        return existingBankAccount;
    }

    // P/s: only use for delete association
    private BankAccountEntity checkBankAccountLinked(
            BankAccountEntity existingBankAccount
    ) throws AppException {
        if(existingBankAccount.getPaymentAccount() == null){
            throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_LINKED);
        }
        return existingBankAccount;
    }

    // Method to associate a bank account with a payment account
    @Override
    public BankAccountEntity insertAssociationToPaymentAccount(
            BankAccountEntity existingBankAccount,
            PaymentAccountRequest paymentAccountRequest,
            String bankName
    ) {
        // Check existing bank (fetch existing bank data in DB) -> do not need to impl
        // Check if bank account linked to payment account or not
        BankAccountEntity notLinkedBankAccount = checkBankAccountNotLinked(existingBankAccount);

        // Check payment account exist
        if(!paymentAccountRepository.existsByAccountNumber(paymentAccountRequest.getAccountNumber())){
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_FOUND);
        }

        PaymentAccountEntity associatedPaymentAccount = paymentAccountRepository
                .getPaymentAccountByAccountNumber(paymentAccountRequest.getAccountNumber());
        notLinkedBankAccount.setPaymentAccount(associatedPaymentAccount);

        return bankAccountRepository.save(notLinkedBankAccount);
    }

    // Method to delete a bank account from a payment account
    @Override
    public BankAccountEntity deleteAssociationFromPaymentAccount(
            BankAccountEntity existingBankAccount,
            PaymentAccountRequest paymentAccountRequest,
            String bankName
    ) {
        // Check if bank account linked to payment account or not
        BankAccountEntity linkedBankAccount = checkBankAccountLinked(existingBankAccount);

        // Check payment account exist
        if(!paymentAccountRepository.existsByAccountNumber(paymentAccountRequest.getAccountNumber())){
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_FOUND);
        }

        // Find payment account
        PaymentAccountEntity paymentAccountEntity = paymentAccountRepository
                .getPaymentAccountByAccountNumber(paymentAccountRequest.getAccountNumber());
        if(!linkedBankAccount.getPaymentAccount().equals(paymentAccountEntity)){
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_INVALID);
        }

        // Delete association between bank account and payment account
        linkedBankAccount.setPaymentAccount(null);
        return bankAccountRepository.save(linkedBankAccount);
    }

    @Override
    public BankAccountEntity getBankAccountByBankAccountId(Long bankAccountId) {

        Optional<BankAccountEntity> optionalBankAccount = bankAccountRepository
                .findById(bankAccountId);
        if(optionalBankAccount.isEmpty()){
            throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
        }

        return optionalBankAccount.get();
    }

    @Override
    public List<BankAccountEntity> getBankAccountsByPaymentAccountId(Long paymentAccountId) {

        List<BankAccountEntity> bankAccounts = bankAccountRepository.findByPaymentAccountId(paymentAccountId);

        if(bankAccounts.isEmpty()) {
            return Collections.emptyList();
        }

        return bankAccounts;
    }

    @Override
    public BankAccountEntity getBankAccountById(
            long bankAccountId
    ) {
        // Get the bank account by its id
        Optional<BankAccountEntity> optionalBankAccount = bankAccountRepository.findById(bankAccountId);
        if(optionalBankAccount.isPresent()) {
            return optionalBankAccount.get();
        }
        throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
    }

    @Override
    public List<BankAccountEntity> getAllAssociatedBankAccountByCustomerId(
            long customerId
    ) {
        // Get all payment accounts associated with the customer
        List<PaymentAccountEntity> paymentAccountsOfACustomer =  paymentAccountRepository
                .getPaymentAccountsByCustomerId(customerId);

        if(paymentAccountsOfACustomer.isEmpty()){
            throw new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_FOUND);
        }

        // Create a list to hold all associated bank accounts
        List<BankAccountEntity> associatedBankAccounts = new ArrayList<>();

        for (PaymentAccountEntity paymentAccount : paymentAccountsOfACustomer) {
            // Find bank accounts linked to the current payment account
            List<BankAccountEntity> bankAccounts = bankAccountRepository.findByPaymentAccount(paymentAccount);

            // Add all found bank accounts to the associatedBankAccounts list
            associatedBankAccounts.addAll(bankAccounts);
        }

        if(associatedBankAccounts.isEmpty()){
            throw new AppException(ErrorCode.NOT_LINKED_ANY_BANK_ACCOUNT);
        }

        System.out.println("associatedBankAccounts: " + associatedBankAccounts);
        return associatedBankAccounts;
    }

    @Override
    public BankAccountEntity getBankAccountByAccountNumber(String bankAccountNumber, String bankName) {
        // Find and check if the bank exists
        Optional<BankEntity> optionalBank = bankRepository.findByName(bankName);
        if(optionalBank.isEmpty()){
            throw new AppException(ErrorCode.BANK_NOT_FOUND);
        }

        BankEntity bank = optionalBank.get();

        // Find and check if the bank account exists
        // Step 1: Check bank account number
        Optional<BankAccountEntity> optionalBankAccount = bankAccountRepository
                .findByBankAccountNumberAndBank(bankAccountNumber, bank);
        if(optionalBankAccount.isEmpty()){
            throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
        }

        return optionalBankAccount.get();
    }
    @Override
    public
    BankAccountEntity getByBankAccountNumber(String bankAccountNumber) {
        Optional<BankAccountEntity> optionalBankAccount = bankAccountRepository
                .findByBankAccountNumber(bankAccountNumber);
        if(optionalBankAccount.isEmpty()){
            throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
        }

        return optionalBankAccount.get();
    }
}
