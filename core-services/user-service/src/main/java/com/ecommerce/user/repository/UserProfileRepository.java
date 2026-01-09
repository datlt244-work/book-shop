package com.ecommerce.user.repository;

import com.ecommerce.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserProfile u LEFT JOIN FETCH u.addresses WHERE u.userId = :userId")
    Optional<UserProfile> findByIdWithAddresses(UUID userId);

    @Query("SELECT u FROM UserProfile u LEFT JOIN FETCH u.preferences WHERE u.userId = :userId")
    Optional<UserProfile> findByIdWithPreferences(UUID userId);
}

