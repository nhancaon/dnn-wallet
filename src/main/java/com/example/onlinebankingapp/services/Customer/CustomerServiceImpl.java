package com.example.onlinebankingapp.services.Customer;

import com.example.onlinebankingapp.components.JwtTokenUtils;
import com.example.onlinebankingapp.dtos.requests.Customer.ChangePasswordCustomerRequest;
import com.example.onlinebankingapp.dtos.requests.Customer.CustomerRequest;
import com.example.onlinebankingapp.dtos.requests.LoginRequest;
import com.example.onlinebankingapp.dtos.responses.Customer.CustomerListResponse;
import com.example.onlinebankingapp.dtos.responses.Customer.CustomerResponse;
import com.example.onlinebankingapp.entities.CustomerEntity;
import com.example.onlinebankingapp.entities.TokenCustomerEntity;
import com.example.onlinebankingapp.exceptions.*;
import com.example.onlinebankingapp.repositories.CustomerRepository;
import com.example.onlinebankingapp.repositories.TokenCustomerRepository;
import com.example.onlinebankingapp.dtos.responses.IDCardExtraction.IDCardExtractionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.example.onlinebankingapp.utils.ValidationUtils.*;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtils jwtTokenUtil;
    private final TokenCustomerRepository tokenCustomerRepository;

    // Implementation of the login functionality
    @Override
    public String login(
            LoginRequest loginRequest
    ) {
        Optional <CustomerEntity> optionalCustomer = Optional.empty();
        String subject = null;

        // Check if the email is in a valid format
        if (!isValidEmail(loginRequest.getEmail())){
            throw new AppException(ErrorCode.EMAIL_INVALID);
        }

        // Retrieve customer by email if available
        if (optionalCustomer.isEmpty() && loginRequest.getEmail() != null) {
            optionalCustomer = customerRepository.findByEmail(loginRequest.getEmail());
            subject = loginRequest.getEmail();
        }

        // Check if the customer exists and the password matches
        CustomerEntity existingUser = optionalCustomer.get();

        // Check if customer account IS_ACTIVE to login or not
        if(!existingUser.isActive()) {
            throw new DataIntegrityViolationException("Customer account is inactive. Cannot login");
        }

        if(!passwordEncoder.matches(loginRequest.getPassword(), existingUser.getPassword())) {
            throw new AppException(ErrorCode.PASSWORD_INCORRECT);
        }
        // Authenticate user and generate JWT token
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                subject, loginRequest.getPassword(),
                existingUser.getAuthorities()
        );

        authenticationManager.authenticate(authenticationToken);
        return jwtTokenUtil.generateTokenForCustomer(existingUser);
    }

    @Override
    public String loginFaceId(
            String email
    ) {
        // Retrieve customer by email
        Optional<CustomerEntity> optionalCustomer = customerRepository.findByEmail(email);
        if (optionalCustomer.isEmpty()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        CustomerEntity existingCustomer = optionalCustomer.get();

        // Check if customer account is active
        if (!existingCustomer.isActive()) {
            throw new DataIntegrityViolationException("Customer account is inactive. Cannot login.");
        }

        // Since FaceID was successful, generate JWT directly
        return jwtTokenUtil.generateTokenForCustomer(existingCustomer);
    }

    // Implementation of inserting a new customer + registering a new account
    // However, cannot log in because field is_active is false
    // Must verify phone number SMS OTP to enable account to log in
    @Override
    public CustomerEntity insertCustomer(
            CustomerRequest customerRequest
    ) {
        String phoneNumber = customerRequest.getPhoneNumber();
        String email = customerRequest.getEmail();
        String password = customerRequest.getPassword();

        // Validate phoneNumber format
        if (!isValidPhoneNumber(phoneNumber)){
            throw new AppException(ErrorCode.PHONE_NUMBER_INVALID);
        }

        // Validate email format
        if (!isValidEmail(email)){
            throw new AppException(ErrorCode.EMAIL_INVALID);
        }

        // Validate password format
        if(!isValidPassword(password)) {
            throw new AppException(ErrorCode.PASSWORD_INVALID);
        };

        // Encode password
        String encodedPassword = passwordEncoder.encode(password);

        // Check for existing customers with the same email, and phone number
        if (customerRepository.existsByEmail(email)){
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        }
        if(customerRepository.existsByPhoneNumber(phoneNumber)){
            throw new AppException(ErrorCode.PHONE_NUMBER_EXISTS);
        }

        // Find the customer by citizenId
        Optional<CustomerEntity> optionalCustomer = customerRepository.findByCitizenId(customerRequest.getCitizenId());
        // Check if the customer exists
        if (optionalCustomer.isPresent()) {
            // Get the existing customer entity (add by extract ID)
            CustomerEntity completeCustomerInfo = optionalCustomer.get();

            if(!completeCustomerInfo.getPhoneNumber().isEmpty() ||
                    !completeCustomerInfo.getPhoneNumber().isBlank()){
                throw new DataIntegrityViolationException("Goi nham API roi");
            }

            completeCustomerInfo.setPhoneNumber(phoneNumber);
            completeCustomerInfo.setEmail(email);
            completeCustomerInfo.setPassword(encodedPassword);
            completeCustomerInfo.setPinNumber(customerRequest.getPinNumber());

            return customerRepository.save(completeCustomerInfo);
        } else {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
    }

    // Method to save FRONT ID card extraction response
    public void saveCustomerFrontIDCardExtraction(
            IDCardExtractionResponse idCardResponse,
            String imgFront,
            String clientSession
    ) {
        Map<String, Object> objectMap = idCardResponse.getObject();

        // Extract relevant fields from the response
        String name = (String) objectMap.get("name");
        String citizenId = (String) objectMap.get("id");

        // Replace next line with commas
        String address = (String) objectMap.get("recent_location");
        String formattedAddress = address.replace("\n", ", ");

        String birthDayFromResponse = (String) objectMap.get("birth_day");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Date convertedBirthDay = null;
        try {
            convertedBirthDay = dateFormat.parse(birthDayFromResponse);
        } catch (ParseException e) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        java.sql.Date dateOfBirth = new java.sql.Date(convertedBirthDay.getTime());

        // Bypass null check
        String empty = "";

        if (customerRepository.existsByCitizenId(citizenId)) {
            // Check 2 cases
            Optional<CustomerEntity> optionalCustomer = customerRepository.findByCitizenId(citizenId);
            CustomerEntity existedCustomerCheckFullInfo = optionalCustomer.get();
            // Case 1: (Catch) Send existed ID card (existedCustomer) -> error exist customer full info
            if(!existedCustomerCheckFullInfo.getPhoneNumber().isBlank() ||
                    !existedCustomerCheckFullInfo.getPhoneNumber().isEmpty()){
                throw new AppException(ErrorCode.USER_EXISTS);
            }
            // Case 2: (Bypass) In sign-up process, exit app -> citizenId saved, phoneNumber blank
            else {
                // Just use for checking
                System.out.println("Existed but not complete customer return signup!");
            }
        }
        else {
            // Create a new customer entity from ID card
            CustomerEntity newCustomer = CustomerEntity.builder()
                    .name(capitalizeFirstLetterOfEachWord(name))
                    .citizenId(citizenId)
                    .address(formattedAddress)
                    .dateOfBirth(dateOfBirth)
                    .phoneNumber(empty)
                    .email(empty)
                    .password(empty)
                    .pinNumber(empty)
                    .imgFront(imgFront)
                    .imgBack(empty)
                    .imgFace(empty)
                    .clientSession(clientSession)
                    .build();

            // Save the new customer entity in the repository
            customerRepository.save(newCustomer);
        }
    }

    // Method to save BACK ID card extraction response
    public void saveCustomerBackIDCardExtraction(
            IDCardExtractionResponse idCardResponse,
            String imgBack
    ) {
        Map<String, Object> objectMap = idCardResponse.getObject();
        // Extract relevant field from the response
        List<String> mrz = (List<String>) objectMap.get("mrz");
        String citizenIdToCompare = extractCitizenIdFromMRZ(mrz);

        Optional<CustomerEntity> optionalCustomer = customerRepository.findByCitizenId(citizenIdToCompare);
        if(optionalCustomer.isPresent()){
            CustomerEntity emptyImageBackCustomer = optionalCustomer.get();
            if(emptyImageBackCustomer.getImgBack() == null || emptyImageBackCustomer.getImgBack().isEmpty()
                    || emptyImageBackCustomer.getImgBack().isBlank()){
                emptyImageBackCustomer.setImgBack(imgBack);
                customerRepository.save(emptyImageBackCustomer);
            }
        }
        else {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
    }

    // extractCitizenIdFromMRZ: Use to compare front and back side owned by same person
    private String extractCitizenIdFromMRZ(List<String> mrz) {
        // Check if the MRZ list is not empty
        if (mrz == null || mrz.isEmpty()) {
            return null; // or throw an exception, depending on your error handling preference
        }

        // Get the first element from the MRZ list
        String firstLine = mrz.get(0);

        // Check if the firstLine is long enough and contains the expected pattern
        if (firstLine.length() >= 12 && firstLine.contains("<<")) {
            // Find the index of "<<0"
            int index = firstLine.indexOf("<<");
            // Extract the 12 characters before "<<"
            return firstLine.substring(index - 12, index);
        }

        return null;
    }

    // Method to save FACE has code
    public void saveCustomerFace(
            String imgFace,
            String clientSession
    ) {
        Optional<CustomerEntity> optionalCustomer = customerRepository.findByClientSession(clientSession);
        if(optionalCustomer.isPresent()) {
            CustomerEntity emptyImageFaceCustomer = optionalCustomer.get();
            if (emptyImageFaceCustomer.getPhoneNumber() == null
                    || emptyImageFaceCustomer.getPhoneNumber().isEmpty()
                    || emptyImageFaceCustomer.getPhoneNumber().isBlank()) {
                emptyImageFaceCustomer.setImgFace(imgFace);
                customerRepository.save(emptyImageFaceCustomer);
            } else {
                throw new AppException(ErrorCode.FACE_NOT_FOUND);
            }
        }
    }

    // Phone number SMS OTP has been verified to enable account
    @Override
    public CustomerEntity activeCustomerAccount(CustomerRequest customerRequest) {
        // Validate the phone number
        if (!isValidPhoneNumber(customerRequest.getPhoneNumber())) {
            throw new AppException(ErrorCode.PHONE_NUMBER_INVALID);
        }

        // Find the customer by phone number
        Optional<CustomerEntity> optionalCustomer = customerRepository
                .findByPhoneNumber(customerRequest.getPhoneNumber());
        // Check if the customer exists
        if (optionalCustomer.isPresent()) {
            // Get the existing customer entity
            CustomerEntity customerEntity = optionalCustomer.get();
            customerEntity.setActive(true);

            return customerRepository.save(customerEntity);
        } else {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
    }

    // Get customer details from token
    @Override
    public CustomerEntity getCustomerDetailsFromToken(String token) {
        // Check if the token has expired
        if(jwtTokenUtil.isTokenExpired(token)) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        // Extract the subject (phone number or email) from the token
        String subject = jwtTokenUtil.getSubject(token);
        Optional<CustomerEntity> customer;

        // Try to find the customer by phone number
        customer = customerRepository.findByPhoneNumber(subject);

        // If customer not found by phone number and subject is a valid email, try finding by email
        if (customer.isEmpty() && isValidEmail(subject)) {
            customer = customerRepository.findByEmail(subject);

        }
        // Return the customer if found, otherwise throw an exception
        return customer.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    // Get customer details from refresh token
    @Override
    public CustomerEntity getCustomerDetailsFromRefreshToken(String refreshToken) {
        // logger.debug("Searching for refresh token: {}", refreshToken);

        // Find the token entity corresponding to the refresh token
        TokenCustomerEntity existingToken = tokenCustomerRepository.findByRefreshToken(refreshToken);

        // If refresh token not found, throw an exception
        if (existingToken == null) {
            // logger.error("Refresh token not found: {}", refreshToken);
            throw new AppException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        // logger.debug("Token found: {}", existingToken.getToken());

        // Retrieve customer details using the token associated with the refresh token
        return getCustomerDetailsFromToken(existingToken.getToken());
    }

    // Retrieve all customers from the repository
    @Override
    public List<CustomerEntity> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Override
    public CustomerListResponse getPaginationListCustomer(
            Integer page, Integer size, String orderedBy, Boolean isAscending, String keyword
    ) {
        Long totalQuantity;
        Page<CustomerEntity> customerPage;

        // Get ascending or descending sort
        Sort sort = Boolean.TRUE.equals(isAscending)
                ? Sort.by(orderedBy).ascending()
                : Sort.by(orderedBy).descending();

        try {
            customerPage = customerRepository.findByNameContainingIgnoreCase(
                    keyword, PageRequest.of(page - 1, size, sort));
            totalQuantity = customerPage.getTotalElements();
        }
        catch (Exception e){
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        List<CustomerResponse> customerResponses = customerPage.stream()
                .map(CustomerResponse::fromCustomerResponse)
                .toList();

        return CustomerListResponse.builder()
                .customerResponses(customerResponses)
                .totalQuantity(totalQuantity)
                .build();
    }

    // Get customer by id
    @Override
    public CustomerEntity getCustomerById(long id) {
        // Find customer by ID
        Optional<CustomerEntity> optionalCustomer = customerRepository.findById(id);

        // If customer exists, return it, otherwise throw an exception
        if(optionalCustomer.isPresent()) {
            return optionalCustomer.get();
        }
        throw new AppException(ErrorCode.USER_NOT_FOUND);
    }

    // Get customer by client session
    @Override
    public CustomerEntity getCustomerByClientSession(
            String clientSession
    ) {
        // Find customer by client session
        Optional<CustomerEntity> optionalCustomer = customerRepository.findByClientSession(clientSession);

        // If customer exists, return it, otherwise throw an exception
        if(optionalCustomer.isPresent()) {
            return optionalCustomer.get();
        }
        throw new AppException(ErrorCode.USER_NOT_FOUND);
    }

    // Change user password
    @Override
    public void changePassword(
            long customerId,
            ChangePasswordCustomerRequest customerRequest
    ) {
        // Retrieve existing customer by ID, throw an exception if not found
        Optional<CustomerEntity> existingCustomer = Optional.ofNullable(customerRepository.findById(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)));

        CustomerEntity customerChangePassword = existingCustomer.get();

        // Check if the current password matches
        if (!passwordEncoder.matches(customerRequest.getPassword(), customerChangePassword.getPassword())){
            throw new AppException(ErrorCode.PASSWORD_INCORRECT);
        }

        // Check if the new password matches the confirmed password and meets criteria
        if (!(customerRequest.getNewPassword().matches(customerRequest.getConfirmPassword()))) {
            throw new AppException(ErrorCode.PASSWORD_INCORRECT);
        }

        if(!isValidPassword(customerRequest.getNewPassword())) {
            throw new AppException(ErrorCode.PASSWORD_INVALID);
        };

        // Encode and set the new password
        customerChangePassword.setPassword(passwordEncoder.encode(customerRequest.getNewPassword()));

        // Save the updated customer
        customerRepository.save(customerChangePassword);

        // Delete all tokens associated with this customer
        List<TokenCustomerEntity> tokens = tokenCustomerRepository.findByCustomer(customerChangePassword);
        for (TokenCustomerEntity token : tokens) {
            tokenCustomerRepository.delete(token);
        }
    }

    // Only update address, email (check valid -> send OTP email to authenticate), pin_number
    @Override
    public CustomerEntity updateCustomerProfile(
            long customerId,
            CustomerRequest customerRequest
    ) {
        String email = customerRequest.getEmail();
        String pinNumber = customerRequest.getPinNumber();

        if (!isValidEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_INVALID);
        }

        if (pinNumber.length() != 6) {
            throw new AppException(ErrorCode.PIN_NUMBER_INVALID);
        }

        CustomerEntity updatedCustomerEntity = getCustomerById(customerId);
        updatedCustomerEntity.setEmail(email);
        updatedCustomerEntity.setPinNumber(pinNumber);
        return customerRepository.save(updatedCustomerEntity);
    }

    // Method to update customer profile by admin/ staff
    @Override
    public CustomerEntity updateCustomerByEmployee(
            long customerId,
            CustomerRequest customerRequest
    ) {
        String address = customerRequest.getAddress();
        String phoneNumber = customerRequest.getPhoneNumber();
        String pinNumber = customerRequest.getPinNumber();
        String email = customerRequest.getEmail();

        CustomerEntity updatedCustomerEntity = getCustomerById(customerId);

        if(!isValidPhoneNumber(phoneNumber)){
            throw new AppException(ErrorCode.PHONE_NUMBER_INVALID);
        }
        else if(customerRepository.existsByPhoneNumber(phoneNumber)
                && !customerRequest.getCitizenId().equals(updatedCustomerEntity.getCitizenId())){
            throw new AppException(ErrorCode.PHONE_NUMBER_EXISTS);
        }

        if (address.length() > 100 || address.length() < 5) {
            throw new AppException(ErrorCode.ADDRESS_INVALID);
        }

        if (!isValidEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_INVALID);
        }

        if (pinNumber.length() != 6) {
            throw new AppException(ErrorCode.PIN_NUMBER_INVALID);
        }


        updatedCustomerEntity.setActive(customerRequest.isActive());
        updatedCustomerEntity.setPhoneNumber(customerRequest.getPhoneNumber());
        updatedCustomerEntity.setAddress(customerRequest.getAddress());
        updatedCustomerEntity.setEmail(customerRequest.getEmail());
        updatedCustomerEntity.setPinNumber(customerRequest.getPinNumber());
        // Encode and set the new password
        if(customerRequest.getPassword() != null) {
            updatedCustomerEntity.setPassword(passwordEncoder.encode(customerRequest.getPassword()));
        }

        return customerRepository.save(updatedCustomerEntity);
    }

    // Do not hard-delete (permanent) -> soft-delete (inactive the account)
    // Only employee can use this
    @Override
    public CustomerEntity deleteCustomer(
            long customerId
    ) {
        CustomerEntity deleteCustomerEntity = getCustomerById(customerId);
        deleteCustomerEntity.setActive(false);
        return customerRepository.save(deleteCustomerEntity);
    }

    public static String capitalizeFirstLetterOfEachWord(String str) {
        String[] words = str.split(" ");
        StringBuilder capitalizedWords = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                // Capitalize the first letter and append the rest of the word
                String capitalizedWord = word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
                capitalizedWords.append(capitalizedWord).append(" ");
            }
        }

        return capitalizedWords.toString().trim(); // Remove the trailing space
    }

    public boolean verifyPinNumber(long customerId, String pinNumber){
        CustomerEntity customer = getCustomerById(customerId);
        return customer.getPinNumber().equals(pinNumber);
    }
}
