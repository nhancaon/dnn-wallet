package com.example.onlinebankingapp.dtos.responses.SavingAccount;

import com.example.onlinebankingapp.entities.SavingAccountEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SavingAccountResponse {
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

    @JsonProperty("saving_current_amount")
    private Double savingCurrentAmount;

    @JsonProperty("saving_initial_amount")
    private Double savingInitialAmount;

    @JsonProperty("payment_account_id")
    private Long paymentAccountId;

    @JsonProperty("interest_rate_id")
    private Long interestRateId;

    //static method to create savingAccountResponse from SavingAccountEntity
    public static SavingAccountResponse fromSavingAccount(SavingAccountEntity savingAccount){
        return SavingAccountResponse.builder()
                .id(savingAccount.getId())
                .accountNumber(savingAccount.getAccountNumber())
                .accountStatus(savingAccount.getAccountStatus().name())
                .accountType(savingAccount.getAccountType().name())
                .dateClosed(savingAccount.getDateClosed())
                .dateOpened(savingAccount.getDateOpened())
                .savingCurrentAmount(savingAccount.getSavingCurrentAmount())
                .savingInitialAmount(savingAccount.getSavingInitialAmount())
                .paymentAccountId(savingAccount.getPaymentAccount().getId())
                .interestRateId(savingAccount.getInterestRate().getId())
                .build();
    }
}
