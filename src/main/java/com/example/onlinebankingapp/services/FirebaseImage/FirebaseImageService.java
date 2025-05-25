package com.example.onlinebankingapp.services.FirebaseImage;

import com.example.onlinebankingapp.dtos.responses.FirebaseImageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FirebaseImageService {
    FirebaseImageResponse uploadCustomerAvatar(long customerId, MultipartFile file, boolean updateAvatar);
    FirebaseImageResponse uploadRewardImage(long rewardId, MultipartFile file, boolean updateImage);
    FirebaseImageResponse getCustomerAvatar(long customerId);
    FirebaseImageResponse getRewardImage(long rewardId);
}
