package com.example.onlinebankingapp.controllers;

import com.example.onlinebankingapp.dtos.responses.IDCardExtraction.AddFileResponse;
import com.example.onlinebankingapp.dtos.responses.ResponseObject;
import com.example.onlinebankingapp.services.IDCardExtraction.IDCardExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/idCardExtraction")
@RequiredArgsConstructor
public class IDCardExtractionController {
    private final IDCardExtractionService idCardExtractionService;

    @PostMapping("/uploadFile")
    public ResponseEntity<?> handleFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("clientSession") String clientSession
    ) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is required.");
        }

        AddFileResponse addFileResponse = idCardExtractionService.addFile(file, title, description, clientSession);

        return ResponseEntity.ok(ResponseObject.builder()
                .status(HttpStatus.OK)
                .message("Enable the customer account successfully. Now can login")
                .result(addFileResponse)
                .build());
    }
}
