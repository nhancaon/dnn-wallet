package com.example.onlinebankingapp.dtos.responses.BankAccount;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class BankAccountListResponse {
    private List<BankAccountResponse> bankAccounts;
    private int totalPages;
}
