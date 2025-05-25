package com.example.onlinebankingapp.dtos.responses.Customer;

import com.example.onlinebankingapp.entities.CustomerEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerResponse {
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

    @JsonProperty("pin_number")
    private String pinNumber;

    @JsonProperty("avatar_hashed_name")
    private String avatarHashedName;

    @JsonProperty("is_active")
    private Boolean isActive;

    // Static method to create a CustomerResponse object from a CustomerEntity object
    public static CustomerResponse fromCustomerResponse(CustomerEntity customerEntity) {
        return CustomerResponse
                .builder()
                .id(customerEntity.getId())
                .email(customerEntity.getEmail())
                .name(customerEntity.getName())
                .phoneNumber(customerEntity.getPhoneNumber())
                .address(customerEntity.getAddress())
                .citizenId(customerEntity.getCitizenId())
                .dateOfBirth(customerEntity.getDateOfBirth())
                .pinNumber(customerEntity.getPinNumber())
                .avatarHashedName(customerEntity.getAvatarHashedName())
                .isActive(customerEntity.isActive())
                .build();
    }
}
