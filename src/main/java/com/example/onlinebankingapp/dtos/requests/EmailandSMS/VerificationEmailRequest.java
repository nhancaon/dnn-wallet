package com.example.onlinebankingapp.dtos.requests.EmailandSMS;

import com.example.onlinebankingapp.dtos.requests.Transaction.TransactionRequest;
import lombok.Data;

@Data
public class VerificationEmailRequest {
    private String receiverEmail;
    private String otp;
    private TransactionRequest transactionRequest;
}
