package com.example.onlinebankingapp.dtos.requests.IDCardExtractionRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FrontSideRequest {
    @JsonProperty("img_front")
    private String imgFront; // quan trong

    @JsonProperty("client_session")
    private String clientSession; // IOS_iphone6plus_ios13_Device_1.3.6_CC332797-E3E5-475F-8546-C9C4AA348837_1581429032

    private Integer type; // -1

    @JsonProperty("validate_postcode")
    private Boolean validatePostcode; // true

    private String token; // "your_encrypted_token"
}