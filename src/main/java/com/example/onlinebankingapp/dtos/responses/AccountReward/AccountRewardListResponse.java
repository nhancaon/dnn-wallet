package com.example.onlinebankingapp.dtos.responses.AccountReward;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class AccountRewardListResponse {
    private List<AccountRewardResponse> accountRewards;
    private int totalPages;
}
