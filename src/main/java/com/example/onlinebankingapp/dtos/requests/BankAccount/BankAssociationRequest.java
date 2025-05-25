package com.example.onlinebankingapp.dtos.requests.BankAccount;

import com.example.onlinebankingapp.dtos.requests.PaymentAccount.PaymentAccountRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BankAssociationRequest {
    @JsonProperty("bankAccountActiveRequest")
    private BankAccountActiveRequest bankAccountActiveRequest;

    @JsonProperty("paymentAccountRequest")
    private PaymentAccountRequest paymentAccountRequest;
}