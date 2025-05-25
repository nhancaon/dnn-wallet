package com.example.onlinebankingapp.controllers;

import com.example.onlinebankingapp.dtos.requests.AccountRewardRequest;
import com.example.onlinebankingapp.dtos.requests.RewardRequest;
import com.example.onlinebankingapp.entities.AccountRewardEntity;
import com.example.onlinebankingapp.entities.RewardEntity;
import com.example.onlinebankingapp.dtos.responses.AccountReward.AccountRewardListResponse;
import com.example.onlinebankingapp.dtos.responses.AccountReward.AccountRewardResponse;
import com.example.onlinebankingapp.dtos.responses.ResponseObject;
import com.example.onlinebankingapp.dtos.responses.Reward.RewardListResponse;
import com.example.onlinebankingapp.dtos.responses.Reward.RewardResponse;
import com.example.onlinebankingapp.services.Reward.RewardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
public class RewardController {
    private final RewardService rewardService;

    // Manage reward (used by admin/ staff)
    // Endpoint for inserting a reward
    @PostMapping("/insertReward")
    public ResponseEntity<?> insertReward(
            @Valid @RequestBody RewardRequest rewardRequest
    ) {
        RewardEntity newRewardResponse = rewardService.insertReward(rewardRequest);

        // Return response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Insert reward successfully")
                .result(RewardResponse.fromReward(newRewardResponse))
                .build());
    }

    // Endpoint for getting a reward by its id
    @GetMapping("/getRewardById/{rewardId}")
    public ResponseEntity<?> getRewardById(
            @Valid @PathVariable("rewardId") Long rewardId
    ) {
        // Retrieve a reward entity by its ID
        RewardEntity rewardResponse = rewardService.getRewardById(rewardId);

        // Return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get reward with ID: " + rewardId + " successfully")
                .result(RewardResponse.fromReward(rewardResponse))
                .build());
    }

    // Endpoint for getting all rewards
    @GetMapping("/getAllRewards")
    public ResponseEntity<?> getAllRewards() {
        // Retrieve a list of reward entities
        List<RewardEntity> rewardEntityList = rewardService.getAllRewards();

        // Build response
        List<RewardResponse> rewardResponseList = rewardEntityList.stream()
                .map(RewardResponse::fromReward)
                .toList();

        RewardListResponse rewardListResponse = RewardListResponse.builder()
                .rewardResponses(rewardResponseList)
                .build();

        // Return the response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get list of all rewards successfully")
                .result(rewardListResponse)
                .build());
    }

    // Endpoint for getting all rewards with pagination
    @GetMapping("/getPaginationListReward")
    public ResponseEntity<?> getPaginationListReward(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String orderedBy,
            @RequestParam(defaultValue = "false") String isAscending,
            @RequestParam(defaultValue = "0") String keyword
    ) {
        Boolean isAsc = Boolean.parseBoolean(isAscending);

        RewardListResponse rewardPaginated = rewardService.
                getPaginationListReward(page, size, orderedBy, isAsc, keyword);

        // Return the response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get all paginated rewards successfully")
                .result(rewardPaginated)
                .build());
    }

    // Endpoint for uploading a reward information
    @PutMapping("/updateReward/{rewardId}")
    public ResponseEntity<?> updateReward(
            @Valid @PathVariable("rewardId") Long rewardId,
            @Valid @RequestBody RewardRequest rewardRequest
    ) {
        // Update the reward details
        RewardEntity rewardResponse = rewardService.updateReward(rewardId, rewardRequest);

        // Return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Update reward with ID: " + rewardId + "successfully")
                .result(RewardResponse.fromReward(rewardResponse))
                .build());
    }

    // Endpoint for deleting reward
    @DeleteMapping("/deleteReward/{rewardId}")
    public ResponseEntity<?> deleteReward(
            @Valid @PathVariable("rewardId") Long rewardId
    ) {
        // Delete the reward
        rewardService.deleteRewardById(rewardId);

        // Return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Delete reward with ID: " + rewardId + "successfully")
                .build());
    }
    ///////////////////////////////////////////////////////////////////////////////////

    // Manage reward redeemed by PAs
    // Endpoint for redeeming a reward
    @PostMapping("/redeemReward")
    public ResponseEntity<?> redeemReward(
            @Valid @RequestBody AccountRewardRequest accountRewardRequest
    ) {
        // Redeem the requested reward
        AccountRewardEntity newAccountRewardEntity = rewardService.redeemReward(accountRewardRequest);

        // Return response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Redeem reward successfully")
                .result(AccountRewardResponse.fromAccountReward(newAccountRewardEntity))
                .build());
    }

    // Endpoint for getting all redeemed rewards of a customer
    @GetMapping("/getRedeemedRewardsNotUsedOfCustomer/{customerId}")
    public ResponseEntity<?> getRedeemedRewardsNotUsedOfCustomer(
            @Valid @PathVariable("customerId") Long customerId
    ) {
        // Retrieve a list of account rewards for the specified user
        List<AccountRewardEntity> accountRewardList = rewardService.getRedeemedRewardsNotUsedOfCustomer(customerId);
        if(accountRewardList.isEmpty()){
            // Return response
            return ResponseEntity.ok(ResponseObject.builder()
                    .status(HttpStatus.OK)
                    .message("Please view page reward and redeem any rewards you want")
                    .result("Customer does not have any valid redeemed rewards")
                    .build());
        }


        // Build response
        List<AccountRewardResponse> accountRewardResponseList = accountRewardList.stream()
                .map(AccountRewardResponse::fromAccountReward)
                .toList();

        AccountRewardListResponse accountRewardListResponse = AccountRewardListResponse.builder()
                .accountRewards(accountRewardResponseList)
                .build();

        // Return response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get all redeemed rewards of customer with ID: " + customerId + " successfully")
                .result(accountRewardListResponse)
                .build());
    }

    // Endpoint for using a reward
    @PutMapping("/useReward")
    public ResponseEntity<?> useReward(
            @Valid @RequestBody AccountRewardRequest accountRewardDTO
    ) {
        // Use the requested reward
        AccountRewardEntity newAccountRewardEntity = rewardService.useReward(accountRewardDTO);

        // Return the result in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Use reward successfully")
                .result(AccountRewardResponse.fromAccountReward(newAccountRewardEntity))
                .build());
    }
}
