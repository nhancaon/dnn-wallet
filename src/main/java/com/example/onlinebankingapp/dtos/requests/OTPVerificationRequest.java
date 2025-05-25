package com.example.onlinebankingapp.dtos.requests;

import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionRequest;
import lombok.Data;

@Data
public class OTPVerificationRequest {
    private String receiverEmail;
    private String otp;
    private TransactionRequest transactionRequest;
}

