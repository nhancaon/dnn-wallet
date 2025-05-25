package com.example.onlinebankingapp.repositories;

import com.example.onlinebankingapp.entities.AccountRewardEntity;
import com.example.onlinebankingapp.entities.PaymentAccountEntity;
import com.example.onlinebankingapp.entities.RewardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRewardRepository extends JpaRepository<AccountRewardEntity, AccountRewardEntity.AccountReward> {
    List<AccountRewardEntity> findAccountRewardEntityByAccountReward_Reward(RewardEntity reward);
    AccountRewardEntity findAccountRewardEntityByAccountReward(AccountRewardEntity.AccountReward accountReward);
    List<AccountRewardEntity> findAccountRewardEntitiesByAccountRewardPaymentAccount(PaymentAccountEntity paymentAccount);
    boolean existsAccountRewardEntityByAccountReward(AccountRewardEntity.AccountReward accountReward);
}
