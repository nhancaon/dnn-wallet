package com.example.onlinebankingapp.dtos.responses.Reward;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class RewardListResponse {
    private List<RewardResponse> rewardResponses;

    @JsonProperty("total_quantity")
    private Long totalQuantity;
}
