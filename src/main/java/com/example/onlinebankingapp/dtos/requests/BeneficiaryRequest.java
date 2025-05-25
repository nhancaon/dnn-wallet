package com.example.onlinebankingapp.dtos.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BeneficiaryRequest {
    @JsonProperty("name")
    private String name;

    @JsonProperty("customer_id")
    private long customerId;

    @JsonProperty("receiver_id")
    private long receiverId;

    @JsonProperty("beneficiary_receiver_type")
    private String beneficiaryReceiverType;

    @JsonProperty("bank_name")
    private String bankName;
}
