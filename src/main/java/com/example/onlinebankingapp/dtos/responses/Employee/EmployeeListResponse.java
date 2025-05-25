package com.example.onlinebankingapp.dtos.responses.Employee;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Data
@Builder
@NoArgsConstructor
public class EmployeeListResponse {
    private List<EmployeeResponse> employeeResponses;

    @JsonProperty("total_quantity")
    private Long totalQuantity;
}
