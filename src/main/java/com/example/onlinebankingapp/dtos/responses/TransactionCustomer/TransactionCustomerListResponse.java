package com.example.onlinebankingapp.dtos.responses.TransactionCustomer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class TransactionCustomerListResponse {
    private List<TransactionCustomerResponse> transactionCustomers;

    @JsonProperty("total_quantity")
    private Long totalQuantity;
}
