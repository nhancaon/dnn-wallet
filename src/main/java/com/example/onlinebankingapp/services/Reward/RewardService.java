package com.example.onlinebankingapp.services.Reward;

import com.example.onlinebankingapp.dtos.requests.AccountRewardRequest;
import com.example.onlinebankingapp.dtos.requests.RewardRequest;
import com.example.onlinebankingapp.dtos.responses.Reward.RewardListResponse;
import com.example.onlinebankingapp.entities.AccountRewardEntity;
import com.example.onlinebankingapp.entities.RewardEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RewardService {
    RewardEntity insertReward(RewardRequest rewardRequest);
    RewardEntity getRewardById(Long rewardId);
    List<RewardEntity> getAllRewards();
    RewardListResponse getPaginationListReward(Integer page, Integer size, String orderedBy, Boolean isAscending, String keyword);
    RewardEntity updateReward(Long rewardId, RewardRequest rewardRequest);
    void deleteRewardById(long rewardId);
    AccountRewardEntity redeemReward(AccountRewardRequest accountrewardRequest);
    List<AccountRewardEntity> getRedeemedRewardsNotUsedOfCustomer(Long customerId);
    AccountRewardEntity useReward(AccountRewardRequest accountrewardRequest);
}
