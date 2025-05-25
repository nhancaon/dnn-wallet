package com.example.onlinebankingapp.dtos.responses.AccountReward;

import com.example.onlinebankingapp.entities.AccountRewardEntity;
import com.example.onlinebankingapp.entities.PaymentAccountEntity;
import com.example.onlinebankingapp.entities.RewardEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountRewardResponse {
    @JsonProperty("reward_id")
    private Long rewardId;

    @JsonProperty("payment_account_id")
    private Long paymentAccountId;

    @JsonProperty("is_valid")
    private Boolean isValid;

    @JsonProperty("cost_point")
    private Integer costPoint;

    @JsonProperty("reward_name")
    private String rewardName;

    @JsonProperty("reward_type")
    private String rewardType;

    @JsonProperty("image_hashed_name")
    private String imageHashedName;

    // Static method to create an AccountRewardResponse object from an AccountRewardEntity object
    public static AccountRewardResponse fromAccountReward(AccountRewardEntity accountReward){
        RewardEntity reward = accountReward.getAccountReward().getReward();
        PaymentAccountEntity paymentAccount = accountReward.getAccountReward().getPaymentAccount();

        return AccountRewardResponse.builder()
                .rewardId(reward.getId())
                .paymentAccountId(paymentAccount.getId())
                .rewardName(reward.getRewardName())
                .costPoint(reward.getCostPoint())
                .rewardType(String.valueOf(reward.getRewardType()))
                .imageHashedName(reward.getImageHashedName())
                .isValid(accountReward.isValid())
                .build();
    }
}
