package com.example.onlinebankingapp.entities;

import com.example.onlinebankingapp.enums.*;
import com.example.onlinebankingapp.utils.DateTimeUtils;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name ="transactions")
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "amount_type", nullable = false)
    private AmountType amountType;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name="transaction_date_time", nullable = false)
    private LocalDateTime transactionDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false)
    private TransactionStatus transactionStatus;

    @Column(name = "transaction_remark", nullable = false)
    private String transactionRemark;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_sender_type", nullable = false)
    private TransactionSenderType transactionSenderType;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_receiver_type", nullable = false)
    private TransactionReceiverType transactionReceiverType;

    @PrePersist
    protected void onCreate() {
        transactionDateTime = DateTimeUtils.getVietnamCurrentDateTime();
        if (transactionStatus == null) {
            transactionStatus = TransactionStatus.PENDING;
        }
    }
}
