package com.example.onlinebankingapp.repositories;

import com.example.onlinebankingapp.entities.BankEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankRepository extends JpaRepository<BankEntity, Long> {
    @Query("SELECT b FROM BankEntity b WHERE b.name = :name")
    BankEntity getBankByName(String name);

    Optional<BankEntity> findByName(String bankName);
}
