package com.example.onlinebankingapp.dtos.requests.Transaction.TransactionToBA;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionToBAFromPARequest {
    @JsonProperty("transaction_id")
    private Long transactionId;

    @JsonProperty("receiver_bank_account_id")
    private Long receiverBankAccountId;

    @JsonProperty("sender_payment_account_id")
    private Long senderPaymentAccountId;
}
