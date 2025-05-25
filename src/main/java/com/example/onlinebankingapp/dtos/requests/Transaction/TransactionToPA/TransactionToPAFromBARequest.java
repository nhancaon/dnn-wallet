package com.example.onlinebankingapp.dtos.requests.Transaction.TransactionToPA;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionToPAFromBARequest {
    @JsonProperty("transaction_id")
    private Long transactionId;

    @JsonProperty("receiver_payment_account_id")
    private Long receiverPaymentAccountId;

    @JsonProperty("sender_bank_account_id")
    private Long senderBankAccountId;
}
