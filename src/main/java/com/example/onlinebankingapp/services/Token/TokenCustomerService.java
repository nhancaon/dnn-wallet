package com.example.onlinebankingapp.services.Token;

import com.example.onlinebankingapp.entities.CustomerEntity;
import com.example.onlinebankingapp.entities.TokenCustomerEntity;
import org.springframework.stereotype.Service;

@Service
public interface TokenCustomerService {
    TokenCustomerEntity addTokenForCustomer(CustomerEntity customer, String token, boolean isMobileDevice);
    TokenCustomerEntity refreshTokenForCustomer(String refreshToken, CustomerEntity customer);
}
