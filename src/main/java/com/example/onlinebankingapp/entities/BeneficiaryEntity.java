package com.example.onlinebankingapp.entities;

import com.example.onlinebankingapp.enums.BeneficiaryReceiverType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name ="beneficiaries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "customer_id"}))
public class BeneficiaryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="name", length = 100, nullable = false)
    private String name;

    @ManyToOne(cascade = CascadeType.DETACH)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerEntity customer;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "beneficiary_receiver_type", nullable = false)
    private BeneficiaryReceiverType beneficiaryReceiverType;
}
