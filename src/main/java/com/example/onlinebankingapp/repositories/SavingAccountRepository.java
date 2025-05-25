package com.example.onlinebankingapp.repositories;

import com.example.onlinebankingapp.entities.PaymentAccountEntity;
import com.example.onlinebankingapp.entities.SavingAccountEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingAccountRepository extends JpaRepository<SavingAccountEntity, Long> {
    boolean existsByAccountNumber(String accountNumber);
    List<SavingAccountEntity> findSavingAccountEntitiesByPaymentAccount(PaymentAccountEntity paymentAccount);

    Page<SavingAccountEntity> findByAccountNumberContainingIgnoreCase(String accountNumber, Pageable pageable);
}
