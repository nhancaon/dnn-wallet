package com.example.onlinebankingapp.controllers;

import com.example.onlinebankingapp.dtos.requests.InterestRateRequest;
import com.example.onlinebankingapp.entities.InterestRateEntity;
import com.example.onlinebankingapp.dtos.responses.InterestRate.InterestRateListResponse;
import com.example.onlinebankingapp.dtos.responses.InterestRate.InterestRateResponse;
import com.example.onlinebankingapp.dtos.responses.ResponseObject;
import com.example.onlinebankingapp.services.InterestRate.InterestRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interestRates")
@RequiredArgsConstructor
public class InterestRateController {
    private final InterestRateService interestRateService;

    // End point for inserting an interest rate
    @PostMapping("/insertInterestRate")
    public ResponseEntity<?> insertInterestRate(
            @Valid @RequestBody InterestRateRequest interestRateRequest
    ) {
        // Insert the interest rate using the provided DTO
        InterestRateEntity interestRateResponse = interestRateService.insertInterestRate(interestRateRequest);

        // Return a successful response with the inserted interest rate data
        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Insert interest rate successfully")
                .result(InterestRateResponse.fromInterestRate(interestRateResponse))
                .build());
    }

    // End point for getting all the interest rates
    @GetMapping("/getAllInterestRates")
    public ResponseEntity<?> getAllInterestRates(){
        // Retrieve all interest rates
        List<InterestRateEntity> interestRateEntities = interestRateService.getAllInterestRates();

        //build response
        List<InterestRateResponse> interestRateResponses = interestRateEntities.stream()
                .map(InterestRateResponse::fromInterestRate)
                .toList();

        InterestRateListResponse interestRateListResponse = InterestRateListResponse.builder()
                .interestRateResponses(interestRateResponses)
                .build();

        // Return a successful response with the interest rate data
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get all interest rates successfully")
                .result(interestRateListResponse)
                .build());
    }

    // End point for getting all the interest rates with pagination
    @GetMapping("/getPaginationListInterestRate")
    public ResponseEntity<?> getPaginationListInterestRates(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String orderedBy,
            @RequestParam(defaultValue = "false") String isAscending,
            @RequestParam(defaultValue = "0") Integer keyword
    ){
        Boolean isAsc = Boolean.parseBoolean(isAscending);

        InterestRateListResponse interestRatePaginated = interestRateService.
                getPaginationListInterestRate(page, size, orderedBy, isAsc, keyword);

        // Return a successful response with the interest rate data
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get all paginated interest rates successfully")
                .result(interestRatePaginated)
                .build());
    }

    // End point for getting an interest rate by id
    @GetMapping("/getInterestRateById/{interestRateId}")
    public ResponseEntity<?> getInterestRateById(
            @Valid @PathVariable("interestRateId") Long interestRateId
    ){
        // Retrieve the interest rate by ID
        InterestRateEntity queryInterestRate = interestRateService.getInterestRateById(interestRateId);

        // Return a successful response with the interest rate data
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Get interest rate with ID: " + interestRateId + " successfully!")
                .result(InterestRateResponse.fromInterestRate(queryInterestRate))
                .build());
    }

    // End point for updating an interest rate
    @PutMapping("/updateInterestRate/{interestRateId}")
    public ResponseEntity<?> updateInterestRate(
            @Valid @PathVariable("interestRateId") Long interestRateId,
            @Valid @RequestBody InterestRateRequest interestRateRequest
    ) {
        // Updating the interest rate
        InterestRateEntity updatedInterestRate = interestRateService.updateInterestRate(interestRateId, interestRateRequest);

        // Return result in response
        return ResponseEntity.ok().body(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Update interest rate with ID: " + interestRateId + " successfully!")
                .result(InterestRateResponse.fromInterestRate(updatedInterestRate))
                .build());
    }

    @DeleteMapping("/deleteInterestRate/{interestRateId}")
    public ResponseEntity<?> deleteInterestRate(
            @Valid @PathVariable("interestRateId") Long interestRateId
    ) {
        interestRateService.deleteInterestRateById(interestRateId);

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Delete interest rate with ID: " + interestRateId + " successfully")
                .build());
    }
}
