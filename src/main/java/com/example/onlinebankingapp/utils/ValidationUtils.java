package com.example.onlinebankingapp.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.regex.Pattern;

public class ValidationUtils {
    // Method to validate an email address
    public static boolean isValidEmail(String email) {
        // Regular expression pattern for validating email addresses
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        // Create a Pattern object
        Pattern pattern = Pattern.compile(emailRegex);
        // Match the input email with the pattern
        return email != null && pattern.matcher(email).matches();
    }

    // Method to validate a password
    public static boolean isValidPassword(String password) {
        // Check if the password is between 8 and 20 characters long
        if (password == null || password.length() < 8 || password.length() > 20) {
            return false;
        }

        // Regular expression to check for at least one digit, one letter, and one special character
        String digitRegex = ".*\\d.*";
        String letterRegex = ".*[a-zA-Z].*";
        String specialCharRegex = ".*[!@#$%^&*()_+\\-\\[\\]{};':\"\\\\|,.<>/?].*";

        // Check if the password matches the regular expressions
        boolean hasDigit = password.matches(digitRegex);
        boolean hasLetter = password.matches(letterRegex);
        boolean hasSpecialChar = password.matches(specialCharRegex);

        // Return true if the password satisfies all conditions, false otherwise
        return hasDigit && hasLetter && hasSpecialChar;
    }

    // Method to validate a Vietnamese phone number
    // For example:
    // 0912345678 (valid)
    // +84912345678 (valid)
    // 08123456789 (invalid)
    public static boolean isValidPhoneNumber(String phoneNumber) {
        // Regular expression for validating Vietnamese phone numbers
        String phoneRegex = "^(\\+84|0)(3|4|5|7|8|9)\\d{8}$"; // Accepts +849xxxxxxxx or 09xxxxxxxx
        Pattern pattern = Pattern.compile(phoneRegex);
        return phoneNumber != null && pattern.matcher(phoneNumber).matches();
    }

    // Method to validate if the date of birth is at least 18 years ago (using java.util.Date)
    public static boolean isValidDateOfBirth(Date dob) {
        if (dob == null) {
            return false;
        }

        // Convert java.util.Date to java.time.LocalDate
        LocalDate dobLocalDate = Instant.ofEpochMilli(dob.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        // Get the current date
        LocalDate today = LocalDate.now();

        // Check if the date of birth is at least 18 years ago
        return today.minusYears(18).isAfter(dobLocalDate) || today.minusYears(18).isEqual(dobLocalDate);
    }

    // Method to validate String equal Enum type class
    public static <E extends Enum<E>> boolean isValidEnum(String value, Class<E> enumClass) {
        if (value == null) {
            return false;
        }
        for (E constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    // Method to validate if a String is null, empty, or contains only whitespace (blank)
    public static boolean isNullOrEmptyOrBlank(String input) {
        return input == null || input.trim().isEmpty();
    }
}
