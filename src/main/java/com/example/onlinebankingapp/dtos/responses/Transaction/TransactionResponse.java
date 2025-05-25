package com.example.onlinebankingapp.dtos.responses.Transaction;

import com.example.onlinebankingapp.entities.TransactionEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;

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

    // Static method to convert TransactionEntity to TransactionResponse
    public static TransactionResponse fromTransaction(TransactionEntity transaction){
        return TransactionResponse.builder()
                .id(transaction.getId())
                .transactionType(String.valueOf(transaction.getTransactionType()))
                .amountType(String.valueOf(transaction.getAmountType()))
                .amount(transaction.getAmount())
                .transactionDateTime(transaction.getTransactionDateTime())
                .transactionStatus(String.valueOf(transaction.getTransactionStatus()))
                .transactionRemark(transaction.getTransactionRemark())
                .senderId(transaction.getSenderId())
                .transactionSenderType(String.valueOf(transaction.getTransactionSenderType()))
                .receiverId(transaction.getReceiverId())
                .transactionReceiverType(String.valueOf(transaction.getTransactionReceiverType()))
                .build();
    }
}
