package com.example.onlinebankingapp.services.Beneficiary;

import com.example.onlinebankingapp.dtos.requests.BeneficiaryRequest;
import com.example.onlinebankingapp.dtos.responses.Beneficiary.BeneficiaryListResponse;
import com.example.onlinebankingapp.dtos.responses.Beneficiary.BeneficiaryResponse;
import com.example.onlinebankingapp.entities.BankAccountEntity;
import com.example.onlinebankingapp.entities.BeneficiaryEntity;
import com.example.onlinebankingapp.entities.CustomerEntity;
import com.example.onlinebankingapp.entities.PaymentAccountEntity;
import com.example.onlinebankingapp.enums.BeneficiaryReceiverType;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import com.example.onlinebankingapp.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BeneficiaryServiceImpl implements BeneficiaryService {
    private final BeneficiaryRepository beneficiaryRepository;
    private final CustomerRepository customerRepository;
    private final PaymentAccountRepository paymentAccountRepository;
    private final BankAccountRepository bankAccountRepository;
    private final BankRepository bankRepository;

    // Method to insert a beneficiary for PA
    @Override
    public BeneficiaryEntity insertBeneficiaryOfPaymentAccount(
            BeneficiaryRequest beneficiaryRequest
    ){
        // Fetch customer and payment account entities or throw an AppException if not found
        CustomerEntity existingCustomer = customerRepository.findById(beneficiaryRequest.getCustomerId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        PaymentAccountEntity existingPaymentAccount = paymentAccountRepository.findById(beneficiaryRequest.getReceiverId())
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_ACCOUNT_NOT_FOUND));

        // Check if beneficiary receiver ID link wrong to own PA of customer
        if(existingPaymentAccount.getCustomer().equals(existingCustomer)){
            throw new AppException(ErrorCode.BENEFICIARY_SELF_PA_FORBIDDEN);
        }

        // Check if the beneficiary already exists
        if (beneficiaryRepository.existsBeneficiaryEntityByCustomerAndReceiverIdAndBeneficiaryReceiverType(
                existingCustomer, existingPaymentAccount.getId(), BeneficiaryReceiverType.PAYMENT_ACCOUNT)) {
            throw new AppException(ErrorCode.BENEFICIARY_EXISTS);
        }

        // Check if updateName exist of the list of customer's beneficiaries
        if(beneficiaryRepository.existsBeneficiaryEntityByCustomerAndName(existingCustomer, beneficiaryRequest.getName())){
            throw new AppException(ErrorCode.BENEFICIARY_NAME_EXISTS);
        }

        // Create new BeneficiaryEntity for PA
        BeneficiaryEntity newBeneficiaryPA = BeneficiaryEntity.builder()
                .name(beneficiaryRequest.getName())
                .customer(existingCustomer)
                .receiverId(existingPaymentAccount.getId())
                .beneficiaryReceiverType(BeneficiaryReceiverType.PAYMENT_ACCOUNT)
                .build();

        // Save and return the new beneficiary
        return beneficiaryRepository.save(newBeneficiaryPA);
    }

    // Method to insert a beneficiary for BA
    @Override
    public BeneficiaryEntity insertBeneficiaryOfBankAccount(
            BeneficiaryRequest beneficiaryRequest
    ) {
        // Fetch customer and payment account entities or throw an AppException if not found
        CustomerEntity existingCustomer = customerRepository.findById(beneficiaryRequest.getCustomerId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        BankAccountEntity existingBankAccount = bankAccountRepository.findById(beneficiaryRequest.getReceiverId())
                .orElseThrow(() -> new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND));

        if (!existingBankAccount.getBank().getName().equals(beneficiaryRequest.getBankName())) {
            throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
        }

        // Check if the beneficiary already exists
        if (beneficiaryRepository.existsBeneficiaryEntityByCustomerAndReceiverIdAndBeneficiaryReceiverType(
                existingCustomer, existingBankAccount.getId(), BeneficiaryReceiverType.BANK_ACCOUNT)) {
            throw new AppException(ErrorCode.BENEFICIARY_EXISTS);
        }

        // Check if updateName exist of the list of customer's beneficiaries
        if(beneficiaryRepository.existsBeneficiaryEntityByCustomerAndName(existingCustomer, beneficiaryRequest.getName())){
            throw new AppException(ErrorCode.BENEFICIARY_NAME_EXISTS);
        }

        // Create new BeneficiaryEntity for BA
        BeneficiaryEntity newBeneficiaryBA = BeneficiaryEntity.builder()
                .name(beneficiaryRequest.getName())
                .customer(existingCustomer)
                .receiverId(existingBankAccount.getId())
                .beneficiaryReceiverType(BeneficiaryReceiverType.BANK_ACCOUNT)
                .build();

        // Save and return the new beneficiary
        return beneficiaryRepository.save(newBeneficiaryBA);
    }

    // Method to get a beneficiary for PA by its id
    @Override
    public BeneficiaryEntity getBeneficiaryOfPaymentAccountById(
            Long beneficiaryId
    ){
        BeneficiaryEntity queryBeneficiaryPA = beneficiaryRepository
                .findBeneficiaryOfPaymentAccountById(beneficiaryId);
        if(queryBeneficiaryPA == null){
            throw new AppException(ErrorCode.BENEFICIARY_INVALID_PA);
        }
        return queryBeneficiaryPA;
    }

    // Method to get list of beneficiaries for PA by customer id
    @Override
    public List<Map<String, Object>> getBeneficiariesOfPaymentAccount(Long customerId) {
        List<BeneficiaryEntity> beneficiariesOfPA = beneficiaryRepository
                .findBeneficiariesOfPaymentAccountByCustomerId(customerId);

        if (beneficiariesOfPA.isEmpty()) {
            throw new AppException(ErrorCode.BENEFICIARY_PA_LIST_NOT_FOUND);
        }

        return beneficiariesOfPA.stream().map(beneficiary -> {
            Map<String, Object> beneficiaryMap = new HashMap<>();
            beneficiaryMap.put("id", beneficiary.getId());
            beneficiaryMap.put("name", beneficiary.getName());
            beneficiaryMap.put("customer_id", beneficiary.getCustomer().getId());
            beneficiaryMap.put("receiver_id", beneficiary.getReceiverId());
            beneficiaryMap.put("beneficiary_receiver_type", beneficiary.getBeneficiaryReceiverType());

            String accountNumber = paymentAccountRepository
                    .findById(beneficiary.getReceiverId()).get().getAccountNumber();
            beneficiaryMap.put("account_number", accountNumber);
            String phoneNumber = customerRepository.findById(beneficiary.getReceiverId()).get().getPhoneNumber();
            beneficiaryMap.put("phone_number", phoneNumber);
            return beneficiaryMap;
        }).toList();
    }

    // Method to get a beneficiary for BA by its id
    @Override
    public BeneficiaryEntity getBeneficiaryOfBankAccountById(
            Long beneficiaryId
    ){
        BeneficiaryEntity queryBeneficiaryBA = beneficiaryRepository
                .findBeneficiaryOfBankAccountById(beneficiaryId);
        if(queryBeneficiaryBA == null){
            throw new AppException(ErrorCode.BENEFICIARY_INVALID_BA);
        }
        return queryBeneficiaryBA;
    }

    // Method to get list of beneficiaries for BA by customer id
    @Override
    public List<Map<String, Object>> getBeneficiariesOfBankAccount(
            Long customerId
    ){
        List<BeneficiaryEntity> beneficiariesOfBA = beneficiaryRepository
                .findBeneficiariesOfBankAccountByCustomerId(customerId);

        if (beneficiariesOfBA.isEmpty()) {
            throw new AppException(ErrorCode.BENEFICIARY_BA_LIST_NOT_FOUND);
        }

        return beneficiariesOfBA.stream().map(beneficiary -> {
            Map<String, Object> beneficiaryMap = new HashMap<>();
            beneficiaryMap.put("id", beneficiary.getId());
            beneficiaryMap.put("name", beneficiary.getName());
            beneficiaryMap.put("customer_id", beneficiary.getCustomer().getId());
            beneficiaryMap.put("receiver_id", beneficiary.getReceiverId());
            beneficiaryMap.put("beneficiary_receiver_type", beneficiary.getBeneficiaryReceiverType());

            String accountNumber = bankAccountRepository
                    .findById(beneficiary.getReceiverId()).get().getBankAccountNumber();
            beneficiaryMap.put("account_number", accountNumber);
            BankAccountEntity bankAccount = bankAccountRepository.findById(beneficiary.getReceiverId()).get();
            beneficiaryMap.put("bank_name", bankRepository.findById(bankAccount.getBank().getId()).get().getName());
            return beneficiaryMap;
        }).toList();
    }

    // Method to get list of all beneficiaries by customer id
    @Override
    public List<BeneficiaryEntity> getAllBeneficiaries(
            Long customerId
    ) {
        List<BeneficiaryEntity> allBeneficiaries = beneficiaryRepository.findAllBeneficiariesByCustomerId(customerId);

        if (allBeneficiaries.isEmpty()) {
            throw new AppException(ErrorCode.BENEFICIARY_LIST_ALL_NOT_FOUND);
        }

        return allBeneficiaries;
    }

    @Override
    public BeneficiaryListResponse getPaginationListBeneficiary(
            Integer page, Integer size, String orderedBy, Boolean isAscending, String keyword
    ) {
        Long totalQuantity;
        Page<BeneficiaryEntity> beneficiaryPage;

        // Get ascending or descending sort
        Sort sort = Boolean.TRUE.equals(isAscending)
                ? Sort.by(orderedBy).ascending()
                : Sort.by(orderedBy).descending();

        try {
            beneficiaryPage = beneficiaryRepository.findByNameContainingIgnoreCase(
                    keyword, PageRequest.of(page - 1, size, sort));
            totalQuantity = beneficiaryPage.getTotalElements();
        }
        catch (Exception e){
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        List<BeneficiaryResponse> beneficiaryResponses = beneficiaryPage.stream()
                .map(BeneficiaryResponse::fromBeneficiary)
                .toList();

        return BeneficiaryListResponse.builder()
                .beneficiaries(beneficiaryResponses)
                .totalQuantity(totalQuantity)
                .build();
    }

    // Method to update name of beneficiary for PA
    @Override
    public BeneficiaryEntity updateNameOfPaymentAccountBeneficiary(
            Long beneficiaryId,
            BeneficiaryRequest beneficiaryRequest
    ) {
        // Get existing BeneficiaryEntity
        BeneficiaryEntity updateNameOfPaymentAccountBeneficiary = getBeneficiaryOfPaymentAccountById(beneficiaryId);

        // Check if updateName different with old name of same BeneficiaryEntity
        if(beneficiaryRequest.getName().equals(updateNameOfPaymentAccountBeneficiary.getName())){
            throw new AppException(ErrorCode.BENEFICIARY_NAME_OLD);
        }

        // Fetch customer throw an AppException if not found
        CustomerEntity existingCustomer = customerRepository.findById(beneficiaryRequest.getCustomerId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check if updateName exist of the list of customer's beneficiaries
        if(beneficiaryRepository.existsBeneficiaryEntityByCustomerAndName(existingCustomer, beneficiaryRequest.getName())){
            throw new AppException(ErrorCode.BENEFICIARY_NAME_EXISTS);
        }

        updateNameOfPaymentAccountBeneficiary.setName(beneficiaryRequest.getName());
        return beneficiaryRepository.save(updateNameOfPaymentAccountBeneficiary);
    }

    // Method to update name of beneficiary for BA
    @Override
    public BeneficiaryEntity updateNameOfBankAccountBeneficiary(
            Long beneficiaryId,
            BeneficiaryRequest beneficiaryRequest
    ) {
        // Get existing BeneficiaryEntity
        BeneficiaryEntity updateNameOfBankAccountBeneficiary = getBeneficiaryOfBankAccountById(beneficiaryId);

        // Check if updateName different with old name of same BeneficiaryEntity
        if(beneficiaryRequest.getName().equals(updateNameOfBankAccountBeneficiary.getName())){
            throw new AppException(ErrorCode.BENEFICIARY_NAME_OLD);
        }

        // Fetch customer throw an AppException if not found
        CustomerEntity existingCustomer = customerRepository.findById(beneficiaryRequest.getCustomerId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check if updateName exist of the list of customer's beneficiaries
        if(beneficiaryRepository.existsBeneficiaryEntityByCustomerAndName(existingCustomer, beneficiaryRequest.getName())){
            throw new AppException(ErrorCode.BENEFICIARY_NAME_EXISTS);
        }

        updateNameOfBankAccountBeneficiary.setName(beneficiaryRequest.getName());
        return beneficiaryRepository.save(updateNameOfBankAccountBeneficiary);
    }

    // Method to delete a beneficiary for PA
    @Override
    public void deleteBeneficiaryOfPaymentAccount(
            Long beneficiaryId
    ){
        BeneficiaryEntity deleteBeneficiary = getBeneficiaryOfPaymentAccountById(beneficiaryId);
        beneficiaryRepository.delete(deleteBeneficiary);
    }

    // Method to delete a beneficiary for BA
    @Override
    public void deleteBeneficiaryOfBankAccount(
            Long beneficiaryId
    ){
        BeneficiaryEntity deleteBeneficiary = getBeneficiaryOfBankAccountById(beneficiaryId);
        beneficiaryRepository.delete(deleteBeneficiary);
    }
}
