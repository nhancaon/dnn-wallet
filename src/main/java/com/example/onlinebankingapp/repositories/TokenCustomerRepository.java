package com.example.onlinebankingapp.repositories;

import com.example.onlinebankingapp.entities.TokenCustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.onlinebankingapp.entities.CustomerEntity;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TokenCustomerRepository extends JpaRepository<TokenCustomerEntity, Long> {
    // Find tokens associated with a specific customer
    List<TokenCustomerEntity> findByCustomer(CustomerEntity customer);
    // Find a token by its token string
    TokenCustomerEntity findByToken(String token);
    // Find a token by its refresh token string
    TokenCustomerEntity findByRefreshToken(String refreshToken);
}
