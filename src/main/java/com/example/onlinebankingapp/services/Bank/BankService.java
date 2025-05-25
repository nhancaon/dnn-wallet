package com.example.onlinebankingapp.services.Bank;

import com.example.onlinebankingapp.dtos.requests.BankRequest;
import com.example.onlinebankingapp.entities.BankEntity;

import java.util.List;

public interface BankService {
    BankEntity insertBank(BankRequest bankRequest);
    List<BankEntity> getAllBanks();
    BankEntity getBankByName(String name) ;
    BankEntity updateBankName(String oldBankName, BankRequest bankRequest);
    void deleteBankByName(String name);
}
