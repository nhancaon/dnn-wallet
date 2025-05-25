package com.example.onlinebankingapp.dtos.responses.Bank;

import com.example.onlinebankingapp.entities.BankEntity;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BankResponse {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    public static BankResponse fromBank(BankEntity bank){
        return BankResponse.builder()
                .id(bank.getId())
                .name(bank.getName())
                .build();
    }
}
