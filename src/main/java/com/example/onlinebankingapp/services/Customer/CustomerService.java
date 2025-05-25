package com.example.onlinebankingapp.services.Customer;

import com.example.onlinebankingapp.dtos.requests.Customer.CustomerRequest;
import com.example.onlinebankingapp.dtos.requests.LoginRequest;
import com.example.onlinebankingapp.dtos.responses.Customer.CustomerListResponse;
import com.example.onlinebankingapp.entities.CustomerEntity;
import com.example.onlinebankingapp.dtos.requests.Customer.ChangePasswordCustomerRequest;

import java.util.List;

public interface CustomerService {
    String login(LoginRequest loginRequest);
    String loginFaceId(String email);
    CustomerEntity insertCustomer(CustomerRequest customerRequest);
    CustomerEntity activeCustomerAccount(CustomerRequest customerRequest);
    CustomerEntity getCustomerDetailsFromToken(String token);
    CustomerEntity getCustomerDetailsFromRefreshToken(String token);
    List<CustomerEntity> getAllCustomers();
    CustomerListResponse getPaginationListCustomer(Integer page, Integer size, String orderedBy, Boolean isAscending, String keyword);
    CustomerEntity getCustomerById(long id);
    CustomerEntity getCustomerByClientSession(String clientSession);
    void changePassword(long customerId, ChangePasswordCustomerRequest customerRequest);
    CustomerEntity updateCustomerProfile(long customerId, CustomerRequest customerRequest);
    CustomerEntity updateCustomerByEmployee(long customerId, CustomerRequest customerRequest);
    CustomerEntity deleteCustomer(long customerId);
    boolean verifyPinNumber(long customerId, String pinNumber);
}
