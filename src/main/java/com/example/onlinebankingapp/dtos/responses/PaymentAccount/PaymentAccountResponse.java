package com.example.onlinebankingapp.dtos.responses.PaymentAccount;

import com.example.onlinebankingapp.entities.PaymentAccountEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentAccountResponse {
    private Long id;

    @JsonProperty("account_number")
    private String accountNumber;

    @JsonProperty("account_status")
    private String accountStatus;

    @JsonProperty("account_type")
    private String accountType;

    @JsonProperty("date_closed")
    private LocalDateTime dateClosed;

    @JsonProperty("date_opened")
    private LocalDateTime dateOpened;

    @JsonProperty("current_balance")
    private Double currentBalance;

    @JsonProperty("reward_point")
    private Integer rewardPoint;

    @JsonProperty("customer_id")
    private Long customerId;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("customer_avatar")
    private String customerAvatar;

    // Static method to convert PaymentAccountEntity to PaymentAccountResponse
    public static PaymentAccountResponse fromPaymentAccount(PaymentAccountEntity paymentAccount) {
        return PaymentAccountResponse
                .builder()
                .id(paymentAccount.getId())
                .accountNumber(paymentAccount.getAccountNumber())
                .accountType(paymentAccount.getAccountType().name())
                .accountStatus(paymentAccount.getAccountStatus().name())
                .currentBalance(paymentAccount.getCurrentBalance())
                .rewardPoint(paymentAccount.getRewardPoint())
                .customerId(paymentAccount.getCustomer().getId())
                .customerName(paymentAccount.getCustomer().getName())
                .customerAvatar(paymentAccount.getCustomer().getAvatarHashedName())
                .dateOpened(paymentAccount.getDateOpened())
                .dateClosed(paymentAccount.getDateClosed())
                .build();
    }
}
