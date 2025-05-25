package com.example.onlinebankingapp.controllers;

import com.example.onlinebankingapp.dtos.requests.BankRequest;
import com.example.onlinebankingapp.entities.BankEntity;
import com.example.onlinebankingapp.dtos.responses.Bank.BankListResponse;
import com.example.onlinebankingapp.dtos.responses.Bank.BankResponse;
import com.example.onlinebankingapp.dtos.responses.ResponseObject;
import com.example.onlinebankingapp.services.Bank.BankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banks")
@RequiredArgsConstructor
public class BankController {
    private final BankService bankService;

    @PostMapping("/insertBank")
    public ResponseEntity<?> insertBank(
            @Valid @RequestBody BankRequest bankRequest
    ) {
        // Insert a bank
        BankEntity bankEntityResponse = bankService.insertBank(bankRequest);

        // Return data in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Insert bank successfully")
                .result(BankResponse.fromBank(bankEntityResponse))
                .build());
    }

    @GetMapping("/getAllBanks")
    public ResponseEntity<?> getAllBanks() {
        // Retrieve a list of bank entities
        List<BankEntity> bankEntityList = bankService.getAllBanks();

        //create response
        List<BankResponse> bankResponseList = bankEntityList.stream()
                .map(BankResponse::fromBank)
                .toList();

        BankListResponse bankListResponse = BankListResponse.builder()
                .banks(bankResponseList)
                .build();

        // Return the response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get bank list successfully!")
                .result(bankListResponse)
                .build());
    }

    @GetMapping("/getBankByName/{name}")
    public ResponseEntity<?> getBankByName(
            @Valid @PathVariable("name") String name
    ) {
        // Retrieve a bank entity by its ID
        BankEntity bankEntity = bankService.getBankByName(name);

        // return response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .message("Get bank successfully")
                .status(HttpStatus.OK)
                .result(BankResponse.fromBank(bankEntity))
                .build());
    }

    @PutMapping("/updateBank/{oldBankName}")
    public ResponseEntity<?> updateBank(
            @Valid @PathVariable("oldBankName") String oldBankName,
            @Valid @RequestBody BankRequest bankRequest
    ) {
        BankEntity bankEntityResponse = bankService.updateBankName(oldBankName, bankRequest);

        // Return updated bank information in the response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Update bank successfully")
                .result(BankResponse.fromBank(bankEntityResponse))
                .build());
    }

    @DeleteMapping("/deleteBank/{bankName}")
    public ResponseEntity<?> deleteBank(
            @Valid @PathVariable("bankName") String bankName
    ) {
        bankService.deleteBankByName(bankName);

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Delete bank with name: " + bankName + " successfully")
                .build());
    }
}
