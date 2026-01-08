package com.ecommerce.auth_service.entity;

import com.ecommerce.common.entity.JpaBaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User credential entity - stores authentication data only.
 * Profile data is stored in user-service's UserProfile entity.
 * 
 * The UUID id is shared with user-service for linking.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_credentials", indexes = {
        @Index(name = "idx_user_credentials_email", columnList = "email"),
        @Index(name = "idx_user_credentials_role", columnList = "role"),
        @Index(name = "idx_user_credentials_status", columnList = "status"),
        @Index(name = "idx_user_credentials_created_at", columnList = "created_at")
})
public class UserCredential extends JpaBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Authentication
    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // Authorization
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "role", columnDefinition = "user_role DEFAULT 'customer'")
    @Builder.Default
    private UserRole role = UserRole.customer;

    // Account Status
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", columnDefinition = "user_status DEFAULT 'pending_verification'")
    @Builder.Default
    private UserStatus status = UserStatus.pending_verification;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    // Login Tracking
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "login_count", nullable = false)
    @Builder.Default
    private Integer loginCount = 0;
}

