package com.example.onlinebankingapp.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name ="transactions_of_customers")
public class TransactionCustomerEntity {
    @EmbeddedId
    private TransactionCustomer transactionCustomerKey;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class TransactionCustomer implements Serializable {
        @OneToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
        @JoinColumn(name = "transaction_id", nullable = false)
        private TransactionEntity transaction;

        @ManyToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
        @JoinColumn(name = "customer_id", nullable = false)
        private CustomerEntity customer;

        @Column(name = "receiver_id", nullable = true)
        private Long receiverId;
    }
}
