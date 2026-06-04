package com.revhive.auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_otp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationOTP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String otp;

    private LocalDateTime expiryTime;

    private boolean verified;
}
