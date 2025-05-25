package com.example.onlinebankingapp.dtos.responses.Beneficiary;

import com.example.onlinebankingapp.entities.BeneficiaryEntity;
import com.example.onlinebankingapp.enums.BeneficiaryReceiverType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BeneficiaryResponse {
    private Long id;

    private String name;

    @JsonProperty("customer_id")
    private long customerId;

    @JsonProperty("receiver_id")
    private long receiverId;

    @JsonProperty("beneficiary_receiver_type")
    private String beneficiaryReceiverType;

    // Static method to create a BeneficiaryResponse object from a BeneficiaryEntity object
    public static BeneficiaryResponse fromBeneficiary(BeneficiaryEntity beneficiary){
        return BeneficiaryResponse.builder()
                .id(beneficiary.getId())
                .name(beneficiary.getName())
                .customerId(beneficiary.getCustomer().getId())
                .receiverId(beneficiary.getReceiverId())
                .beneficiaryReceiverType(String.valueOf(beneficiary.getBeneficiaryReceiverType()))
                .build();
    }
}
