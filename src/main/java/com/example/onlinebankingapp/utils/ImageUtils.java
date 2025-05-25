package com.example.onlinebankingapp.utils;

import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

public class ImageUtils {
    // Array of allowed file types
    private static final String[] ALLOWED_FILE_EXTENSIONS = {"image/jpeg", "image/jpg", "image/png", "image/webp"};

    // Maximum file size in bytes (10 MB)
    private static final Long MAX_FILE_SIZE = (long) (10 * 1024 * 1024);

    // Method to check if the image is null or empty
    public static boolean isImageNull(MultipartFile file) {
        return Objects.requireNonNull(file.getOriginalFilename()).isEmpty() || file.isEmpty();
    }

    // Method to check if the file is an image
    public static boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    // Method to check if the size of the image is valid (10 MB limit)
    public static boolean isValidFileSize(MultipartFile file) {
        return file.getSize() <= MAX_FILE_SIZE;
    }

    // Method to check if the image is valid
    public static boolean isValidImage(MultipartFile file) {
        // Check if the image is null
        if (isImageNull(file)) {
            throw new AppException(ErrorCode.IMAGE_NULL_INVALID);
        }

        // Check if the file is an image
        if (!isImageFile(file)) {
            throw new AppException(ErrorCode.IMAGE_INVALID);
        }

        // Check if the file size is valid
        if (!isValidFileSize(file)) {
            throw new AppException(ErrorCode.IMAGE_SIZE_INVALID);
        }

        // Try to read the image; returns false if it fails
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        // Check if the file type is in the allowed file types
        for (String type : ALLOWED_FILE_EXTENSIONS) {
            if (type.equals(file.getContentType())) {
                return true;
            }
        }
        return false;
    }
}
