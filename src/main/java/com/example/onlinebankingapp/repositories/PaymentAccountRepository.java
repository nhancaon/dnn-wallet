package com.example.onlinebankingapp.repositories;

import com.example.onlinebankingapp.enums.AccountStatus;
import com.example.onlinebankingapp.entities.PaymentAccountEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentAccountRepository extends JpaRepository<PaymentAccountEntity, Long> {
    boolean existsByAccountNumber(String accountNumber);
    // Check if a payment account exists by customer ID and account status
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PaymentAccountEntity p WHERE p.customer.id = :customerId AND p.accountStatus = :accountStatus")
    boolean checkExistingByStatus(long customerId, AccountStatus accountStatus);

    // Retrieve a payment account by customer ID and account status
    @Query("SELECT p FROM PaymentAccountEntity p WHERE p.accountStatus = :accountStatus and p.customer.id = :customerId")
    Optional<PaymentAccountEntity> getPaymentAccountByStatus(long customerId, AccountStatus accountStatus);

    // Retrieve payment accounts by customer ID
    @Query ("SELECT p FROM PaymentAccountEntity p WHERE p.customer.id = :customerId")
    List<PaymentAccountEntity> getPaymentAccountsByCustomerId(long customerId);

    @Query("SELECT p FROM PaymentAccountEntity p WHERE p.accountNumber LIKE %:accountNumber%")
    List<PaymentAccountEntity> searchPaymentAccountByAccountNumber(String accountNumber);

    PaymentAccountEntity getPaymentAccountByAccountNumber(String accountNumber);

    Page<PaymentAccountEntity> findByAccountNumberContainingIgnoreCase(String accountNumber, Pageable pageable);
}
