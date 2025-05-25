package com.example.onlinebankingapp.dtos.responses.IDCardExtraction;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddFileResponse {
    private String message;

    private Map<String, Object> object;
}