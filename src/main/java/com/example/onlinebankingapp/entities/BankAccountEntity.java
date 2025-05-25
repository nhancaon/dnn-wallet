package com.example.onlinebankingapp.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name ="bank_accounts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"bank_account_number", "bank_id"}))
public class BankAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_account_number", nullable = false)
    private String bankAccountNumber;

    @Column(name = "citizen_id", length = 12, nullable = false)
    private String citizenId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name="phone_number", length = 10, nullable = false)
    private String phoneNumber;

    @Column(name = "current_balance", nullable = false)
    private Double currentBalance;

    @ManyToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id")
    private BankEntity bank;

    @ManyToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_account_id")
    private PaymentAccountEntity paymentAccount;
}
