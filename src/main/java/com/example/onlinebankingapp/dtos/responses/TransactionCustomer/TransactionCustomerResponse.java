package com.example.onlinebankingapp.dtos.responses.TransactionCustomer;

import com.example.onlinebankingapp.entities.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionCustomerResponse {
    @JsonProperty("transaction_id")
    private Long transactionId;

    @JsonProperty("customer_id")
    private Long customerId;

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

    // Static method to create an TransactionCustomerResponse object from an TransactionCustomerEntity object
    public static TransactionCustomerResponse fromTransactionCustomer(TransactionCustomerEntity transactionCustomer){
        TransactionEntity transaction = transactionCustomer.getTransactionCustomerKey().getTransaction();
        CustomerEntity customer = transactionCustomer.getTransactionCustomerKey().getCustomer();

        return TransactionCustomerResponse.builder()
                .transactionId(transaction.getId())
                .customerId(customer.getId())
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
