package com.example.onlinebankingapp.services.InterestRate;

import com.example.onlinebankingapp.dtos.requests.InterestRateRequest;
import com.example.onlinebankingapp.dtos.responses.InterestRate.InterestRateListResponse;
import com.example.onlinebankingapp.dtos.responses.InterestRate.InterestRateResponse;
import com.example.onlinebankingapp.entities.InterestRateEntity;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import com.example.onlinebankingapp.repositories.InterestRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InterestRateServiceImpl implements InterestRateService{
    private final InterestRateRepository interestRateRepository;
    @Override
    public InterestRateEntity insertInterestRate(
            InterestRateRequest interestRateRequest
    ) {
        // Validate input data
        String validationResult = isInterestRateDTOValid(interestRateRequest);
        if(!validationResult.equals("OK")){
            throw new DataIntegrityViolationException(validationResult);
        }

        // Create a new InterestRateEntity
        InterestRateEntity newInterestRate = InterestRateEntity.builder()
                .term(interestRateRequest.getTerm())
                .interestRate(interestRateRequest.getInterestRate())
                .minBalance(interestRateRequest.getMinBalance())
                .build();

        // Save and return the new interest rate entity
        return interestRateRepository.save(newInterestRate);
    }

    @Override
    public List<InterestRateEntity> getAllInterestRates() {
        return interestRateRepository.findAll();
    }

    @Override
    public InterestRateListResponse getPaginationListInterestRate(
            Integer page, Integer size, String orderedBy, Boolean isAscending, Integer keyword
    ) {
        Long totalQuantity;
        Page<InterestRateEntity> interestRatePage;

        // Get ascending or descending sort
        Sort sort = Boolean.TRUE.equals(isAscending)
                ? Sort.by(orderedBy).ascending()
                : Sort.by(orderedBy).descending();

        try {
            if(keyword.equals(0) || keyword.toString().isBlank() || keyword.toString().isEmpty()){
                interestRatePage = interestRateRepository.findAll(PageRequest.of(page - 1, size, sort));
            }
            else{
                interestRatePage = interestRateRepository.findByTerm(
                        keyword, PageRequest.of(page - 1, size, sort));
            }
            totalQuantity = interestRatePage.getTotalElements();
        }
        catch (Exception e){
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        List<InterestRateResponse> interestRateResponses = interestRatePage.stream()
                .map(InterestRateResponse::fromInterestRate)
                .toList();

        return InterestRateListResponse.builder()
                .interestRateResponses(interestRateResponses)
                .totalQuantity(totalQuantity)
                .build();
    }

    @Override
    public InterestRateEntity getInterestRateById(
            Long interestRateId
    ) {
        Optional<InterestRateEntity> queryInterestRate = interestRateRepository.findById(interestRateId);
        if(queryInterestRate.isPresent()){
            return queryInterestRate.get();
        }
        throw new AppException(ErrorCode.INTEREST_RATE_NOT_FOUND);
    }

    @Override
    public InterestRateEntity updateInterestRate(
            Long id,
            InterestRateRequest interestRateRequest
    ) {
        InterestRateEntity updatedInterestRateEntity = getInterestRateById(id);

        // Validate input data
        String validationResult = isInterestRateDTOValid(interestRateRequest);
        if(!validationResult.equals("OK")){
            throw new DataIntegrityViolationException(validationResult);
        }

        // Update the interest rate entity
        updatedInterestRateEntity.setInterestRate(interestRateRequest.getInterestRate());
        updatedInterestRateEntity.setTerm(interestRateRequest.getTerm());
        updatedInterestRateEntity.setMinBalance(interestRateRequest.getMinBalance());

        // Save and return the updated interest rate entity
        return interestRateRepository.save(updatedInterestRateEntity);
    }

    @Override
    public void deleteInterestRateById(
            long id
    ) {
        InterestRateEntity deletedInterestRateEntity = getInterestRateById(id);
        interestRateRepository.delete(deletedInterestRateEntity);
    }

    private String isInterestRateDTOValid(
            InterestRateRequest interestRateRequest
    ){
        Integer term = interestRateRequest.getTerm();
        Double rate = interestRateRequest.getInterestRate();
        Double minBalance = interestRateRequest.getMinBalance();

        // Validate term
        if(term < 1 || term > 99){
            return "Term must be larger than 1 and smaller than 99 months";
        }

        // Validate interest rate
        if(rate <= 0 || rate > 99){
            return "Interest rate must be larger than 0% and smaller than 99%";
        }

        // Validate minimum balance
        if(minBalance <= 100000 || minBalance > 999999999){
            return "Minimum balance must be larger than 100.000VND and smaller than 999.999.999VND";
        }

        // Check for existing interest rate with the same attributes
        if(interestRateRepository.existsByTermEqualsAndInterestRateEqualsAndMinBalanceEquals(term, rate, minBalance)){
            return "This interest rate has been existed";
        }

        return "OK";
    }
}