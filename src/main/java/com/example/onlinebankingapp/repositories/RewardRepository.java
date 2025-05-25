package com.example.onlinebankingapp.repositories;

import com.example.onlinebankingapp.entities.RewardEntity;
import com.example.onlinebankingapp.enums.RewardType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RewardRepository extends JpaRepository<RewardEntity, Long> {
    boolean existsByCostPointAndRewardNameAndRewardType(Integer costPoint, String rewardName, RewardType rewardType);
    Page<RewardEntity> findByRewardNameContainingIgnoreCase(String rewardName, Pageable pageable);
}
