package com.example.onlinebankingapp.schedulers;

import com.example.onlinebankingapp.entities.SavingAccountEntity;
import com.example.onlinebankingapp.enums.AccountStatus;
import com.example.onlinebankingapp.services.SavingAccount.SavingAccountService;
import com.example.onlinebankingapp.utils.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
public class SavingAccountScheduler {
    private final SavingAccountService savingAccountService;

    // Update every minute (for testing)
//    private static final String cronEveryMinute = "0 * * * * *";

    // Update at 0:00:00 everyday
    private static final String cronDaily = "0 0 0 * * *";
    private static final String zoneVietNam = "Asia/Saigon";

    @Autowired
    public SavingAccountScheduler(SavingAccountService savingAccountService) {
        this.savingAccountService = savingAccountService;
    }

    // Scheduled task to perform daily update for saving accounts
    @Scheduled(cron = cronDaily, zone = zoneVietNam)
    @Transactional(rollbackFor = {Exception.class, Throwable.class})
    public void performSavingAccountsDailyUpdate() {
        log.info("Start daily saving account updates: {}", DateTimeUtils.getVietnamCurrentDateTime());

        // Retrieve all saving accounts
        List<SavingAccountEntity> savingAccountEntityList = savingAccountService.getAllSavingAccounts();

        // Iterate through each saving account
        for (SavingAccountEntity savingAccount : savingAccountEntityList) {
            // Check if the account is active
            if (savingAccount.getAccountStatus().equals(AccountStatus.ACTIVE)) {
                // If the account term has ended
                // Deactivate it and transfer the balance to the associated payment account
                if(savingAccountService.isEndOfTerm(savingAccount)){
                    savingAccountService.deactivateAndWithdrawCurrentAmountToPA(savingAccount);
                } else {
                    // Otherwise, update the daily current balance
                    savingAccountService.updateDailyCurrentBalance(savingAccount);
                }
            }
        }

        log.info("End daily saving account updates: {}", DateTimeUtils.getVietnamCurrentDateTime());
    }
}
