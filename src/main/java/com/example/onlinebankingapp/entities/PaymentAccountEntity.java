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
@Table(name ="payment_accounts")
public class PaymentAccountEntity extends AbstractAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "current_balance", nullable = false)
    private Double currentBalance;

    @Column(name = "reward_point", nullable = false)
    private Integer rewardPoint;

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    protected void onCreate() {
        super.onCreate();
        currentBalance = (double) 0;
        rewardPoint = 0;
    }
}
