package com.example.onlinebankingapp.repositories;

import com.example.onlinebankingapp.entities.CustomerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface CustomerRepository extends JpaRepository<CustomerEntity, Long> {
    boolean existsByEmail(String email);
    boolean existsByCitizenId(String citizenId);
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<CustomerEntity> findByCitizenId(String citizenId);
    Optional<CustomerEntity> findByPhoneNumber(String phoneNumber);
    Optional<CustomerEntity> findByEmail(String email);
    Optional<CustomerEntity> findByClientSession(String clientSession);
    Page<CustomerEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
