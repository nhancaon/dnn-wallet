package com.example.onlinebankingapp.dtos.requests.Customer;

import com.example.onlinebankingapp.dtos.requests.AbstractUserRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomerRequest extends AbstractUserRequest {
    @JsonProperty("pin_number")
    private String pinNumber;

    @JsonProperty("avatar_hashed_name")
    private String avatarHashedName;
}
