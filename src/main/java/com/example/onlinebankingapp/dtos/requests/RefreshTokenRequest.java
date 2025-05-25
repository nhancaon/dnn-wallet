package com.example.onlinebankingapp.dtos.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshTokenRequest {
    @JsonProperty("refresh_token")
    private String refreshToken;
}
