package com.example.onlinebankingapp.entities;

import com.example.onlinebankingapp.enums.AccountStatus;
import com.example.onlinebankingapp.enums.AccountType;
import com.example.onlinebankingapp.utils.DateTimeUtils;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractAccount {
    @Column(name = "account_number", length = 10, nullable = false, unique = true)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status")
    private AccountStatus accountStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type")
    private AccountType accountType;

    @Column(name="date_closed")
    private LocalDateTime dateClosed;

    @Column(name="date_opened")
    private LocalDateTime dateOpened;;

    @PrePersist
    protected void onCreate() {
        if(this.accountStatus == null) {
            accountStatus = AccountStatus.ACTIVE;
        }
        if(this.accountType == null ) {
            accountType = AccountType.CLASSIC;
        }
        this.dateOpened = DateTimeUtils.getVietnamCurrentDateTime();
    }
}
