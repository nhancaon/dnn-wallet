package com.example.onlinebankingapp.dtos.requests.PaymentAccount;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
public class AmountOperationRequest {
    @JsonProperty("amount")
    private double amount;

    @JsonProperty("otp")
    private String otp;
}
