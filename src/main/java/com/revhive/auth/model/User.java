package com.revhive.auth.model;

import com.revhive.auth.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false,unique = true)
    private String username;

    @Column(nullable = false,unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String bio;
    private String avatarUrl;



    @Builder.Default
    private int followersCount=0;
    @Builder.Default
    private int followingCount=0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private boolean isActive=true;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    private LocalDateTime lastActive;

    private String resetToken;
    private  LocalDateTime tokenExpiry;

    private boolean premium;


    @Past(message = "DOB must be in the past")
    @Column(nullable = false)
    private LocalDate dob;

    private LocalDate premiumExpiry;
    @PrePersist
    protected void onCreate()
    {
        this.createdAt=LocalDateTime.now();
        this.updatedAt=LocalDateTime.now();
    }


    private Boolean subscribeNewsletter;

    private String status;

    @PreUpdate
    protected void onUpdate()
    {
        this.updatedAt=LocalDateTime.now();
    }

//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private Status status;

}
