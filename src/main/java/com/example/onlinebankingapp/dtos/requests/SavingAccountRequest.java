package com.example.onlinebankingapp.dtos.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SavingAccountRequest extends AbstractAccountRequest {
    @JsonProperty("saving_current_amount")
    private Double savingCurrentAmount;

    @JsonProperty("saving_initial_amount")
    private Double savingInitialAmount;

    @JsonProperty("payment_account_id")
    private Long paymentAccountId;

    @JsonProperty("interest_rate_id")
    private Long interestRateId;
}
