package com.example.onlinebankingapp.dtos.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountRewardRequest {
    @JsonProperty("reward_id")
    private Long rewardId;

    @JsonProperty("payment_account_id")
    private Long paymentAccountId;

    @JsonProperty("is_valid")
    private Boolean isValid;
}
