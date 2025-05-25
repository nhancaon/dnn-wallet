package com.example.onlinebankingapp.dtos.responses.IDCardExtraction;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IDCardExtractionResponse {
    private String message;

    @JsonProperty("server_version")
    private String serverVersion;

    private Map<String, Object> object;
}