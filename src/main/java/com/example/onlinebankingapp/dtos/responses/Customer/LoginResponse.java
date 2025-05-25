package com.example.onlinebankingapp.dtos.responses.Customer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse {
    @JsonProperty("token")
    private String token;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long id;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("role")
    private String role;
}
