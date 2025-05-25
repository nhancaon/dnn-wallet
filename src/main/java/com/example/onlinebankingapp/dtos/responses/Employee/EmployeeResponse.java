package com.example.onlinebankingapp.dtos.responses.Employee;

import com.example.onlinebankingapp.entities.EmployeeEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeResponse {
    private Long id;

    @JsonProperty("email")
    private String email;

    @JsonProperty("name")
    private String name;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("address")
    private String address;

    @JsonProperty("citizen_id")
    private String citizenId;

    @JsonProperty("date_of_birth")
    private Date dateOfBirth;

    @JsonProperty("role")
    private String role;

    // Static method to create a EmployeeResponse object from a EmployeeEntity object
    public static EmployeeResponse fromEmployeeResponse(EmployeeEntity employeeEntity) {
        return EmployeeResponse
                .builder()
                .id(employeeEntity.getId())
                .email(employeeEntity.getEmail())
                .name(employeeEntity.getName())
                .phoneNumber(employeeEntity.getPhoneNumber())
                .address(employeeEntity.getAddress())
                .citizenId(employeeEntity.getCitizenId())
                .dateOfBirth(employeeEntity.getDateOfBirth())
                .role(employeeEntity.getRole())
                .build();
    }

}