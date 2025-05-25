package com.example.onlinebankingapp.controllers;

import com.example.onlinebankingapp.dtos.responses.FirebaseImageResponse;
import com.example.onlinebankingapp.dtos.responses.ResponseObject;
import com.example.onlinebankingapp.services.FirebaseImage.FirebaseImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/firebase")
@RequiredArgsConstructor
public class FirebaseImageController {
    private final FirebaseImageService firebaseImageService;

    // How to use: this is for both upload at initial or update
    // If user want to insert avatar for 1st time, let variable updateAvatar = false
    // If user want to update avatar, let variable updateAvatar = true
    @PostMapping("/uploadCustomerAvatar/{customerId}")
    public ResponseEntity<?> uploadCustomerAvatar(
            @Valid @PathVariable("customerId") long customerId,
            @ModelAttribute("file") MultipartFile file,
            @RequestParam boolean updateAvatar
    ) {
        // Upload customer avatar
        FirebaseImageResponse firebaseImageResponse = firebaseImageService
                .uploadCustomerAvatar(customerId, file,updateAvatar);

        // Return data in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Upload customer avatar to Firebase successfully")
                .result(firebaseImageResponse)
                .build());
    }

    // How to use: this is for both upload at initial or update
    // If we want to insert reward image for 1st time, let variable updateImage = false
    // If we want to update reward image, let variable updateImage = true
    @PostMapping("/uploadRewardImage/{rewardId}")
    public ResponseEntity<?> uploadRewardImage(
            @Valid @PathVariable("rewardId") long rewardId,
            @ModelAttribute("file") MultipartFile file,
            @RequestParam boolean updateImage
    ) {
        // Upload customer avatar
        FirebaseImageResponse firebaseImageResponse = firebaseImageService
                .uploadRewardImage(rewardId, file, updateImage);

        // Return data in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Upload reward image to Firebase successfully")
                .result(firebaseImageResponse)
                .build());
    }

    @GetMapping("/getCustomerAvatar/{customerId}")
    public ResponseEntity<?> getCustomerAvatar(
            @PathVariable("customerId") long customerId
    ) {
        // Get customer avatar
        FirebaseImageResponse firebaseImageResponse = firebaseImageService.getCustomerAvatar(customerId);

        // Return data in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get customer avatar from Firebase successfully")
                .result(firebaseImageResponse)
                .build());
    }

    @GetMapping("/getRewardImage/{rewardId}")
    public ResponseEntity<?> getRewardImage(
            @PathVariable("rewardId") long rewardId
    ) {
        // get reward image
        FirebaseImageResponse firebaseImageResponse = firebaseImageService.getRewardImage(rewardId);

        // Return data in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get reward image from Firebase successfully")
                .result(firebaseImageResponse)
                .build());
    }
}
