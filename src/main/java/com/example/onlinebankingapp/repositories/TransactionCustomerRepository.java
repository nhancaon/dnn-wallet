package com.example.onlinebankingapp.repositories;

import com.example.onlinebankingapp.entities.TransactionCustomerEntity;
import com.example.onlinebankingapp.entities.TransactionEntity;
import com.example.onlinebankingapp.enums.TransactionType;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionCustomerRepository extends JpaRepository<TransactionCustomerEntity, TransactionCustomerEntity.TransactionCustomer> {
    boolean existsTransactionCustomerEntityByTransactionCustomerKey(TransactionCustomerEntity.TransactionCustomer transactionCustomerKey);

    @Transactional
    void deleteTransactionCustomerEntityByTransactionCustomerKey(TransactionCustomerEntity.TransactionCustomer transactionCustomerKey);

    @Query("SELECT tce FROM TransactionCustomerEntity tce " +
            "WHERE tce.transactionCustomerKey.transaction.id = :transactionId")
    TransactionCustomerEntity findByTransactionCustomerKey_Transaction_transactionId(@Param("transactionId") Long transactionId);

    @Query("SELECT tce FROM TransactionCustomerEntity tce " +
            "JOIN tce.transactionCustomerKey.transaction t " +
            "JOIN tce.transactionCustomerKey.customer c " +
            "WHERE UPPER(t.transactionType) LIKE UPPER(CONCAT('%', :transactionType, '%'))")
    Page<TransactionCustomerEntity> findByTransactionType(
            @Param("transactionType") String transactionType, Pageable pageable);
}
