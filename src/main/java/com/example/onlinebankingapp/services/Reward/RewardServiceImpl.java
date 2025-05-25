package com.example.onlinebankingapp.services.Reward;

import com.example.onlinebankingapp.dtos.requests.AccountRewardRequest;
import com.example.onlinebankingapp.dtos.requests.RewardRequest;
import com.example.onlinebankingapp.dtos.responses.Reward.RewardListResponse;
import com.example.onlinebankingapp.dtos.responses.Reward.RewardResponse;
import com.example.onlinebankingapp.entities.AccountRewardEntity;
import com.example.onlinebankingapp.entities.PaymentAccountEntity;
import com.example.onlinebankingapp.entities.RewardEntity;
import com.example.onlinebankingapp.enums.RewardType;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import com.example.onlinebankingapp.repositories.AccountRewardRepository;
import com.example.onlinebankingapp.repositories.PaymentAccountRepository;
import com.example.onlinebankingapp.repositories.RewardRepository;
import com.example.onlinebankingapp.services.PaymentAccount.PaymentAccountService;
import com.example.onlinebankingapp.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RewardServiceImpl implements RewardService {
    private final PaymentAccountService paymentAccountService;

    private final RewardRepository rewardRepository;
    private final AccountRewardRepository accountRewardRepository;
    private final PaymentAccountRepository paymentAccountRepository;

    // Manage reward (used by admin/ staff)
    // Method to insert a new reward
    @Override
    public RewardEntity insertReward(
            RewardRequest rewardRequest
    ) {
        // Validate the reward data
        isRewardRequestValid(rewardRequest);

        // Create a new RewardEntity object from the rewardRequest
        RewardEntity newRewardEntity = RewardEntity.builder()
                .costPoint(rewardRequest.getCostPoint())
                .rewardName(rewardRequest.getRewardName())
                .rewardType(RewardType.valueOf(rewardRequest.getRewardType()))
                .build();

        // Save and return the new reward entity
        return rewardRepository.save(newRewardEntity);
    }

    // Method to get a reward by its ID
    @Override
    public RewardEntity getRewardById(
            Long rewardId
    ) {
        Optional<RewardEntity> optionalReward = rewardRepository.findById(rewardId);
        if(optionalReward.isPresent()) {
            return optionalReward.get();
        }
        throw new AppException(ErrorCode.REWARD_NOT_FOUND);
    }

    // Method to get all rewards
    @Override
    public List<RewardEntity> getAllRewards() {
        return rewardRepository.findAll();
    }

    @Override
    public RewardListResponse getPaginationListReward(
            Integer page, Integer size, String orderedBy, Boolean isAscending, String keyword
    ) {
        Long totalQuantity;
        Page<RewardEntity> rewardPage;

        // Get ascending or descending sort
        Sort sort = Boolean.TRUE.equals(isAscending)
                ? Sort.by(orderedBy).ascending()
                : Sort.by(orderedBy).descending();

        try {
            rewardPage = rewardRepository.findByRewardNameContainingIgnoreCase(
                    keyword, PageRequest.of(page - 1, size, sort));
            totalQuantity = rewardPage.getTotalElements();
        }
        catch (Exception e){
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        List<RewardResponse> rewardResponses = rewardPage.stream()
                .map(RewardResponse::fromReward)
                .toList();

        return RewardListResponse.builder()
                .rewardResponses(rewardResponses)
                .totalQuantity(totalQuantity)
                .build();
    }

    // Method to update an existing reward
    @Override
    public RewardEntity updateReward(
            Long rewardId,
            RewardRequest rewardRequest
    ) {
        // Validate the reward data
        isRewardRequestValid(rewardRequest);

        // Find the reward entity by ID and update its fields
        Optional<RewardEntity> optionalReward = rewardRepository.findById(rewardId);
        if(optionalReward.isEmpty()) {
            throw new AppException(ErrorCode.REWARD_NOT_FOUND);
        }

        RewardEntity updateReward = optionalReward.get();
        updateReward.setCostPoint(rewardRequest.getCostPoint());
        updateReward.setRewardName(rewardRequest.getRewardName());
        updateReward.setRewardType(RewardType.valueOf(rewardRequest.getRewardType()));

        // Save and return the updated reward entity
        return rewardRepository.save(updateReward);
    }

    @Override
    public void deleteRewardById(
            long rewardId
    ) {
        // Find existingReward
        RewardEntity deleteReward = getRewardById(rewardId);

        // Check relation of PA and reward in table rewards_of_accounts
        // Find all related AccountReward entities to delete
        List<AccountRewardEntity> listDeleteAccountReward = accountRewardRepository
                .findAccountRewardEntityByAccountReward_Reward(deleteReward);

        // If there are related AccountReward entities, delete them
        if (!listDeleteAccountReward.isEmpty()) {
            accountRewardRepository.deleteAll(listDeleteAccountReward);
        }

        // Delete reward
        rewardRepository.delete(deleteReward);
    }
    ///////////////////////////////////////////////////////////////////////////////////

    // Manage reward redeemed by PAs
    // Method to redeem a reward
    @Transactional(rollbackFor = {Exception.class, Throwable.class})
    @Override
    public AccountRewardEntity redeemReward(
            AccountRewardRequest accountrewardRequest
    ) {
        PaymentAccountEntity queryPaymentAccount = paymentAccountService.getPaymentAccountById(accountrewardRequest.getPaymentAccountId());
        RewardEntity queryReward = getRewardById(accountrewardRequest.getRewardId());

        // Check if the user has enough points to redeem the reward
        if(queryPaymentAccount.getRewardPoint() < queryReward.getCostPoint()){
            throw new AppException(ErrorCode.INSUFFICIENT_POINT);
        }

        // Create a relationship key for the account-reward relationship
        AccountRewardEntity.AccountReward accountReward = AccountRewardEntity.AccountReward.builder()
                .reward(queryReward)
                .paymentAccount(queryPaymentAccount)
                .build();

        // Check if this voucher has been redeemed by this account
        if(accountRewardRepository.existsAccountRewardEntityByAccountReward(accountReward)){
            throw new AppException(ErrorCode.REWARD_ACCOUNT_EXISTS);
        }

        // Create a new AccountRewardEntity
        AccountRewardEntity newAccountReward = AccountRewardEntity.builder()
                .accountReward(accountReward)
                .build();

        // Subtract point from payment account
        queryPaymentAccount.setRewardPoint(queryPaymentAccount.getRewardPoint()- queryReward.getCostPoint());
        paymentAccountRepository.save(queryPaymentAccount);

        return accountRewardRepository.save(newAccountReward);
    }

    // Method to get all redeemed rewards of a customer
    @Override
    public List<AccountRewardEntity> getRedeemedRewardsNotUsedOfCustomer(
            Long customerId
    ) {
        // Get all payment accounts of the customer
        List<PaymentAccountEntity> customerPaymentAccountsList = paymentAccountService
                .getPaymentAccountsByCustomerId(customerId);

        // Find all the account rewards in has Payment Account of the customer
        List<AccountRewardEntity> userAccountRewardsList = new ArrayList<>();
        for(PaymentAccountEntity paymentAccount : customerPaymentAccountsList){
            userAccountRewardsList.addAll(accountRewardRepository.findAccountRewardEntitiesByAccountRewardPaymentAccount(paymentAccount));
        }

        // Filter out invalid rewards (not used rewards)
        userAccountRewardsList.removeIf(accountReward -> !accountReward.isValid());

        // Return the list or null if it's empty
        return userAccountRewardsList.isEmpty() ? Collections.emptyList() : userAccountRewardsList;
    }

    // Method to use a reward
    @Override
    public AccountRewardEntity useReward(
            AccountRewardRequest accountrewardRequest
    ) {
        // Find the payment account and reward
        PaymentAccountEntity queryPaymentAccount = paymentAccountService.getPaymentAccountById(accountrewardRequest.getPaymentAccountId());
        RewardEntity queryReward = getRewardById(accountrewardRequest.getRewardId());

        AccountRewardEntity.AccountReward accountRewardKey = AccountRewardEntity.AccountReward.builder()
                .reward(queryReward)
                .paymentAccount(queryPaymentAccount)
                .build();

        // Find the account with this reward
        AccountRewardEntity accountReward = accountRewardRepository
                .findAccountRewardEntityByAccountReward(accountRewardKey);

        // Check if the account has this reward
        if(accountReward == null){
            throw new AppException(ErrorCode.REDEEMED_REWARD_NOT_FOUND);
        }

        // Check if the reward is still valid
        if(!accountReward.isValid()){
            throw new AppException(ErrorCode.USED_REWARD);
        }

        accountReward.setValid(false);

        return accountRewardRepository.save(accountReward);
    }

    // Method to validate a rewardRequest
    private void isRewardRequestValid(RewardRequest rewardRequest) {
        // Check if name is empty or blank
        if (StringUtils.isEmpty(rewardRequest.getRewardName()) || StringUtils.isBlank(rewardRequest.getRewardName())) {
            throw new AppException(ErrorCode.REWARD_NAME_MISSING);
        }

        // Check if reward type is empty or blank
        if (StringUtils.isEmpty(rewardRequest.getRewardType()) || StringUtils.isBlank(rewardRequest.getRewardType())) {
            throw new AppException(ErrorCode.REWARD_TYPE_MISSING);
        }

        // Check if type is a valid enum data
        if(!ValidationUtils.isValidEnum(rewardRequest.getRewardType().toUpperCase(), RewardType.class)){
            throw new AppException(ErrorCode.REWARD_TYPE_INVALID);
        }

        // Check if reward cost is between 0 & 9999
        if(rewardRequest.getCostPoint() <= 0 || rewardRequest.getCostPoint() > 9999){
            throw new AppException(ErrorCode.REWARD_COST_INVALID);
        }

        // Check if reward existed
        if(rewardRepository.existsByCostPointAndRewardNameAndRewardType(
                rewardRequest.getCostPoint(),
                rewardRequest.getRewardName(),
                RewardType.valueOf(rewardRequest.getRewardType()))
        ){
            throw new AppException(ErrorCode.REWARD_EXISTS);
        }
    }
}
