package com.example.onlinebankingapp.repositories;

import com.example.onlinebankingapp.entities.InterestRateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InterestRateRepository extends JpaRepository<InterestRateEntity, Long> {
    boolean existsByTermEqualsAndInterestRateEqualsAndMinBalanceEquals(Integer term, Double interestRate, Double minBalance);
    Page<InterestRateEntity> findByTerm(Integer term, Pageable pageable);
}
