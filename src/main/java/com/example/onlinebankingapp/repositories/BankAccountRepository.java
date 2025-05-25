package com.example.onlinebankingapp.repositories;

import com.example.onlinebankingapp.entities.BankAccountEntity;
import com.example.onlinebankingapp.entities.BankEntity;
import com.example.onlinebankingapp.entities.PaymentAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccountEntity, Long> {
    Optional<BankAccountEntity> findByBankAccountNumberAndBank(String bankAccountNumber, BankEntity bankEntity);

    Optional<BankAccountEntity> findByBankAccountNumber(String bankAccountNumber);

    List<BankAccountEntity> findByPaymentAccount(PaymentAccountEntity paymentAccountEntity);


    List<BankAccountEntity> findByPaymentAccountId(Long paymentAccountId);
}
