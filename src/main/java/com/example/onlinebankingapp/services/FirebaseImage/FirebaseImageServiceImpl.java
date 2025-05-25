package com.example.onlinebankingapp.services.FirebaseImage;

import com.example.onlinebankingapp.dtos.responses.FirebaseImageResponse;
import com.example.onlinebankingapp.entities.CustomerEntity;
import com.example.onlinebankingapp.entities.RewardEntity;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import com.example.onlinebankingapp.repositories.CustomerRepository;
import com.example.onlinebankingapp.repositories.RewardRepository;
import com.example.onlinebankingapp.services.Customer.CustomerService;
import com.example.onlinebankingapp.services.Reward.RewardService;
import com.example.onlinebankingapp.utils.ImageUtils;
import com.google.cloud.storage.*;
import com.google.firebase.cloud.StorageClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FirebaseImageServiceImpl implements FirebaseImageService {
    private final CustomerService customerService;
    private final RewardService rewardService;

    private final CustomerRepository customerRepository;
    private final RewardRepository rewardRepository;

    @Value("${firebase.bucket}")
    private String bucket;

    @Value("${firebase.public-retrieve-url}")
    private String publicRetrieveUrl;

    private static final String CUSTOMER_NOT_FOUND_DEFAULT = "customer_avatar_NOTFOUND.jpg";
    private static final String REWARD_NOT_FOUND_DEFAULT = "reward_image_NOTFOUND.jpg";
    private static final String CUSTOMER_AVATAR_BUCKET = "customerAvatars/";
    private static final String REWARD_IMAGE_BUCKET = "rewardImages/";

    @Override
    public FirebaseImageResponse uploadCustomerAvatar(
            long customerId,
            MultipartFile file,
            boolean updateAvatar
    ) {
        // Find existingCustomer
        CustomerEntity existingCustomer = customerService.getCustomerById(customerId);
        if(existingCustomer == null){
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        String nameFileCustom = customImageName(file, "CUSTOMER_AVATAR", customerId);
        if(nameFileCustom.isEmpty() || nameFileCustom.isBlank()){
            throw new AppException(ErrorCode.IMAGE_CUSTOM_NAME_INVALID);
        }

        // Set image hashed name for existingCustomer
        String customerAvatarHashedName = nameFileCustom + getFileExtension(file);

        // Create filePath to store in folder CUSTOMER_AVATAR_BUCKET
        String newFilePath = CUSTOMER_AVATAR_BUCKET + customerAvatarHashedName;
        String oldFilePath = null;
        if(updateAvatar){
            // Check not default NOTFOUND of system for avoid delete
            if(!existingCustomer.getAvatarHashedName().equals(CUSTOMER_NOT_FOUND_DEFAULT)) {
                oldFilePath = CUSTOMER_AVATAR_BUCKET + existingCustomer.getAvatarHashedName();
            }
        }

        // Upload to firebase
        FirebaseImageResponse uploadedRewardImage = uploadFileToFirebase(file, newFilePath, oldFilePath);
        if(!uploadedRewardImage.getStatus().equals("success")){
            throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }

        existingCustomer.setAvatarHashedName(customerAvatarHashedName);
        customerRepository.save(existingCustomer);

        return uploadedRewardImage;
    }

    // When to use?
    // If admin/ staff create a new reward, fulfill all info and choose image
    // Next, click ADD button. First, call API insertReward
    // Set imageURL to reward_image_NOTFOUND.jpg to have an existingReward
    // Then, the API uploadRewardImage must be called instantly after that to
    // Upload reward image to Firebase and save into DB for existingReward
    @Override
    public FirebaseImageResponse uploadRewardImage(
            long rewardId,
            MultipartFile file,
            boolean updateImage
    ) {
        // Find existingReward
        RewardEntity existingReward = rewardService.getRewardById(rewardId);
        if(existingReward == null){
            throw new AppException(ErrorCode.REWARD_NOT_FOUND);
        }

        String nameFileCustom = customImageName(file, "REWARD_IMAGE", rewardId);
        if(nameFileCustom.isEmpty() || nameFileCustom.isBlank()){
            throw new AppException(ErrorCode.IMAGE_CUSTOM_NAME_INVALID);
        }

        // Set image hashed name for existingReward
        String rewardImageHashedName = nameFileCustom + getFileExtension(file);

        // Create filePath to store in folder REWARD_IMAGE_BUCKET
        String newFilePath = REWARD_IMAGE_BUCKET + rewardImageHashedName;
        String oldFilePath = null;
        if(updateImage){
            // Check not default NOTFOUND of system for avoid delete
            if(!existingReward.getImageHashedName().equals(REWARD_NOT_FOUND_DEFAULT)){
                oldFilePath = REWARD_IMAGE_BUCKET + existingReward.getImageHashedName();
            }
        }

        // Upload to firebase
        FirebaseImageResponse uploadedRewardImage = uploadFileToFirebase(file, newFilePath, oldFilePath);
        if(!uploadedRewardImage.getStatus().equals("success")){
            throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }

        existingReward.setImageHashedName(rewardImageHashedName);
        rewardRepository.save(existingReward);

        return uploadedRewardImage;
    }

    @Override
    public FirebaseImageResponse getCustomerAvatar(
            long customerId
    ) {
        // Find existingCustomer
        CustomerEntity existingCustomer = customerService.getCustomerById(customerId);
        if(existingCustomer == null || existingCustomer.getAvatarHashedName().isEmpty() || existingCustomer.getAvatarHashedName().isBlank()){
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        // Get from firebase
        FirebaseImageResponse customerAvatar = getFileFromFirebase(CUSTOMER_AVATAR_BUCKET, existingCustomer.getAvatarHashedName());
        if(!customerAvatar.getStatus().equals("success")){
            throw new AppException(ErrorCode.IMAGE_RETRIEVE_FAILED);
        }

        return customerAvatar;
    }

    @Override
    public FirebaseImageResponse getRewardImage(
            long rewardId
    ) {
        // Find existingReward
        RewardEntity existingReward = rewardService.getRewardById(rewardId);
        if(existingReward == null || existingReward.getImageHashedName().isEmpty() || existingReward.getImageHashedName().isBlank()){
            throw new AppException(ErrorCode.REWARD_NOT_FOUND);
        }

        // Get from firebase
        FirebaseImageResponse rewardImage = getFileFromFirebase(REWARD_IMAGE_BUCKET, existingReward.getImageHashedName());
        if(!rewardImage.getStatus().equals("success")){
            throw new AppException(ErrorCode.IMAGE_RETRIEVE_FAILED);
        }

        return rewardImage;
    }

    // Upload or update image of customer/ reward
    private FirebaseImageResponse uploadFileToFirebase(
            MultipartFile file,
            String newFilePath,
            String oldFilePath
    ) {
        try {
            // Get the storage bucket using the bucket name from properties
            Bucket storageBucket = StorageClient.getInstance().bucket(bucket);

            // Check if existingCustomer or existingReward already has image
            // Delete to update new image
            if(oldFilePath != null){
                Blob oldBlob = storageBucket.get(oldFilePath);
                // Check if blob exists and delete it
                if (oldBlob != null && oldBlob.exists()) {
                    oldBlob.delete();
                } else {
                    return new FirebaseImageResponse("fail", "Firebase does not have this file path for the image", oldFilePath);
                }
            }

            // Create a blob and upload the file to Firebase Storage
            storageBucket.create(newFilePath, file.getInputStream(), file.getContentType());

            // Get public URL for viewing image
            String publicUrl = getPublicUrl(newFilePath);

            // Return success response
            return new FirebaseImageResponse("success", "Image uploaded successfully", publicUrl);
        } catch (IOException e) {
            // Return fail response
            return new FirebaseImageResponse("fail", "Failed to upload image to Firebase", e.toString());
        }
    }

    // Method to retrieve a file from Firebase Storage
    private FirebaseImageResponse getFileFromFirebase(
            String bucketType,
            String fileNameInDB
    ) {
        try {
            // Construct the filePath using bucketType and fileName
            String filePath = bucketType + fileNameInDB;

            // Check if the blob exists
            Bucket storageBucket = StorageClient.getInstance().bucket(bucket);
            Blob blob = storageBucket.get(filePath); // Use filePath directly
            if (blob == null || !blob.exists()) {
                if(bucketType.equals(CUSTOMER_AVATAR_BUCKET)){
                    throw new AppException(ErrorCode.IMAGE_CUSTOMER_NOT_FOUND);
                }
                else{
                    throw new AppException(ErrorCode.IMAGE_REWARD_NOT_FOUND);
                }
            }

            // Get public URL for viewing image
            String publicUrl = getPublicUrl(filePath);

            // Return success response
            return new FirebaseImageResponse("success", "Image retrieved successfully", publicUrl);
        } catch (Exception e) {
            // Return fail response
            return new FirebaseImageResponse("fail", "Failed to retrieve image from Firebase", e.toString());
        }
    }

    // Method to custom the image file name to be unique
    private String customImageName(
            MultipartFile file,
            String imageFor,
            Long id
    ) {
        // Bypass all check but extension file is return false
        if(!ImageUtils.isValidImage(file)){
            throw new AppException(ErrorCode.IMAGE_EXTENSION_INVALID);
        }

        // Add UUID at beginning of fileName to ensure unique
        String uniqueFileName = UUID.randomUUID().toString() + "_";

        if(imageFor.equals("CUSTOMER_AVATAR")){
            uniqueFileName += "customer_id_" + id;
        }
        else if(imageFor.equals("REWARD_IMAGE")) {
            uniqueFileName += "reward_id_" + id;
        }
        else{
            throw new AppException(ErrorCode.IMAGE_PURPOSE_INVALID);
        }

        return uniqueFileName;
    }

    // Method to get file extension from MultipartFile
    private String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        return originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf('.')) : "";
    }

    // Method to construct the public URL using the specified pattern
    private String getPublicUrl(String exitingFirebaseFilePath){
        return String.format(publicRetrieveUrl + "%s/o/%s?alt=media",
                bucket,
                URLEncoder.encode(exitingFirebaseFilePath, StandardCharsets.UTF_8));
    }
}
