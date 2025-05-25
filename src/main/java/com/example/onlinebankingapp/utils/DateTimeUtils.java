package com.example.onlinebankingapp.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class DateTimeUtils {
    // Method to return date time based on Vietnam time zone
    // Display: 2024-10-25T23:05:37
    public static LocalDateTime getVietnamCurrentDateTime() {
        return ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.SECONDS);
    }

    // Method to return the number of days of normal or leap year
    public static int getDaysInYear(int year) {
        // Check if the year is a leap year
        if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
            return 366; // Leap year
        } else {
            return 365; // Regular year
        }
    }
}
