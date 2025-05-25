package com.example.onlinebankingapp.dtos.responses.InterestRate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class InterestRateListResponse {
    private List<InterestRateResponse> interestRateResponses;

    @JsonProperty("total_quantity")
    private Long totalQuantity;
}
