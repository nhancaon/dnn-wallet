package com.example.onlinebankingapp.controllers;

import com.example.onlinebankingapp.dtos.requests.BeneficiaryRequest;
import com.example.onlinebankingapp.dtos.responses.Employee.EmployeeListResponse;
import com.example.onlinebankingapp.entities.BeneficiaryEntity;
import com.example.onlinebankingapp.dtos.responses.Beneficiary.BeneficiaryListResponse;
import com.example.onlinebankingapp.dtos.responses.Beneficiary.BeneficiaryResponse;
import com.example.onlinebankingapp.dtos.responses.ResponseObject;
import com.example.onlinebankingapp.services.Beneficiary.BeneficiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {
    private final BeneficiaryService beneficiaryService;

    // End point for inserting a beneficiary for PA
    @PostMapping("/insertBeneficiaryOfPaymentAccount")
    public ResponseEntity<?> insertBeneficiaryOfPaymentAccount(
            @Valid @RequestBody BeneficiaryRequest beneficiaryRequest
    ) {
        // Insert a beneficiary for PA
        BeneficiaryEntity beneficiaryPA = beneficiaryService.insertBeneficiaryOfPaymentAccount(beneficiaryRequest);

        // Return a successful response with the inserted beneficiary details
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Insert a PA beneficiary successfully")
                .result(BeneficiaryResponse.fromBeneficiary(beneficiaryPA))
                .build());
    }

    // End point for inserting a beneficiary for BA
    @PostMapping("/insertBeneficiaryOfBankAccount")
    public ResponseEntity<?> insertBeneficiaryOfBankAccount(
            @Valid @RequestBody BeneficiaryRequest beneficiaryRequest
    ) {
        // Insert a beneficiary for BA
        BeneficiaryEntity beneficiaryBA = beneficiaryService.insertBeneficiaryOfBankAccount(beneficiaryRequest);

        // Return a successful response with the inserted beneficiary details
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Insert a BA beneficiary successfully")
                .result(BeneficiaryResponse.fromBeneficiary(beneficiaryBA))
                .build());
    }

    // End point for getting a beneficiary for PA by its id
    @GetMapping("/getBeneficiaryOfPaymentAccount/{beneficiaryId}")
    public ResponseEntity<?> getBeneficiaryOfPaymentAccount(
            @Valid @PathVariable("beneficiaryId") Long beneficiaryId
    ){
        // Retrieve the beneficiary by ID using the beneficiaryService
        BeneficiaryEntity beneficiary = beneficiaryService.getBeneficiaryOfPaymentAccountById(beneficiaryId);

        // Return a successful response with the beneficiary details
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get a PAYMENT_ACCOUNT beneficiary successfully")
                .result(BeneficiaryResponse.fromBeneficiary(beneficiary))
                .build());
    }

    // End point for getting list of beneficiaries for PA by customer id
    @GetMapping("/getBeneficiariesOfPaymentAccount/{customerId}")
    public ResponseEntity<?> getBeneficiariesOfPaymentAccount(
            @Valid @PathVariable("customerId") Long customerId
    ){
        // Retrieve PAYMENT_ACCOUNT beneficiaries by customer ID
        List<Map<String, Object>> beneficiaries = beneficiaryService.getBeneficiariesOfPaymentAccount(customerId);

        // Return a successful response with the list of PAYMENT_ACCOUNT beneficiaries
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get list of beneficiaries for PA successfully")
                .result(beneficiaries)
                .build());
    }

    // End point for getting a beneficiary for BA by its id
    @GetMapping("/getBeneficiaryOfBankAccount/{beneficiaryId}")
    public ResponseEntity<?> getBeneficiaryOfBankAccount(
            @Valid @PathVariable("beneficiaryId") Long beneficiaryId
    ){
        // Retrieve the beneficiary by ID using the beneficiaryService
        BeneficiaryEntity beneficiary = beneficiaryService.getBeneficiaryOfBankAccountById(beneficiaryId);

        // Return a successful response with the beneficiary details
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get a BANK_ACCOUNT beneficiary successfully")
                .result(BeneficiaryResponse.fromBeneficiary(beneficiary))
                .build());
    }

    // End point for getting list of beneficiaries for BA by customer id
    @GetMapping("/getBeneficiariesOfBankAccount/{customerId}")
    public ResponseEntity<?> getBeneficiariesOfBankAccount(
            @Valid @PathVariable("customerId") Long customerId
    ){
        // Retrieve BANK_ACCOUNT beneficiaries by customer ID
        List<Map<String, Object>> beneficiaries = beneficiaryService.getBeneficiariesOfBankAccount(customerId);

        // Return a successful response with the list of BANK_ACCOUNT beneficiaries
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get list of beneficiaries for BA successfully")
                .result(beneficiaries)
                .build());
    }

    // End point for getting list of all beneficiaries of a customer
    @GetMapping("/getAllBeneficiaries/{customerId}")
    public ResponseEntity<?> getAllBeneficiaries(
            @Valid @PathVariable("customerId") Long customerId
    ){
        // Retrieve beneficiaries by customer ID
        List<BeneficiaryEntity> beneficiaries = beneficiaryService.getAllBeneficiaries(customerId);

        // Build response
        List<BeneficiaryResponse> beneficiariesResponse = beneficiaries.stream()
                .map(BeneficiaryResponse::fromBeneficiary)
                .toList();

        BeneficiaryListResponse beneficiaryListResponse = BeneficiaryListResponse.builder()
                .beneficiaries(beneficiariesResponse)
                .build();

        // Return a successful response with the list of all beneficiaries
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get list of all beneficiaries successfully!")
                .result(beneficiaryListResponse)
                .build());
    }

    // Get all beneficiaries paginated
    @GetMapping("/getPaginationListBeneficiary")
    public ResponseEntity<?> getPaginationListBeneficiary(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String orderedBy,
            @RequestParam(defaultValue = "false") String isAscending,
            @RequestParam(defaultValue = "") String keyword
    ) {
        Boolean isAsc = Boolean.parseBoolean(isAscending);

        BeneficiaryListResponse beneficiaryPaginated = beneficiaryService
                .getPaginationListBeneficiary(page, size, orderedBy, isAsc, keyword);

        //return result in response
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get all paginated beneficiaries successfully")
                .result(beneficiaryPaginated)
                .build());
    }

    // End point for updating name of beneficiary for PA
    @PutMapping("/updateNameOfPaymentAccountBeneficiary/{beneficiaryId}")
    public ResponseEntity<?> updateNameOfPaymentAccountBeneficiary(
            @Valid @PathVariable("beneficiaryId") Long beneficiaryId,
            @Valid @RequestBody BeneficiaryRequest beneficiaryRequest
    ) {
        // Updating name of the beneficiary
        BeneficiaryEntity updatedBeneficiaryPA = beneficiaryService
                .updateNameOfPaymentAccountBeneficiary(beneficiaryId, beneficiaryRequest);

        // Return result in response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Update beneficiary's name of PA with ID: " + beneficiaryId + " successfully!")
                .result(BeneficiaryResponse.fromBeneficiary(updatedBeneficiaryPA))
                .build());
    }

    // End point for updating name of beneficiary for BA
    @PutMapping("/updateNameOfBankAccountBeneficiary/{beneficiaryId}")
    public ResponseEntity<?> updateNameOfBankAccountBeneficiary(
            @Valid @PathVariable("beneficiaryId") Long beneficiaryId,
            @Valid @RequestBody BeneficiaryRequest beneficiaryRequest
    ) {
        // Updating name of the beneficiary
        BeneficiaryEntity updatedBeneficiaryBA = beneficiaryService
                .updateNameOfBankAccountBeneficiary(beneficiaryId, beneficiaryRequest);

        // Return result in response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Update beneficiary's name of BA with ID: " + beneficiaryId + " successfully!")
                .result(BeneficiaryResponse.fromBeneficiary(updatedBeneficiaryBA))
                .build());
    }

    // End point for deleting a beneficiary for PA
    @DeleteMapping("/deleteBeneficiaryOfPaymentAccount/{beneficiaryId}")
    public ResponseEntity<?> deleteBeneficiaryOfPaymentAccount(
            @Valid @PathVariable("beneficiaryId") Long beneficiaryId
    ){
        // Delete the requested beneficiary
        beneficiaryService.deleteBeneficiaryOfPaymentAccount(beneficiaryId);

        // Return result in response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Delete beneficiary of PAYMENT_ACCOUNT ID: " + beneficiaryId + " successfully")
                .build());
    }

    // End point for deleting a beneficiary for BA
    @DeleteMapping("/deleteBeneficiaryOfBankAccount/{beneficiaryId}")
    public ResponseEntity<?> deleteBeneficiaryOfBankAccount(
            @Valid @PathVariable("beneficiaryId") Long beneficiaryId
    ){
        // Delete the requested beneficiary
        beneficiaryService.deleteBeneficiaryOfBankAccount(beneficiaryId);

        // Return result in response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Delete beneficiary of BANK_ACCOUNT ID: " + beneficiaryId + " successfully")
                .build());
    }
}
