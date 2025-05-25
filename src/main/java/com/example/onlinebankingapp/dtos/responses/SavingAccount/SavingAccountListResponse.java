package com.example.onlinebankingapp.dtos.responses.SavingAccount;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class SavingAccountListResponse {
    private List<SavingAccountResponse> savingAccounts;

    @JsonProperty("total_quantity")
    private Long totalQuantity;
}
