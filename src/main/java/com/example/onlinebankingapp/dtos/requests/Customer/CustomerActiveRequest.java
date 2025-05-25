package com.example.onlinebankingapp.dtos.requests.Customer;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomerActiveRequest extends CustomerRequest {
    private String otp;
}
