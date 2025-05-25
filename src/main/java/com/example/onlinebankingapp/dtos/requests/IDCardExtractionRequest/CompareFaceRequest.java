package com.example.onlinebankingapp.dtos.requests.IDCardExtractionRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompareFaceRequest {
    @JsonProperty("img_front")
    private String imgFront;

    @JsonProperty("img_face")
    private String imgFace;

    @JsonProperty("client_session")
    private String clientSession;

    private String token;
}