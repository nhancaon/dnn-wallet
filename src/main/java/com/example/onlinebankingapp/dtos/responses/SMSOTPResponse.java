package com.example.onlinebankingapp.dtos.responses;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SMSOTPResponse {
    private String status;
    private String code;
    private DataSMSDTO data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataSMSDTO {
        private long tranId;
        private String name;
        private int totalSMS;
        private int totalPrice;
        private List<String> invalidPhone;
    }
}
