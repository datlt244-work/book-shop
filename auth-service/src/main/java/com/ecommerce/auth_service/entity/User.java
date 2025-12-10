package com.ecommerce.auth_service.entity;

import com.ecommerce.common.entity.JpaBaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User extends JpaBaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name = "username", unique = true, columnDefinition = "VARCHAR(255) COLLATE \"C\"")
    private String username;

    private String password;

    private String firstName;
    private String lastName;
    private LocalDate dob;

    private String roles;
}
