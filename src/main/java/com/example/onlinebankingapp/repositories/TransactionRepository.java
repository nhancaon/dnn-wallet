package com.example.onlinebankingapp.repositories;

import com.example.onlinebankingapp.entities.CustomerEntity;
import com.example.onlinebankingapp.entities.TransactionEntity;
import com.example.onlinebankingapp.enums.TransactionReceiverType;
import com.example.onlinebankingapp.enums.TransactionStatus;
import com.example.onlinebankingapp.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    // Find unexpired pending transaction for TRANSFER_MONEY
    @Query("SELECT t FROM TransactionEntity t " +
            "WHERE t.transactionType = :transactionType " +
            "AND t.transactionStatus = :transactionStatus " +
            "AND t.transactionDateTime > :dateTimeThreshold")
    List<TransactionEntity> findUnexpiredPendingTransactions(
            @Param("transactionType") TransactionType transactionType,
            @Param("transactionStatus") TransactionStatus transactionStatus,
            @Param("dateTimeThreshold") LocalDateTime dateTimeThreshold
    );

    // Find expired pending transaction for TRANSFER_MONEY
    @Query("SELECT t FROM TransactionEntity t " +
            "WHERE t.transactionType = :transactionType " +
            "AND t.transactionStatus = :transactionStatus " +
            "AND t.transactionDateTime < :dateTimeThreshold")
    List<TransactionEntity> findExpiredPendingTransactions(
            @Param("transactionType") TransactionType transactionType,
            @Param("transactionStatus") TransactionStatus transactionStatus,
            @Param("dateTimeThreshold") LocalDateTime dateTimeThreshold
    );

    // Find transactions of a customer
    // Check status from return above (get COMPLETED, FAILED)
    // Sort by date time
    @Query("SELECT t FROM TransactionEntity t " +
            "JOIN TransactionCustomerEntity tce ON t = tce.transactionCustomerKey.transaction " +
            "WHERE (tce.transactionCustomerKey.customer = :existingCustomer " +
            "OR tce.transactionCustomerKey.receiverId = :receiverId) " +
            "AND t.transactionStatus IN :transactionStatusList " +
            "AND t.transactionType IN :transactionTypes " +
            "AND (:transactionReceiverType IS NULL OR t.transactionReceiverType = :transactionReceiverType) " +
            "ORDER BY t.transactionDateTime DESC")
    List<TransactionEntity> findTransactionsByTypeAndCustomer(
            @Param("transactionStatusList") List<TransactionStatus> transactionStatusList,
            @Param("existingCustomer") CustomerEntity existingCustomer,
            @Param("receiverId") Long receiverId,
            @Param("transactionTypes") List<TransactionType> transactionTypes,
            @Param("transactionReceiverType") TransactionReceiverType transactionReceiverType,
            Pageable pageable);

    @Query("SELECT t FROM TransactionEntity t " +
            "JOIN TransactionCustomerEntity tce ON t = tce.transactionCustomerKey.transaction " +
            "WHERE tce.transactionCustomerKey.customer = :existingCustomer " +
            "OR tce.transactionCustomerKey.receiverId = :receiverId " +
            "AND t.transactionStatus IN :transactionStatusList " +
            "ORDER BY t.transactionDateTime DESC")
    List<TransactionEntity> findNotPendingTransactionsOfCustomer(
            @Param("transactionStatusList") List<TransactionStatus> transactionStatusList,
            @Param("existingCustomer") CustomerEntity existingCustomer,
            @Param("receiverId") Long receiverId,
            Pageable pageable);

    @Query("SELECT EXTRACT(MONTH FROM t.transactionDateTime) AS month, SUM(t.amount) AS totalAmount " +
            "FROM TransactionEntity t " +
            "JOIN TransactionCustomerEntity tce ON t = tce.transactionCustomerKey.transaction " +
            "WHERE tce.transactionCustomerKey.customer.id = :customerId " +
            "AND t.transactionStatus = :transactionStatus " +
            "AND t.transactionType = :transactionType " +
            "AND EXTRACT(YEAR FROM t.transactionDateTime) = :year " +
            "GROUP BY EXTRACT(MONTH FROM t.transactionDateTime) " +
            "ORDER BY month ASC")
    List<Object[]> findMonthlyTotalExpenseAmountForCustomer(
            @Param("customerId") Long customerId,
            @Param("transactionStatus") TransactionStatus transactionStatus,
            @Param("transactionType") TransactionType transactionType,
            @Param("year") int year);

    @Query("SELECT EXTRACT(MONTH FROM t.transactionDateTime) AS month, SUM(t.amount) AS totalAmount " +
            "FROM TransactionEntity t " +
            "JOIN TransactionCustomerEntity tce ON t = tce.transactionCustomerKey.transaction " +
            "WHERE tce.transactionCustomerKey.receiverId = :customerId " +
            "AND t.transactionStatus = :transactionStatus " +
            "AND t.transactionType = :transactionType " +
            "AND EXTRACT(YEAR FROM t.transactionDateTime) = :year " +
            "GROUP BY EXTRACT(MONTH FROM t.transactionDateTime) " +
            "ORDER BY month ASC")
    List<Object[]> findMonthlyTotalIncomeAmountForCustomer(
            @Param("customerId") Long customerId,
            @Param("transactionStatus") TransactionStatus transactionStatus,
            @Param("transactionType") TransactionType transactionType,
            @Param("year") int year);

    @Query("SELECT r.rewardType AS rewardType, SUM(t.amount) AS totalAmount " +
            "FROM TransactionEntity t " +
            "JOIN TransactionCustomerEntity tce ON t = tce.transactionCustomerKey.transaction " +
            "JOIN RewardEntity r ON t.receiverId = r.id " +
            "WHERE tce.transactionCustomerKey.customer.id = :customerId " +
            "AND t.transactionStatus = :transactionStatus " +
            "AND t.transactionType = :transactionType " +
            "AND EXTRACT(YEAR FROM t.transactionDateTime) = :year " +
            "GROUP BY r.rewardType " +
            "ORDER BY r.rewardType ASC")
    List<Object[]> findTotalRewardAmountForCustomer(
            @Param("customerId") Long customerId,
            @Param("transactionStatus") TransactionStatus transactionStatus,
            @Param("transactionType") TransactionType transactionType,
            @Param("year") int year);
}
