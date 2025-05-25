package com.example.onlinebankingapp.dtos.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseObject {
    @Builder.Default
    private int code = 200;

    @JsonProperty("message")
    private String message;

    @JsonProperty("status")
    private HttpStatus status;

    @JsonProperty("result")
    private Object result;

    @JsonProperty("multi_result")
    private Map<String, Object> multiResult;
}
