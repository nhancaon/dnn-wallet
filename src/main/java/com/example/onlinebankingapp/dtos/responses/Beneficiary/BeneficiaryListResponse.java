package com.example.onlinebankingapp.dtos.responses.Beneficiary;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class BeneficiaryListResponse {
    private List<BeneficiaryResponse> beneficiaries;

    @JsonProperty("total_quantity")
    private Long totalQuantity;
}
