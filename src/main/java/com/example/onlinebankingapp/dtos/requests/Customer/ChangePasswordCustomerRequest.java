package com.example.onlinebankingapp.dtos.requests.Customer;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ChangePasswordCustomerRequest {
    // Password field with validation for not being blank
    @NotBlank(message =  "Password cannot be blank")
    private String password;

    // New password field with validation for not being blank
    @NotBlank(message = "New password cannot be blank")
    @JsonProperty("new_password")
    private String newPassword;

    // Confirm password field with validation for not being blank
    @NotBlank(message = "Confirm password cannot be blank")
    @JsonProperty("confirm_password")
    private String confirmPassword;
}
