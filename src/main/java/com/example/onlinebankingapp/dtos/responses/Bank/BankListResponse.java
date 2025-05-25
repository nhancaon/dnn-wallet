package com.example.onlinebankingapp.dtos.responses.Bank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class BankListResponse {
    private List<BankResponse> banks;
    private int totalPages;
}
