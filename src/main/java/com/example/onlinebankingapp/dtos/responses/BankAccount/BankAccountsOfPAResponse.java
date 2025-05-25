package com.example.onlinebankingapp.dtos.responses.BankAccount;

import com.example.onlinebankingapp.entities.BankAccountEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BankAccountsOfPAResponse {
    private Long id;

    @JsonProperty("bank_id")
    private Long bankId;

    @JsonProperty("bank_account_number")
    private String bankAccountNumber;

    public static BankAccountsOfPAResponse generateAccountsOfPAResponse(BankAccountEntity bankAccount) {
        return BankAccountsOfPAResponse.builder()
                .id(bankAccount.getId())
                .bankId(bankAccount.getBank().getId())
                .bankAccountNumber(bankAccount.getBankAccountNumber())
                .build();
    }
}
