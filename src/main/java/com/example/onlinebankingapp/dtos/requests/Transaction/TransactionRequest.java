package com.example.onlinebankingapp.dtos.requests.Transaction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionRequest {
    @JsonProperty("transaction_type")
    private String transactionType;

    @JsonProperty("amount_type")
    private String amountType;

    private Double amount;

    @JsonProperty("transaction_date_time")
    private LocalDateTime transactionDateTime;

    @JsonProperty("transaction_status")
    private String transactionStatus;

    @JsonProperty("transaction_remark")
    private String transactionRemark;

    @JsonProperty("sender_id")
    private Long senderId;

    @JsonProperty("transaction_sender_type")
    private String transactionSenderType;

    @JsonProperty("receiver_id")
    private Long receiverId;

    @JsonProperty("transaction_receiver_type")
    private String transactionReceiverType;
}
