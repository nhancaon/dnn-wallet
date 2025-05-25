package com.example.onlinebankingapp.exceptions;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 4xx Client Errors
    BAD_REQUEST(400, "Bad Request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "Forbidden", HttpStatus.FORBIDDEN),
    NOT_FOUND(404, "Not Found", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed", HttpStatus.METHOD_NOT_ALLOWED),
    CONFLICT(409, "Conflict", HttpStatus.CONFLICT),
    TOO_MANY_REQUESTS(429, "Too Many Requests", HttpStatus.TOO_MANY_REQUESTS),


    //Custom Invalid
    PASSWORD_INVALID(400, """
            Password must satisfy the following conditions:
            The length must be from 8 to 20 characters
            Contains at least 01 digit, 01 letter and 01 special character""", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(400, "The format of email is not correct!", HttpStatus.BAD_REQUEST),
    PIN_NUMBER_INVALID(400, "Pin number must be exactly 6 digits", HttpStatus.BAD_REQUEST),
    ADDRESS_INVALID(400, "Invalid Address", HttpStatus.BAD_REQUEST),
    TERM_INVALID(400, "Term must be between 1 and 99 months", HttpStatus.BAD_REQUEST),
    RATE_INVALID(400, "Interest rate must be between 0% and 999%", HttpStatus.BAD_REQUEST),
    MIN_BALANCE_INVALID(400, "Min balance must be between 100.000 and 999.999.999", HttpStatus.BAD_REQUEST),
    WITHDRAW_BALANCE_INVALID(400, "Insufficient balance for withdrawing", HttpStatus.BAD_REQUEST),
    BALANCE_AMOUNT_INVALID(400, "Payment account does not have enough balance", HttpStatus.BAD_REQUEST),
    OWNER_NAME_INVALID(400, "Owner name of this bank account number invalid", HttpStatus.BAD_REQUEST),
    CITIZEN_ID_INVALID(400, "Citizen Identification number invalid", HttpStatus.BAD_REQUEST),
    PAYMENT_ACCOUNT_INVALID(404, "Payment Account Invalid", HttpStatus.NOT_FOUND),
    TRANSACTION_INVALID(400, "Cannot make transaction to your current account", HttpStatus.BAD_REQUEST),
    INACTIVE_WITHDRAW_INVALID(400, "Cannot withdraw from an inactive account", HttpStatus.BAD_REQUEST),
    MISSING_PARAMETER(400, "Missing Parameter", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_POINT(400, "Insufficient points, you do not have enough reward points", HttpStatus.BAD_REQUEST),
    REWARD_INVALID(400, "This account has not redeem this reward yet", HttpStatus.BAD_REQUEST),
    REWARD_TYPE_INVALID(400, "Invalid reward type", HttpStatus.BAD_REQUEST),
    REWARD_COST_INVALID(400, "Reward cost must be between 1 and 9999", HttpStatus.BAD_REQUEST),
    LINK_ACCOUNT_INVALID(400, "Cannot link to new account", HttpStatus.BAD_REQUEST),
    PHONE_NUMBER_INVALID(400, "Phone number is not in the correct format", HttpStatus.BAD_REQUEST),
    USER_ROLE_INVALID(400, "Invalid user role", HttpStatus.BAD_REQUEST),
    IMAGE_NULL_INVALID(400, "The input image must be not null", HttpStatus.BAD_REQUEST),
    IMAGE_INVALID(400, "This is not an image file", HttpStatus.BAD_REQUEST),
    IMAGE_EXTENSION_INVALID(400, "The extension of image file must be in: jpeg, jpg, png, webp", HttpStatus.BAD_REQUEST),
    IMAGE_SIZE_INVALID(400, "The size of image file must be smaller or equal 10MB", HttpStatus.BAD_REQUEST),
    IMAGE_PURPOSE_INVALID(400, "The purpose of image file must be CUSTOMER_AVATAR or REWARD_IMAGE", HttpStatus.BAD_REQUEST),
    IMAGE_CUSTOM_NAME_INVALID(400, "Cannot create custom name for image file", HttpStatus.BAD_REQUEST),
    IMAGE_UPLOAD_FAILED(400, "Failed to upload image file to Firebase", HttpStatus.BAD_REQUEST),
    IMAGE_RETRIEVE_FAILED(400, "Failed to retrieve image file from Firebase", HttpStatus.BAD_REQUEST),

    //Custom Not Found
    USER_NOT_FOUND(404, "User does not exists", HttpStatus.NOT_FOUND),
    BANK_ACCOUNT_OF_PA_NOT_FOUND(404, "accountsOfPANotFound", HttpStatus.NOT_FOUND),
    BANK_ACCOUNT_NOT_FOUND(404, "Bank Account does not exists", HttpStatus.NOT_FOUND),
    BANK_NOT_FOUND(404, "Bank does not exists", HttpStatus.NOT_FOUND),
    BENEFICIARY_NOT_FOUND(404, "Beneficiary does not exists", HttpStatus.NOT_FOUND),
    BENEFICIARY_PA_LIST_NOT_FOUND(404, "List of PA beneficiaries does not exists", HttpStatus.NOT_FOUND),
    BENEFICIARY_BA_LIST_NOT_FOUND(404, "List of BA beneficiaries does not exists", HttpStatus.NOT_FOUND),
    BENEFICIARY_LIST_ALL_NOT_FOUND(404, "List of all beneficiaries does not exists", HttpStatus.NOT_FOUND),
    REFRESH_TOKEN_NOT_FOUND(404, "Refresh token does not exist", HttpStatus.NOT_FOUND),
    INTEREST_RATE_NOT_FOUND(404, "Interest rate does not exist", HttpStatus.NOT_FOUND),
    TRANSACTION_NOT_FOUND(404, "Transaction does not exist", HttpStatus.NOT_FOUND),
    SENDER_OR_RECEIVER_NOT_FOUND(404, "Sender or receiver does not exist", HttpStatus.NOT_FOUND),
    SAVING_ACCOUNT_NOT_FOUND(404, "Saving account does not exist", HttpStatus.NOT_FOUND),
    FACE_NOT_FOUND(404, "Customer face not found but has phone number", HttpStatus.NOT_FOUND),
    REWARD_NOT_FOUND(404, "Reward does not exist", HttpStatus.NOT_FOUND),
    BANK_NAME_NOT_FOUND(404, "Bank name does not exists", HttpStatus.NOT_FOUND),
    IMAGE_CUSTOMER_NOT_FOUND(404, "Avatar of customer does not exists in Firebase", HttpStatus.NOT_FOUND),
    IMAGE_REWARD_NOT_FOUND(404, "Image of reward does not exists in Firebase", HttpStatus.NOT_FOUND),

    //Custom Unauthorized
    PASSWORD_INCORRECT(401, "Username or password is incorrect", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(401, "Token is expired", HttpStatus.UNAUTHORIZED),

    //Custom Exists
    BENEFICIARY_EXISTS(403, "Beneficiary already exists", HttpStatus.FORBIDDEN),
    PAYMENT_ACCOUNT_EXISTS(403, "Payment Account already exists", HttpStatus.FORBIDDEN),
    EMAIL_EXISTS(403, "Email already exists", HttpStatus.FORBIDDEN),
    CITIZEN_ID_EXISTS(403, "Citizen Identification already exists", HttpStatus.FORBIDDEN),
    PHONE_NUMBER_EXISTS(403, "Phone number already exists", HttpStatus.FORBIDDEN),
    USER_EXISTS(403, "User already exists", HttpStatus.FORBIDDEN),
    REWARD_EXISTS(403, "This reward has been existed", HttpStatus.FORBIDDEN),
    REWARD_ACCOUNT_EXISTS(403, "This account has already redeemed this reward", HttpStatus.FORBIDDEN),
    REDEEMED_REWARD_NOT_FOUND(403, "This account has not redeem this reward yet", HttpStatus.FORBIDDEN),
    USED_REWARD(403, "This reward has already been used", HttpStatus.FORBIDDEN),
    SAVING_ACCOUNT_EXISTS(403, "Saving account already exists", HttpStatus.FORBIDDEN),

    // Payment account
    PAYMENT_ACCOUNT_DEFAULT(403, "This is DEFAULT payment account", HttpStatus.FORBIDDEN),
    PAYMENT_ACCOUNT_NOT_FOUND(404, "Payment Account does not exists", HttpStatus.NOT_FOUND),
    PAYMENT_ACCOUNT_LINKED_BANK_ACCOUNT(403, "Payment Account has already linked with bank account(s)", HttpStatus.FORBIDDEN),
    TOP_UP_MONEY_EXCEED(400, "Top-up money has exceeded the current balance in BANK ACCOUNT", HttpStatus.BAD_REQUEST),
    WITHDRAW_MONEY_EXCEED(400, "Withdraw money has exceeded the current balance in PAYMENT ACCOUNT", HttpStatus.BAD_REQUEST),
    WRONG_PAYMENT_ACCOUNT(400, "Customer does not have this payment account", HttpStatus.BAD_REQUEST),
    PAYMENT_ACCOUNT_TYPE_INVALID(400, "Payment account type is invalid", HttpStatus.BAD_REQUEST),

    // Bank account
    BANK_ACCOUNT_LINKED(400, "Bank account number has already linked", HttpStatus.BAD_REQUEST),
    BANK_ACCOUNT_NOT_LINKED(400, "Bank account number has not linked yet", HttpStatus.BAD_REQUEST),
    MINIMUM_ADD_MONEY(400, "The minimum top-up amount is 10,000 VND", HttpStatus.BAD_REQUEST),
    MINIMUM_WITHDRAW_MONEY(400, "The minimum withdraw amount is 50,000 VND", HttpStatus.BAD_REQUEST),

    MAXIMUM_ADD_MONEY(400, "The maximum top-up amount is 50,000,000 VND", HttpStatus.BAD_REQUEST),
    MAXIMUM_WITHDRAW_MONEY(400, "The maximum withdraw amount is 50,000,000", HttpStatus.BAD_REQUEST),

    // Transaction
    USER_INACTIVE(401, "User's account has been set to INACTIVE", HttpStatus.UNAUTHORIZED),
    NOT_LINKED_ANY_BANK_ACCOUNT(400, "Customer do not link any bank account", HttpStatus.BAD_REQUEST),
    MINIMUM_IN_TRANSFER_MONEY(400, "The minimum transaction amount in e-wallet is 100 VND", HttpStatus.BAD_REQUEST),
    MINIMUM_OUT_TRANSFER_MONEY(400, "The minimum transaction amount to bank account is 2,000 VND", HttpStatus.BAD_REQUEST),

    MAXIMUM_IN_TRANSFER_MONEY(400, "The maximum transaction amount in e-wallet is 50,000,000 VND", HttpStatus.BAD_REQUEST),
    MAXIMUM_OUT_TRANSFER_MONEY(400, "The maximum transaction amount to bank account is 99,999,999 VND", HttpStatus.BAD_REQUEST),

    DEFAULT_PA_BALANCE_NOT_ENOUGH(400, "Balance in default PA is not enough", HttpStatus.BAD_REQUEST),
    ACTIVE_PA_BALANCE_NOT_ENOUGH(400, "Balance in active PA is not enough", HttpStatus.BAD_REQUEST),
    BA_BALANCE_NOT_ENOUGH(400, "Balance in associated chosen BA is not enough", HttpStatus.BAD_REQUEST),

    UNEXPIRED_TRANSFER_NOT_EXIST(400, "Unexpired pending transfer does not exist", HttpStatus.BAD_REQUEST),
    UNEXPIRED_TRANSFER_INVALID(403, "Unexpired pending transfer is wrong RECEIVER", HttpStatus.FORBIDDEN),
    UNEXPIRED_TRANSFER_TO_PA_EXIST(403, "Unexpired pending transfer to PA exists more than one", HttpStatus.FORBIDDEN),
    UNEXPIRED_TRANSFER_TO_BA_EXIST(403, "Unexpired pending transfer to BA exists more than one", HttpStatus.FORBIDDEN),
    TRANSACTION_OF_CUSTOMER_NOT_FOUND(404, "Transaction of customer does not exist", HttpStatus.NOT_FOUND),
    SENDER_INVALID(400, "A sender account must be provided", HttpStatus.BAD_REQUEST),
    RECEIVER_INVALID(400, "A receiver account must be provided", HttpStatus.BAD_REQUEST),
    TRANSACTION_FAIL(403, "Verify wrong OTP for transaction exceed 3 times", HttpStatus.FORBIDDEN),

    // Beneficiary
    BENEFICIARY_SELF_PA_FORBIDDEN(403, "Cannot insert new beneficiary with your own Payment Account", HttpStatus.FORBIDDEN),
    BENEFICIARY_INVALID_PA(400, "Required id for getting PAYMENT_ACCOUNT receiver type of beneficiary", HttpStatus.BAD_REQUEST),
    BENEFICIARY_INVALID_BA(400, "Required id for getting BANK_ACCOUNT receiver type of beneficiary", HttpStatus.BAD_REQUEST),
    BENEFICIARY_NAME_OLD(400, "Please input new beneficiary name for updating", HttpStatus.BAD_REQUEST),
    BENEFICIARY_NAME_EXISTS(400, "Beneficiary name has been existed in your list", HttpStatus.BAD_REQUEST),

    // Reward
    REWARD_NAME_MISSING(400, "Missing reward name parameter", HttpStatus.BAD_REQUEST),
    REWARD_TYPE_MISSING(400, "Missing reward type parameter", HttpStatus.BAD_REQUEST),

    // Saving account
    SAVING_PARAMETER_MISSING(400, "Missing parameter(s) for valid saving account", HttpStatus.BAD_REQUEST),
    SAVING_AMOUNT_SMALLER_THAN_ZERO(400, "Saving initial amount must be greater than 0", HttpStatus.BAD_REQUEST),
    SAVING_AMOUNT_INITIAL_INVALID(400, "Saving initial amount must be greater than or equal to min balance of Interest Rate term", HttpStatus.BAD_REQUEST),
    SAVING_TYPE_INVALID(400, "Saving account type is invalid", HttpStatus.BAD_REQUEST),
    SAVING_AMOUNT_GREATER_THAN_ZERO(400, "Saving current amount must be 0 (withdrawn all)", HttpStatus.BAD_REQUEST),

    // OTP Errors
    SMS_OTP_NOT_FOUND(404, "SMS OTP does not exists", HttpStatus.NOT_FOUND),
    EMAIL_OTP_NOT_FOUND(404, "Email OTP does not exists", HttpStatus.NOT_FOUND),
    SMS_OTP_FAIL(500, "SMS OTP sent unsuccessfully", HttpStatus.INTERNAL_SERVER_ERROR),
    OTP_INVALID(400, "Invalid OTP", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(400, "OTP has been expired", HttpStatus.BAD_REQUEST),
    OTP_PURPOSE_MISMATCH(400, "OTP purpose does not match", HttpStatus.BAD_REQUEST),

    // 5xx Server Errors
    INTERNAL_SERVER_ERROR(500, "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR),
    TOKEN_FAILED(500, "There was an error when generate Token", HttpStatus.INTERNAL_SERVER_ERROR),

    // 6xx Custom Errors
    INVALID_TRANSACTION_TYPE(600, "Invalid transaction type", HttpStatus.BAD_REQUEST),
    INVALID_AMOUNT_TYPE(600, "Invalid amount type", HttpStatus.BAD_REQUEST),
    INVALID_TRANSACTION_SENDER_TYPE(600, "Invalid transaction sender type", HttpStatus.BAD_REQUEST),
    INVALID_TRANSACTION_RECEIVER_TYPE(600, "Invalid transaction receiver type", HttpStatus.BAD_REQUEST),
    INVALID_INPUT(600, "Invalid Input", HttpStatus.BAD_REQUEST);

    ErrorCode(int code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    private final int code;
    private final String message;
    private final HttpStatus status;
}
