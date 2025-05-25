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
@Table(name ="interest_rates")
public class InterestRateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="interestRate", nullable = false)
    private Double interestRate;

    @Column(name="term", nullable = false)
    private Integer term;

    @Column(name="min_balance", nullable = false)
    private Double minBalance;
}
