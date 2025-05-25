package com.example.onlinebankingapp.services.Token;

import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import org.springframework.stereotype.Service;
import com.example.onlinebankingapp.repositories.TokenCustomerRepository;
import com.example.onlinebankingapp.entities.CustomerEntity;
import com.example.onlinebankingapp.entities.TokenCustomerEntity;
import com.example.onlinebankingapp.components.JwtTokenUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenCustomerServiceImpl implements TokenCustomerService {
    //jwt settings
    private static final int MAX_TOKENS = 3;
    @Value("${jwt.expiration}")
    private int expiration;
    @Value("${jwt.expiration-refresh-token}")
    private int expirationRefreshToken;

    private final TokenCustomerRepository tokenCustomerRepository;
    private final JwtTokenUtils jwtTokenUtil;

    // Refresh a token
    @Transactional
    @Override
    public TokenCustomerEntity refreshTokenForCustomer(String refreshToken, CustomerEntity customer) {
        //find the refresh token
        TokenCustomerEntity existingToken = tokenCustomerRepository.findByRefreshToken(refreshToken);
        //check if the requested token exists
        if(existingToken == null) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        //check if it has expired
        if(existingToken.getRefreshExpirationDate().compareTo(LocalDateTime.now()) < 0){
            tokenCustomerRepository.delete(existingToken);
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }
        //generate new jwt token
        String token = jwtTokenUtil.generateTokenForCustomer(customer);
        LocalDateTime expirationDateTime = LocalDateTime.now().plusSeconds(expiration);
        existingToken.setExpirationDate(expirationDateTime);
        existingToken.setToken(token);
        existingToken.setRefreshToken(UUID.randomUUID().toString());
        existingToken.setRefreshExpirationDate(LocalDateTime.now().plusSeconds(expirationRefreshToken));
        return existingToken;
    }

    // Add a new token for a customer
    @Transactional
    @Override
    public TokenCustomerEntity addTokenForCustomer(CustomerEntity customer, String token, boolean isMobileDevice) {
        List<TokenCustomerEntity> userTokens = tokenCustomerRepository.findByCustomer(customer);
        int tokenCount = userTokens.size();
        // Số lượng token vượt quá giới hạn, xóa một token cũ
        if (tokenCount >= MAX_TOKENS) {
            //kiểm tra xem trong danh sách userTokens có tồn tại ít nhất
            //một token không phải là thiết bị di động (non-mobile)
            boolean hasNonMobileToken = !userTokens.stream().allMatch(TokenCustomerEntity::isMobile);
            TokenCustomerEntity tokenToDelete;
            if (hasNonMobileToken) {
                tokenToDelete = userTokens.stream()
                        .filter(userToken -> !userToken.isMobile())
                        .findFirst()
                        .orElse(userTokens.get(0));
            } else {
                //tất cả các token đều là thiết bị di động,
                //chúng ta sẽ xóa token đầu tiên trong danh sách
                tokenToDelete = userTokens.get(0);
            }
            tokenCustomerRepository.delete(tokenToDelete);
        }
        long expirationInSeconds = expiration;
        LocalDateTime expirationDateTime = LocalDateTime.now().plusSeconds(expirationInSeconds);
        // Tạo mới một token cho người dùng
        TokenCustomerEntity newToken = TokenCustomerEntity.builder()
                .customer(customer)
                .token(token)
                .revoked(false)
                .expired(false)
                .tokenType("Bearer")
                .expirationDate(expirationDateTime)
                .isMobile(isMobileDevice)
                .build();

        newToken.setRefreshToken(UUID.randomUUID().toString());
        newToken.setRefreshExpirationDate(LocalDateTime.now().plusSeconds(expirationRefreshToken));
        tokenCustomerRepository.save(newToken);
        return newToken;
    }
}
