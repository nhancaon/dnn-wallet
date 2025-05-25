package com.example.onlinebankingapp.dtos.requests.Employee;

import com.example.onlinebankingapp.dtos.requests.AbstractUserRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeRequest extends AbstractUserRequest {
    @JsonProperty("role")
    private String role;
}
