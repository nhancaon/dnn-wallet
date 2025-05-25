package com.example.onlinebankingapp.services.InterestRate;

import com.example.onlinebankingapp.dtos.requests.InterestRateRequest;
import com.example.onlinebankingapp.dtos.responses.InterestRate.InterestRateListResponse;
import com.example.onlinebankingapp.entities.InterestRateEntity;

import java.util.List;

public interface InterestRateService {
    InterestRateEntity insertInterestRate(InterestRateRequest interestRateRequest);
    List<InterestRateEntity> getAllInterestRates();
    InterestRateListResponse getPaginationListInterestRate(Integer page, Integer size, String orderedBy, Boolean isAscending, Integer keyword);
    InterestRateEntity getInterestRateById(Long id);
    InterestRateEntity updateInterestRate(Long id, InterestRateRequest interestRateRequest);
    void deleteInterestRateById(long id);
}
