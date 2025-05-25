package com.example.onlinebankingapp.dtos.requests.PaymentAccount;

import com.example.onlinebankingapp.dtos.requests.BankAccount.BankAccountRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MoneyFlowRequest {
    @JsonProperty("bankAccountRequest")
    private BankAccountRequest bankAccountRequest;

    @JsonProperty("amountOperationRequest")
    private AmountOperationRequest amountOperationRequest;
}
