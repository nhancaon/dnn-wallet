package com.example.onlinebankingapp.services.Bank;

import com.example.onlinebankingapp.dtos.requests.BankRequest;
import com.example.onlinebankingapp.entities.BankEntity;
import com.example.onlinebankingapp.exceptions.AppException;
import com.example.onlinebankingapp.exceptions.ErrorCode;
import com.example.onlinebankingapp.repositories.BankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BankServiceImpl implements BankService {
    private final BankRepository bankRepository;

    @Override
    public BankEntity insertBank(BankRequest bankRequest) {
        BankEntity newBankEntity = BankEntity.builder()
                .name(bankRequest.getName())
                .build();
        return bankRepository.save(newBankEntity);
    }

    @Override
    public List<BankEntity> getAllBanks() {
        return bankRepository.findAll();
    }

    @Override
    public BankEntity getBankByName(String name) {
        if(name == null || name.isEmpty()){
            throw new AppException(ErrorCode.BANK_NAME_NOT_FOUND);
        }

        //check if the query reward exists, yes then return the data, else throw error
        BankEntity bankEntity = bankRepository.getBankByName(name);
        if(bankEntity != null) {
            return bankEntity;
        }
        throw new AppException(ErrorCode.BANK_NAME_NOT_FOUND);
    }

    @Override
    public BankEntity updateBankName(String oldBankName, BankRequest bankRequest) {
        BankEntity updatedBankEntity = getBankByName(oldBankName);
        updatedBankEntity.setName(bankRequest.getName());
        return bankRepository.save(updatedBankEntity);
    }

    @Override
    public void deleteBankByName(String name) {
        BankEntity deletedBankEntity = getBankByName(name);
        bankRepository.delete(deletedBankEntity);
    }
}
