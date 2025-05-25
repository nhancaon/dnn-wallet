package com.example.onlinebankingapp.services.Beneficiary;

import com.example.onlinebankingapp.dtos.requests.BeneficiaryRequest;
import com.example.onlinebankingapp.dtos.responses.Beneficiary.BeneficiaryListResponse;
import com.example.onlinebankingapp.entities.BeneficiaryEntity;

import java.util.List;
import java.util.Map;

public interface BeneficiaryService {
    BeneficiaryEntity insertBeneficiaryOfPaymentAccount(BeneficiaryRequest beneficiaryRequest);
    BeneficiaryEntity insertBeneficiaryOfBankAccount(BeneficiaryRequest beneficiaryRequest);
    BeneficiaryEntity getBeneficiaryOfPaymentAccountById(Long beneficiaryId) ;
    List<Map<String, Object>> getBeneficiariesOfPaymentAccount(Long customerId);
    BeneficiaryEntity getBeneficiaryOfBankAccountById(Long beneficiaryId) ;
    List<Map<String, Object>> getBeneficiariesOfBankAccount(Long customerId);
    List<BeneficiaryEntity> getAllBeneficiaries(Long customerId);
    BeneficiaryListResponse getPaginationListBeneficiary(Integer page, Integer size, String orderedBy, Boolean isAscending, String keyword);
    BeneficiaryEntity updateNameOfPaymentAccountBeneficiary(Long beneficiaryId, BeneficiaryRequest beneficiaryRequest);
    BeneficiaryEntity updateNameOfBankAccountBeneficiary(Long beneficiaryId, BeneficiaryRequest beneficiaryRequest);
    void deleteBeneficiaryOfPaymentAccount(Long beneficiaryId);
    void deleteBeneficiaryOfBankAccount(Long beneficiaryId);
}
