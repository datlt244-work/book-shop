package com.ecommerce.user.repository;

import com.ecommerce.user.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {

    List<UserAddress> findByUserProfileUserId(UUID userId);

    Optional<UserAddress> findByIdAndUserProfileUserId(UUID id, UUID userId);

    Optional<UserAddress> findByUserProfileUserIdAndIsDefaultTrue(UUID userId);

    long countByUserProfileUserId(UUID userId);

    @Modifying
    @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.userProfile.userId = :userId AND a.id != :addressId")
    void clearOtherDefaultAddresses(UUID userId, UUID addressId);

    void deleteByUserProfileUserId(UUID userId);
}

