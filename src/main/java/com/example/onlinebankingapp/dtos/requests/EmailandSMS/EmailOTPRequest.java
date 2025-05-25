package com.example.onlinebankingapp.dtos.requests.EmailandSMS;

import com.example.onlinebankingapp.enums.OTPPurpose;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EmailOTPRequest {
    private String otp;

    private LocalDateTime sendingTime;

    private long expirationMinute;

    private OTPPurpose otpPurpose;
}
