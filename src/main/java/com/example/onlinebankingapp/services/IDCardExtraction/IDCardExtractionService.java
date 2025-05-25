package com.example.onlinebankingapp.services.IDCardExtraction;

import com.example.onlinebankingapp.dtos.requests.IDCardExtractionRequest.BackSideRequest;
import com.example.onlinebankingapp.dtos.requests.IDCardExtractionRequest.CompareFaceRequest;
import com.example.onlinebankingapp.dtos.requests.IDCardExtractionRequest.FrontSideRequest;
import com.example.onlinebankingapp.entities.CustomerEntity;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import com.example.onlinebankingapp.repositories.CustomerRepository;
import com.example.onlinebankingapp.dtos.responses.IDCardExtraction.AddFileResponse;
import com.example.onlinebankingapp.dtos.responses.IDCardExtraction.CompareIDCardFaceResponse;
import com.example.onlinebankingapp.dtos.responses.IDCardExtraction.IDCardExtractionResponse;
import com.example.onlinebankingapp.services.Customer.CustomerServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IDCardExtractionService {
    private static final Logger logger = LoggerFactory.getLogger(IDCardExtractionService.class);

    @Value("${spring.eKyc.baseUrl}")
    private String baseUrl;

    @Value("${spring.eKyc.tokenId}")
    private String tokenId;

    @Value("${spring.eKyc.tokenKey}")
    private String tokenKey;

    @Value("${spring.eKyc.accessToken}")
    private String accessToken;

    @Value("${spring.eKyc.macAddress}")
    private String macAddress;

    private final ObjectMapper objectMapper;
    private final CustomerRepository customerRepository;
    private final CustomerServiceImpl customerServiceImpl;

    // Add File to get hash MinIO code
    public AddFileResponse addFile(MultipartFile file, String title, String description, String clientSession) {
        String url = baseUrl + "/file-service/v1/addFile";

        try {
            // Create a connection
            HttpURLConnection conn = createJSONRequest(url, "POST");

            // Prepare form-data body
            String boundary = Long.toHexString(System.currentTimeMillis()); // Just random ID
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setDoOutput(true);

            // Send multipart request
            try (OutputStream output = conn.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true)
            ) {

                // Add title
                writer.append("--" + boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"title\"").append("\r\n");
                writer.append("\r\n").append(title).append("\r\n").flush();

                // Add description
                writer.append("--" + boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"description\"").append("\r\n");
                writer.append("\r\n").append(description).append("\r\n").flush();

                // Add file
                writer.append("--" + boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getOriginalFilename() + "\"").append("\r\n");
                writer.append("Content-Type: " + file.getContentType()).append("\r\n");
                writer.append("\r\n").flush();
                file.getInputStream().transferTo(output);
                output.flush(); // Important before continuing with writer!

                writer.append("\r\n").flush(); // End of multipart/form-data.
                writer.append("--" + boundary + "--").append("\r\n").flush();
            }

            // Get response
            String response = getResponse(conn);
            AddFileResponse addFileResponse = objectMapper.readValue(response, AddFileResponse.class);

            // Hash code
            Map<String, Object> objectAddFile = addFileResponse.getObject();
            String hashCode = (String) objectAddFile.get("hash");

            // Check title
            // Front
            if(title.equals("FRONT")){
                logger.info("Processing in FRONT condition");
                FrontSideRequest frontSideRequest = new FrontSideRequest();
                frontSideRequest.setImgFront(hashCode);
                frontSideRequest.setClientSession(clientSession);
                frontSideRequest.setType(-1);
                frontSideRequest.setValidatePostcode(true);
                frontSideRequest.setToken("your_encrypted_token");

                IDCardExtractionResponse extractFrontSide = extractFrontSide(frontSideRequest, clientSession);
                logger.info("Front extraction ID card: {}", extractFrontSide);
            }
            // Back
            if(title.equals("BACK")){
                logger.info("Processing in BACK condition");
                BackSideRequest backSideRequest = new BackSideRequest();
                backSideRequest.setImgBack(hashCode);
                backSideRequest.setClientSession(clientSession);
                backSideRequest.setType(-1);
                backSideRequest.setToken("your_encrypted_token");

                IDCardExtractionResponse extractBackSide = extractBackSide(backSideRequest);
                logger.info("Back extraction ID card: {}", extractBackSide);
            }
            // Face
            if(title.equals("FACE")){
                logger.info("Processing in FACE condition");
                Optional<CustomerEntity> optionalCustomer = customerRepository.findByClientSession(clientSession);
                if(optionalCustomer.isPresent()){
                    CustomerEntity sameClientSession = optionalCustomer.get();
                    if(sameClientSession.getImgFace().isEmpty() || sameClientSession.getImgFace().isBlank()){
                        CompareFaceRequest compareFaceRequest = new CompareFaceRequest();
                        compareFaceRequest.setImgFront(sameClientSession.getImgFront());
                        compareFaceRequest.setImgFace(hashCode);
                        compareFaceRequest.setClientSession(clientSession);
                        compareFaceRequest.setToken("your_encrypted_token");

                        CompareIDCardFaceResponse compareIDCardFaceResponse = compareFace(compareFaceRequest, clientSession);
                        logger.info("Compare FACE and ID Card: {}", compareIDCardFaceResponse);
                    }
                }
            }
            // Login using FACE ID
            if(title.equals("FACE_ID_LOGIN")){
                logger.info("Processing in FACE_ID_LOGIN condition");
                Optional<CustomerEntity> optionalCustomer = customerRepository.findByClientSession(clientSession);
                if(optionalCustomer.isEmpty()){
                    throw new AppException(ErrorCode.USER_NOT_FOUND);
                }

                CustomerEntity existingCustomerWithClientSession = optionalCustomer.get();
                if(!existingCustomerWithClientSession.getImgFront().isEmpty() || !existingCustomerWithClientSession.getImgFront().isBlank()){
                    CompareFaceRequest compareFaceRequest = new CompareFaceRequest();
                    compareFaceRequest.setImgFront(existingCustomerWithClientSession.getImgFront());
                    compareFaceRequest.setImgFace(hashCode);
                    compareFaceRequest.setClientSession(clientSession);
                    compareFaceRequest.setToken("your_encrypted_token");

                    CompareIDCardFaceResponse compareIDCardFaceResponse = loginFaceId(compareFaceRequest, clientSession);
                    logger.info("Compare ID Card portrait and current FACE for login: {}", compareIDCardFaceResponse);

                    // Add loginStatus to the object map after successful login
                    Map<String, Object> objectMap = addFileResponse.getObject();
                    objectMap.put("loginStatus", true);
                }
            }
            // Allow transfer >= 10M
            if(title.equals("TRANSFER_MONEY")){
                logger.info("Processing in TRANSFER_MONEY condition");
                Optional<CustomerEntity> optionalCustomer = customerRepository.findByClientSession(clientSession);
                if(optionalCustomer.isEmpty()){
                    throw new AppException(ErrorCode.USER_NOT_FOUND);
                }

                CustomerEntity existingCustomerWithClientSession = optionalCustomer.get();
                if(!existingCustomerWithClientSession.getImgFront().isEmpty() || !existingCustomerWithClientSession.getImgFront().isBlank()){
                    CompareFaceRequest compareFaceRequest = new CompareFaceRequest();
                    compareFaceRequest.setImgFront(existingCustomerWithClientSession.getImgFront());
                    compareFaceRequest.setImgFace(hashCode);
                    compareFaceRequest.setClientSession(clientSession);
                    compareFaceRequest.setToken("your_encrypted_token");

                    CompareIDCardFaceResponse compareIDCardFaceResponse = transferFaceId(compareFaceRequest, clientSession);
                    logger.info("Compare ID Card portrait and current FACE for transfer >= 10M: {}", compareIDCardFaceResponse);

                    // Add loginStatus to the object map after successful login
                    Map<String, Object> objectMap = addFileResponse.getObject();
                    objectMap.put("allowTransfer", true);
                }
            }

            logger.info("Successfully uploaded file: {}", addFileResponse);
            return addFileResponse;

        } catch (Exception e) {
            logger.error("Error uploading file to get hash code: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to get hash MinIO code", e);
        }
    }

    // Extract ID CARD front side information
    public IDCardExtractionResponse extractFrontSide(FrontSideRequest frontSideRequest, String clientSession) {
        String url = baseUrl + "/ai/v1/ocr/id/front"; // Endpoint to extract front side

        try {
            // Create connection
            HttpURLConnection conn = createJSONRequest(url, "POST");
            String jsonRequest = objectMapper.writeValueAsString(frontSideRequest);
            sendRequest(conn, jsonRequest);

            // Get response
            String response = getResponse(conn);
            IDCardExtractionResponse idCardResponse = objectMapper.readValue(response, IDCardExtractionResponse.class);

            // Access the 'object' field
            Map<String, Object> objectMap = idCardResponse.getObject();
            // Error case: have to capture again the front side
            if(!isFrontIDCardValid(objectMap)){
                throw new DataIntegrityViolationException("Data is not valid. Please capture FRONT ID Card again");
            }

            // SAVE CUSTOMER -> REALLY FUCKING IMPORTANT
            customerServiceImpl.saveCustomerFrontIDCardExtraction(idCardResponse,
                    frontSideRequest.getImgFront(), clientSession);

            logger.info("Successfully extracted ID Card front side: {}", idCardResponse);
            return idCardResponse;
        } catch (Exception e) {
            logger.error("Error extracting ID Card front side: {}", e.getMessage());
            throw new RuntimeException("Failed to extract ID Card front side", e);
        }
    }

    // Extract ID CARD back side information
    public IDCardExtractionResponse extractBackSide(BackSideRequest backSideRequest) {
        String url = baseUrl + "/ai/v1/ocr/id/back"; // Endpoint to extract back side

        try {
            // Create connection
            HttpURLConnection conn = createJSONRequest(url, "POST");
            String jsonRequest = objectMapper.writeValueAsString(backSideRequest);
            sendRequest(conn, jsonRequest);

            // Get response
            String response = getResponse(conn);
            IDCardExtractionResponse idCardResponse = objectMapper.readValue(response, IDCardExtractionResponse.class);

            // Access the 'object' field
            Map<String, Object> objectMap = idCardResponse.getObject();
            // Error case: have to capture again the back side
            if(!isBackIDCardValid(objectMap)){
                throw new DataIntegrityViolationException("Please capture BACK ID Card again");
            }

            customerServiceImpl.saveCustomerBackIDCardExtraction(idCardResponse, backSideRequest.getImgBack());

            logger.info("Successfully extracted ID Card back side: {}", idCardResponse);
            return idCardResponse;
        } catch (Exception e) {
            logger.error("Error extracting ID Card back side: {}", e.getMessage());
            throw new RuntimeException("Failed to extract ID Card back side", e);
        }
    }

    // Compare face and ID card
    public CompareIDCardFaceResponse compareFace(CompareFaceRequest compareFaceRequest, String clientSession) {
        String url = baseUrl + "/ai/v1/face/compare"; // Endpoint to compare face and ID card

        try {
            // Create connection
            HttpURLConnection conn = createJSONRequest(url, "POST");
            String jsonRequest = objectMapper.writeValueAsString(compareFaceRequest);
            sendRequest(conn, jsonRequest);

            // Get response
            String response = getResponse(conn);
            CompareIDCardFaceResponse compareIDCardFaceResponse = objectMapper.readValue(response, CompareIDCardFaceResponse.class);

            // Access the 'object' field
            Map<String, Object> objectMap = compareIDCardFaceResponse.getObject();
            // Error case: have to capture again the face
            if(!isFaceAndIdCardValid(objectMap)){
                throw new DataIntegrityViolationException("Please capture FACE again");
            }

            customerServiceImpl.saveCustomerFace(compareFaceRequest.getImgFace(), clientSession);

            logger.info("Successfully compare Face and ID card: {}", compareIDCardFaceResponse);
            return compareIDCardFaceResponse;
        } catch (Exception e) {
            logger.error("Error compare ID Card and FACE: {}", e.getMessage());
            throw new RuntimeException("Failed to compare ID Card and FACE", e);
        }
    }

    // Compare ID card and current FACE for login
    public CompareIDCardFaceResponse loginFaceId(CompareFaceRequest compareFaceRequest, String clientSession) {
        String url = baseUrl + "/ai/v1/face/compare"; // Endpoint to compare ID card and current FACE for login

        try {
            // Create connection
            HttpURLConnection conn = createJSONRequest(url, "POST");
            String jsonRequest = objectMapper.writeValueAsString(compareFaceRequest);
            sendRequest(conn, jsonRequest);

            // Get response
            String response = getResponse(conn);
            CompareIDCardFaceResponse compareIDCardFaceResponse = objectMapper.readValue(response, CompareIDCardFaceResponse.class);

            // Access the 'object' field
            Map<String, Object> objectMap = compareIDCardFaceResponse.getObject();
            // Error case: have to capture again the face
            if(!isFaceAndIdCardValid(objectMap)){
                throw new DataIntegrityViolationException("Please capture FACE again");
            }

            logger.info("Successfully compare ID card and current FACE for login: {}", compareIDCardFaceResponse);
            return compareIDCardFaceResponse;
        } catch (Exception e) {
            logger.error("Error compare ID card and current FACE for login: {}", e.getMessage());
            throw new RuntimeException("Failed to compare ID card and current FACE for login", e);
        }
    }

    // Compare ID card and current FACE for transfer >= 10 M
    public CompareIDCardFaceResponse transferFaceId(CompareFaceRequest compareFaceRequest, String clientSession) {
        String url = baseUrl + "/ai/v1/face/compare"; // Endpoint to compare ID card and current FACE for login

        try {
            // Create connection
            HttpURLConnection conn = createJSONRequest(url, "POST");
            String jsonRequest = objectMapper.writeValueAsString(compareFaceRequest);
            sendRequest(conn, jsonRequest);

            // Get response
            String response = getResponse(conn);
            CompareIDCardFaceResponse compareIDCardFaceResponse = objectMapper.readValue(response, CompareIDCardFaceResponse.class);

            // Access the 'object' field
            Map<String, Object> objectMap = compareIDCardFaceResponse.getObject();
            // Error case: have to capture again the face
            if(!isFaceAndIdCardValid(objectMap)){
                throw new DataIntegrityViolationException("Please capture FACE again");
            }

            logger.info("Successfully compare ID card and current FACE for login: {}", compareIDCardFaceResponse);
            return compareIDCardFaceResponse;
        } catch (Exception e) {
            logger.error("Error compare ID card and current FACE for transfer: {}", e.getMessage());
            throw new RuntimeException("Failed to compare ID card and current FACE for login", e);
        }
    }

    // Method to create a JSON Request
    private HttpURLConnection createJSONRequest(String urlString, String method) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Token-id", tokenId);
        conn.setRequestProperty("Token-key", tokenKey);
        conn.setRequestProperty("mac-address", macAddress);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        return conn;
    }

    // Method to send the request
    private void sendRequest(HttpURLConnection conn, String json) throws Exception {
        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.writeBytes(json);
            wr.flush();
        }
    }

    // Helper method to get the response
    private String getResponse(HttpURLConnection conn) throws Exception {
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            InputStream errorStream = conn.getErrorStream();
            String errorMessage = new BufferedReader(new InputStreamReader(errorStream))
                    .lines().collect(Collectors.joining("\n"));
            throw new RuntimeException("Request failed with status code: " + responseCode + ", Error: " + errorMessage);
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            return response.toString();
        }
    }

    // Safely converts both Integer and Double to double
    public Double extractNumericValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            throw new IllegalArgumentException("Unsupported data type: " + value.getClass().getName());
        }
    }

    // Validate the front information
    public boolean isFrontIDCardValid(Map<String, Object> objectMap){
        // Zone 1
        String msg = (String) objectMap.get("msg");
        Double nameProb = extractNumericValue(objectMap.get("name_prob"));
        Double nameFakeWarningProb = extractNumericValue(objectMap.get("name_fake_warning_prob"));
        String expireWarning = (String) objectMap.get("expire_warning");
        // Check zone 1
        if(!msg.equals("OK") || nameProb < 0.9 || nameFakeWarningProb > 0.12 || !expireWarning.equalsIgnoreCase("no")){
            logger.info("Catch error in zone 1");
            return false;
        }

        // Zone 2
        Boolean addressFakeWarning = (Boolean) objectMap.get("address_fake_warning");
        Double validDateProb = extractNumericValue(objectMap.get("valid_date_prob"));
        String cornerWarning = (String) objectMap.get("corner_warning");
        List<String> generalWarning = (List<String>) objectMap.get("general_warning");
        // Check zone 2
        if(addressFakeWarning == true || validDateProb < 0.9 || !cornerWarning.equalsIgnoreCase("no")
                || !generalWarning.isEmpty()){
            logger.info("Catch error in zone 2");
            return false;
        }

        // Zone 3
        Boolean duplicationWarning = (Boolean) objectMap.get("dupplication_warning");
        Double idFakeProb = extractNumericValue(objectMap.get("id_fake_prob"));
        Double dobFakeWarningProb = extractNumericValue(objectMap.get("dob_fake_warning_prob"));
        String idFakeWarning = (String) objectMap.get("id_fake_warning");
        // Check zone 3
        if(duplicationWarning == true || idFakeProb > 0.12 || dobFakeWarningProb > 0.12 || !idFakeWarning.equalsIgnoreCase("no")){
            logger.info("Catch error in zone 3");
            return false;
        }

        // Zone 4
        Boolean dobFakeWarning = (Boolean) objectMap.get("dob_fake_warning");
        // Check zone 4
        if(dobFakeWarning == true){
            logger.info("Catch error in zone 4");
            return false;
        }

        // Check if 'tampering' exists in the objectMap
        if (objectMap.containsKey("tampering")) {
            Object tamperingObj = objectMap.get("tampering");
            logger.info("tamperingObj: {}", tamperingObj);  // Use {} for variable logging

            Map<String, Object> tamperingMap = (Map<String, Object>) tamperingObj;

            String isLegal = (String) tamperingMap.get("is_legal");
            List<String> warningTampering = (List<String>) tamperingMap.get("warning");
            logger.info("isLegal: {}", isLegal != null ? isLegal : "null");
            logger.info("warningTampering: {}", warningTampering != null ? warningTampering : "null");

            // Check conditions
            if (!isLegal.equalsIgnoreCase("yes") ||
                    (warningTampering != null && !warningTampering.isEmpty())) {
                logger.info("Catch error in tampering");
                return false;
            }
        } else {
            logger.info("'tampering' field not found in objectMap.");
        }

        // Zone displaying only when error -> check null
        List<String> warningMsg = (List<String>) objectMap.get("warning_msg");
        List<String> warning = (List<String>) objectMap.get("warning");
        if(warningMsg != null || warning != null) {
            if(!warningMsg.isEmpty() || !warning.isEmpty()){
                logger.info("Front-side catches error in zone displaying only when error");
                return false;
            }
        }

        return true;
    }

    // Validate the back information
    public boolean isBackIDCardValid(Map<String, Object> objectMap) {
        // Zone always displaying -> not need check null
        // 1
        String issueDate = (String) objectMap.get("issue_date");
        String features = (String) objectMap.get("features");
        List<String> generalWarning = (List<String>) objectMap.get("general_warning");
        if(issueDate == null || features == null || !generalWarning.isEmpty()){
            logger.info("Back Dang loi zone 1");
            return false;
        }

        // 2
        String backCornerWarning = (String) objectMap.get("back_corner_warning");
        String backExpireWarning = (String) objectMap.get("back_expire_warning");
        Double featuresProb = extractNumericValue(objectMap.get("features_prob"));
        if(!backCornerWarning.equalsIgnoreCase("no") ||
                backExpireWarning.equalsIgnoreCase("yes") ||
                featuresProb < 0.9){
            logger.info("Back Dang loi zone 2");
            return false;
        }

        // Zone displaying only when error -> check null
        // 3
        Boolean issueDateFakeWarning = (Boolean) objectMap.get("issuedate_fake_warning");
        String msgBack = (String) objectMap.get("msg_back");
        if(issueDateFakeWarning != null || msgBack != null){
            logger.info("issueDateFakeWarning: {}", issueDateFakeWarning);
            logger.info("msgBack: {}", msgBack);

            if(issueDateFakeWarning == true || !msgBack.equalsIgnoreCase("ok")){
                logger.info("Back-side catches error in zone 3 displaying only when error ");
                return false;
            }
        }

        // 4
        List<String> warningMsg = (List<String>) objectMap.get("warning_msg");
        List<String> warning = (List<String>) objectMap.get("warning");
        if(warningMsg != null || warning != null){
            logger.info("warningMsg: {}", warningMsg);
            logger.info("warning: {}", warning);
            if(!warningMsg.isEmpty() || !warning.isEmpty()){
                logger.info("Back-side catches error in zone 4 displaying only when error");
                return false;
            }
        }

        // Check if 'tampering' exists in the objectMap
        if (objectMap.containsKey("tampering")) {
            logger.info("Back Dang loi tampering");
            Object tamperingObj = objectMap.get("tampering");
            Map<String, Object> tamperingMap = (Map<String, Object>) tamperingObj;

            String isLegal = (String) tamperingMap.get("is_legal");
            List<String> warningTampering = (List<String>) tamperingMap.get("warning");
            logger.info("isLegal: {}", isLegal != null ? isLegal : "null");
            logger.info("warningTampering: {}", warningTampering != null ? warningTampering : "null");

            // Check conditions
            if (!isLegal.equalsIgnoreCase("yes") ||
                    (warningTampering != null && !warningTampering.isEmpty())) {
                logger.info("Catch error in tampering");
                return false;
            }
        } else {
            logger.info("'tampering' field not found in objectMap.");
        }

        return true;
    }

    // Validate comparing FACE and ID card
    public boolean isFaceAndIdCardValid(Map<String, Object> objectMap) {
        String result = (String) objectMap.get("result");
        String msg = (String) objectMap.get("msg");
        Double prob = extractNumericValue(objectMap.get("prob"));

        logger.info("result: {}", result);
        logger.info("msg: {}", msg);
        logger.info("prob: {}", prob);

        if(result.equalsIgnoreCase("Khuôn mặt không khớp")
                || !msg.equals("MATCH") || prob < 0.9) {
            return false;
        }

        return true;
    }
}