package com.example.onlinebankingapp.dtos.requests.Transaction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionCustomerRequest {
    @JsonProperty("transaction_id")
    private Long transactionId;

    @JsonProperty("customer_id")
    private Long customerId;
}
