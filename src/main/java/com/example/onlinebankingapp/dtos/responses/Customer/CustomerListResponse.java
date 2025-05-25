package com.example.onlinebankingapp.dtos.responses.Customer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class CustomerListResponse {
    private List<CustomerResponse> customerResponses;

    @JsonProperty("total_quantity")
    private Long totalQuantity;
}
