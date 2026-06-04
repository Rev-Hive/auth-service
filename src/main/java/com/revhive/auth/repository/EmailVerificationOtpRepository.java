package com.revhive.auth.repository;

import com.revhive.auth.model.EmailVerificationOTP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationOtpRepository
        extends JpaRepository<EmailVerificationOTP, Long> {

    Optional<EmailVerificationOTP> findByUserId(Long userId);

    Optional<EmailVerificationOTP> findByUserIdAndOtp(Long userId, String otp);
}
