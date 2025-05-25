package com.example.onlinebankingapp.dtos.responses.PaymentAccount;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class PaymentAccountListResponse {
    private List<PaymentAccountResponse> paymentAccounts;

    @JsonProperty("total_quantity")
    private Long totalQuantity;
}
