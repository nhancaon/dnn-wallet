package com.example.onlinebankingapp.dtos.requests.BankAccount;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BankAccountRequest {
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
}