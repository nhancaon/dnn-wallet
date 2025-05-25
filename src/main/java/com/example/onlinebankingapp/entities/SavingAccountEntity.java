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
@Table(name ="saving_accounts")
public class SavingAccountEntity extends AbstractAccount{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="saving_current_amount", nullable = false)
    private Double savingCurrentAmount;

    @Column(name="saving_initial_amount", nullable = false)
    private Double savingInitialAmount;

    @ManyToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_account_id")
    private PaymentAccountEntity paymentAccount;

    @ManyToOne(cascade = CascadeType.DETACH, fetch = FetchType.LAZY)
    @JoinColumn(name = "interest_rate_id")
    private InterestRateEntity interestRate;

    // At initial, saving current amount is always equal to saving initial amount
    protected void onCreate() {
        super.onCreate();
        savingCurrentAmount = savingInitialAmount;
    }
}
