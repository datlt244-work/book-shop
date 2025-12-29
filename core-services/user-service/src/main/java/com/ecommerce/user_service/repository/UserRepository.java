package com.ecommerce.user_service.repository;

import com.ecommerce.user_service.entity.User;
import com.ecommerce.user_service.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
    
    List<User> findByStatus(UserStatus status);
    
    @Query("SELECT u FROM User u WHERE u.emailVerified = false AND u.status = 'pending_verification'")
    List<User> findUnverifiedUsers();
}
