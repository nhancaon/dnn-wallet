package com.example.onlinebankingapp.dtos.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InterestRateRequest {
    @JsonProperty("interest_rate")
    private Double interestRate;

    @JsonProperty("term")
    private Integer term;

    @JsonProperty("min_balance")
    private Double minBalance;
}
