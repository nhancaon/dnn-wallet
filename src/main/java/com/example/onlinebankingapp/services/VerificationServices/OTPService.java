package com.example.onlinebankingapp.services.VerificationServices;

import com.example.onlinebankingapp.dtos.requests.EmailandSMS.EmailOTPRequest;
import com.example.onlinebankingapp.dtos.requests.EmailandSMS.SMSOTPRequest;
import com.example.onlinebankingapp.enums.OTPPurpose;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OTPService {
    // Storage for OTPs mapped to email addresses
    private final Map<String, EmailOTPRequest> otpEmailStorage = new ConcurrentHashMap<>();

    // Storage for OTPs mapped to phone SMS
    private final Map<String, SMSOTPRequest> otpSMSStorage = new HashMap<>();

    public String generateOTP(String email, String smsPhoneNumber, long expirationMinute, OTPPurpose otpPurpose) {
        // Generate a random 6-digit OTP
        String otp = new DecimalFormat("000000")
                .format(new Random().nextInt(999999)); // Generate a 6 digit OTP

        if(smsPhoneNumber.isEmpty() || smsPhoneNumber.isBlank()) {
            // Store the OTP in the storage map with the email as the key
            EmailOTPRequest emailDTO = new EmailOTPRequest(otp, LocalDateTime.now(), expirationMinute, otpPurpose);
            System.out.println("Bay gio la: " + LocalDateTime.now());
            System.out.println("emailDTO vua moi tao: " + emailDTO);
            otpEmailStorage.put(email, emailDTO);
        }
        else if (email.isEmpty() || email.isBlank()) {
            // Store the OTP in the storage map with the smsDTO as the key
            SMSOTPRequest smsDTO = new SMSOTPRequest(otp, LocalDateTime.now(), expirationMinute, otpPurpose);
            System.out.println("Bay gio la: " + LocalDateTime.now());
            System.out.println("smsDTO vua moi tao: " + smsDTO);
            otpSMSStorage.put(smsPhoneNumber, smsDTO);
        }
        return otp;
    }

    public boolean verifyOTP(String email, String smsPhoneNumber, String otp, OTPPurpose otpPurpose) {
        if(smsPhoneNumber.isEmpty() || smsPhoneNumber.isBlank()) {
            EmailOTPRequest emailDTO = otpEmailStorage.get(email);
            if(emailDTO != null) {
                if (emailDTO.getOtp().equals(otp)) {
                    // Check OTP purpose
                    if(emailDTO.getOtpPurpose().equals(otpPurpose)){
                        // Check if OTP has expired
                        if (LocalDateTime.now().isBefore(emailDTO.getSendingTime().plusMinutes(emailDTO.getExpirationMinute()))) {
                            // Remove OTP after successful verification
                            otpEmailStorage.remove(email);
                            return true;
                        } else {
                            // OTP has expired
                            // Remove expired OTP
                            otpEmailStorage.remove(email);
                            throw new AppException(ErrorCode.OTP_EXPIRED);
                        }
                    }
                    else {
                        throw new AppException(ErrorCode.OTP_PURPOSE_MISMATCH);
                    }
                }
                else{
                    throw new AppException(ErrorCode.OTP_INVALID);
                }
            }
            else{
                throw new AppException(ErrorCode.EMAIL_OTP_NOT_FOUND);
            }
        }
        else if (email.isEmpty() || email.isBlank()) {
            SMSOTPRequest smsDTO = otpSMSStorage.get(smsPhoneNumber);
            if(smsDTO != null) {
                if (smsDTO.getOtp().equals(otp)) {
                    // Check OTP purpose
                    if(smsDTO.getOtpPurpose().equals(otpPurpose)){
                        // Check if OTP has expired
                        if (LocalDateTime.now().isBefore(smsDTO.getSendingTime().plusMinutes(smsDTO.getExpirationMinute()))) {
                            // Remove OTP after successful verification
                            otpSMSStorage.remove(smsPhoneNumber);
                            return true;
                        } else {
                            // OTP has expired
                            // Remove expired OTP
                            otpSMSStorage.remove(smsPhoneNumber);
                            throw new AppException(ErrorCode.OTP_EXPIRED);
                        }
                    }
                    else {
                        return false;
                    }
                }
                else{
                    return false;
                }
            }
            else{
                return false;
            }
        }
        return false;
    }
}