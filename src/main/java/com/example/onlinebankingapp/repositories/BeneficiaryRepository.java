package com.example.onlinebankingapp.repositories;

import com.example.onlinebankingapp.entities.BeneficiaryEntity;
import com.example.onlinebankingapp.entities.CustomerEntity;
import com.example.onlinebankingapp.enums.BeneficiaryReceiverType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeneficiaryRepository extends JpaRepository<BeneficiaryEntity, Long> {
    boolean existsBeneficiaryEntityByCustomerAndReceiverIdAndBeneficiaryReceiverType(CustomerEntity customer, Long receiverId, BeneficiaryReceiverType beneficiaryReceiverType);
    boolean existsBeneficiaryEntityByCustomerAndName(CustomerEntity customer, String name);

    @Query("SELECT b FROM BeneficiaryEntity b WHERE b.id = :beneficiaryId AND b.beneficiaryReceiverType = 'PAYMENT_ACCOUNT'")
    BeneficiaryEntity findBeneficiaryOfPaymentAccountById(Long beneficiaryId);

    @Query("SELECT b FROM BeneficiaryEntity b WHERE b.customer.id = :customerId AND b.beneficiaryReceiverType = 'PAYMENT_ACCOUNT'")
    List<BeneficiaryEntity> findBeneficiariesOfPaymentAccountByCustomerId(Long customerId);

    @Query("SELECT b FROM BeneficiaryEntity b WHERE b.id = :beneficiaryId AND b.beneficiaryReceiverType = 'BANK_ACCOUNT'")
    BeneficiaryEntity findBeneficiaryOfBankAccountById(Long beneficiaryId);

    @Query("SELECT b FROM BeneficiaryEntity b WHERE b.customer.id = :customerId AND b.beneficiaryReceiverType = 'BANK_ACCOUNT'")
    List<BeneficiaryEntity> findBeneficiariesOfBankAccountByCustomerId(Long customerId);

    @Query("SELECT b FROM BeneficiaryEntity b WHERE b.customer.id = :customerId ORDER BY b.id ASC")
    List<BeneficiaryEntity> findAllBeneficiariesByCustomerId(Long customerId);

    Page<BeneficiaryEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
