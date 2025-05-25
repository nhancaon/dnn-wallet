package com.example.onlinebankingapp.dtos.requests.Transaction.TransactionToBA;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionToBAFromBARequest {
    @JsonProperty("transaction_id")
    private Long transactionId;

    @JsonProperty("receiver_bank_account_id")
    private Long receiverBankAccountId;

    @JsonProperty("sender_bank_account_id")
    private Long senderBankAccountId;
}
