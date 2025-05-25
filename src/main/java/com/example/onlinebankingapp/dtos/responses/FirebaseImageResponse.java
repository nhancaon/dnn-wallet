package com.example.onlinebankingapp.dtos.responses;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirebaseImageResponse {
    private String status;
    private String message;
    private String data;
}
