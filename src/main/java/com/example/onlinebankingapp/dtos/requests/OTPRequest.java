package com.example.onlinebankingapp.dtos.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OTPRequest {
    @JsonProperty("receiver_email")
    private String receiverEmail;

    @JsonProperty("phone_to")
    private String phoneTo;

    private LocalDateTime timestamp;
}
