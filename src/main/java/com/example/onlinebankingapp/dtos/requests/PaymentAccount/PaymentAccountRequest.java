package com.example.onlinebankingapp.dtos.requests.PaymentAccount;

import com.example.onlinebankingapp.dtos.requests.AbstractAccountRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentAccountRequest extends AbstractAccountRequest {
    @JsonProperty("current_balance")
    private Double currentBalance;

    @JsonProperty("reward_point")
    private Integer rewardPoint;

    @JsonProperty("customer_id")
    private Long customerId;
}