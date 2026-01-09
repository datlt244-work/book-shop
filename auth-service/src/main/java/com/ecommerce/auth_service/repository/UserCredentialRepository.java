package com.ecommerce.auth_service.repository;

import com.ecommerce.auth_service.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {
    
    boolean existsByEmail(String email);

    Optional<UserCredential> findByEmail(String email);
}

