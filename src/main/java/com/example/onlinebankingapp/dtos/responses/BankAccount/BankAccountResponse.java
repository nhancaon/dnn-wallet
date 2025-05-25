package com.example.onlinebankingapp.dtos.responses.BankAccount;

import com.example.onlinebankingapp.entities.BankAccountEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BankAccountResponse {
    private Long id;

    @JsonProperty("bank_account_number")
    private String bankAccountNumber;

    @JsonProperty("citizen_id")
    private String citizenId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("current_balance")
    private Double currentBalance;

    @JsonProperty("bank_id")
    private Long bankId;

    @JsonProperty("payment_account_id")
    private Long paymentAccountId;

    @JsonProperty("bank_name")
    private String bankName;

    public static BankAccountResponse fromBankAccount(BankAccountEntity bankAccount) {
        return BankAccountResponse
                .builder()
                .id(bankAccount.getId())
                .bankAccountNumber(bankAccount.getBankAccountNumber())
                .citizenId(bankAccount.getCitizenId())
                .name(bankAccount.getName())
                .bankName(bankAccount.getBank().getName())
                .phoneNumber(bankAccount.getPhoneNumber())
                .currentBalance(bankAccount.getCurrentBalance())
                .bankId(bankAccount.getBank().getId())
                .currentBalance(bankAccount.getCurrentBalance())
                .paymentAccountId(bankAccount.getPaymentAccount() != null ? bankAccount.getPaymentAccount().getId() : null)
                .build();
    }


    public static BankAccountResponse generateAccountsOfPAResponse(BankAccountEntity bankAccount) {
        return BankAccountResponse
                .builder()
                .id(bankAccount.getId())
                .bankId(bankAccount.getBank().getId())
                .build();
    }
}
