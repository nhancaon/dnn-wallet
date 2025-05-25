package com.example.onlinebankingapp.services.VerificationServices;

import com.example.onlinebankingapp.dtos.responses.SMSOTPResponse;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class SMSService {
    private final OTPService otpService;

    @Value("${spring.sms.access.token}")
    private String accessToken;

    @Value("${spring.sms.access.deviceId}")
    private String deviceId; // sender -> Android phone

    // Send OTP -> DO NOT CHANGE anything
    public SMSOTPResponse sendSMSOTP(String phoneTo, String content) {
        SpeedSMSAPI api = new SpeedSMSAPI(accessToken);

        // Convert the phone number from "0911111111" to "+84911111111"
        if (phoneTo.startsWith("0") && phoneTo.length() == 10) {
            phoneTo = "+84" + phoneTo.substring(1); // Replace '0' with '+84'
        }

        int type = 5; // Send via SMS Gateway Android using my own phone number

        try {
            String smsResponse = api.sendSMS(phoneTo, content, type, deviceId);
            SMSOTPResponse smsotpResponse = parseOTPResponse(smsResponse);
            if(smsotpResponse == null){
                throw new AppException(ErrorCode.SMS_OTP_NOT_FOUND);
            }
            return smsotpResponse;
        } catch (IOException e) {
            e.printStackTrace();
            throw new AppException(ErrorCode.SMS_OTP_FAIL);
        }
    }

    public SMSOTPResponse parseOTPResponse(String smsResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // Deserialize the JSON string into the OTPResponseDTO object
            return objectMapper.readValue(smsResponse, SMSOTPResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
