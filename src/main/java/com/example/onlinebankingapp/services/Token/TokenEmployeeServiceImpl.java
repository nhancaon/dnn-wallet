package com.example.onlinebankingapp.services.Token;

import com.example.onlinebankingapp.components.JwtTokenUtils;
import com.example.onlinebankingapp.entities.EmployeeEntity;
import com.example.onlinebankingapp.entities.TokenEmployeeEntity;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import com.example.onlinebankingapp.repositories.TokenEmployeeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenEmployeeServiceImpl implements TokenEmployeeService {
    //jwt settings
    private static final int MAX_TOKENS = 3;
    @Value("${jwt.expiration}")
    private int expiration;
    @Value("${jwt.expiration-refresh-token}")
    private int expirationRefreshToken;

    private final TokenEmployeeRepository tokenEmployeeRepository;
    private final JwtTokenUtils jwtTokenUtil;

    @Transactional
    @Override
    public TokenEmployeeEntity refreshTokenForEmployee(String refreshToken, EmployeeEntity employee) {
        //find the refresh token
        TokenEmployeeEntity existingToken = tokenEmployeeRepository.findByRefreshToken(refreshToken);
        //check if the requested token exists
        if(existingToken == null) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        //check if it has expired
        if(existingToken.getRefreshExpirationDate().compareTo(LocalDateTime.now()) < 0){
            tokenEmployeeRepository.delete(existingToken);
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }
        //generate new jwt token
        String token = jwtTokenUtil.generateTokenForEmployee(employee);
        LocalDateTime expirationDateTime = LocalDateTime.now().plusSeconds(expiration);
        existingToken.setExpirationDate(expirationDateTime);
        existingToken.setToken(token);
        existingToken.setRefreshToken(UUID.randomUUID().toString());
        existingToken.setRefreshExpirationDate(LocalDateTime.now().plusSeconds(expirationRefreshToken));
        return existingToken;
    }

    // Add a new token for an employee
    @Transactional
    @Override
    public TokenEmployeeEntity addTokenForEmployee(EmployeeEntity employee, String token, boolean isMobileDevice) {
        List<TokenEmployeeEntity> userTokens = tokenEmployeeRepository.findByEmployee(employee);
        int tokenCount = userTokens.size();
        // Số lượng token vượt quá giới hạn, xóa một token cũ
        if (tokenCount >= MAX_TOKENS) {
            //kiểm tra xem trong danh sách userTokens có tồn tại ít nhất
            //một token không phải là thiết bị di động (non-mobile)
            boolean hasNonMobileToken = !userTokens.stream().allMatch(TokenEmployeeEntity::isMobile);
            TokenEmployeeEntity tokenToDelete;
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
            tokenEmployeeRepository.delete(tokenToDelete);
        }
        long expirationInSeconds = expiration;
        LocalDateTime expirationDateTime = LocalDateTime.now().plusSeconds(expirationInSeconds);
        // Tạo mới một token cho người dùng
        TokenEmployeeEntity newToken = TokenEmployeeEntity.builder()
                .employee(employee)
                .token(token)
                .revoked(false)
                .expired(false)
                .tokenType("Bearer")
                .expirationDate(expirationDateTime)
                .isMobile(isMobileDevice)
                .build();

        newToken.setRefreshToken(UUID.randomUUID().toString());
        newToken.setRefreshExpirationDate(LocalDateTime.now().plusSeconds(expirationRefreshToken));
        tokenEmployeeRepository.save(newToken);
        return newToken;
    }

    @Override
    public void deleteTokenByEmployeeId(EmployeeEntity employeeEntity) {
        List<TokenEmployeeEntity> deletedTokens = tokenEmployeeRepository.findByEmployee(employeeEntity);
        if (deletedTokens != null && !deletedTokens.isEmpty()) {
            tokenEmployeeRepository.deleteAll(deletedTokens);
        }
    }
}
