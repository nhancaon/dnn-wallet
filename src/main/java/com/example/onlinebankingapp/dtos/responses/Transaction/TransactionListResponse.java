package com.example.onlinebankingapp.dtos.responses.Transaction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class TransactionListResponse {
    private List<TransactionResponse> transactions;

    @JsonProperty("total_quantity")
    private Long totalQuantity;
}
