package com.example.onlinebankingapp.dtos.requests.BankAccount;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BankAccountActiveRequest extends BankAccountRequest {
    private String otp;
}