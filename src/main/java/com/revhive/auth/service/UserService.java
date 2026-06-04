package com.revhive.auth.service;

import com.revhive.auth.dto.request.ChangePasswordRequest;
import com.revhive.auth.dto.request.LoginRequest;
import com.revhive.auth.dto.request.RegisterRequest;
import com.revhive.auth.dto.response.LoginResponse;
import com.revhive.auth.dto.response.UserSearchDTO;
import com.revhive.auth.enums.AccountStatus;
import com.revhive.auth.enums.Role;
import com.revhive.auth.model.EmailVerificationOTP;
import com.revhive.auth.model.User;
import com.revhive.auth.repository.EmailVerificationOtpRepository;
import com.revhive.auth.repository.UserRepository;
import com.revhive.auth.security.JWTUtil;
import com.revhive.auth.util.OtpUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger =
            LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private final EmailVerificationOtpRepository otpRepository;
    private final EmailService emailService;

    public User register(RegisterRequest registerRequest) {

        logger.info("Register request received");

        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .password(
                        passwordEncoder.encode(
                                registerRequest.getPassword()
                        )
                )
                .role(Role.USER)
                .bio(registerRequest.getBio())
                .dob(registerRequest.getDob())
                .status(String.valueOf(AccountStatus.PENDING_VERIFICATION))
                .build();

        user = userRepository.save(user);

        String otp = OtpUtil.generateOtp();

        EmailVerificationOTP verificationOtp =
                EmailVerificationOTP.builder()
                        .userId(user.getId())
                        .otp(otp)
                        .expiryTime(
                                LocalDateTime.now().plusMinutes(5)
                        )
                        .verified(false)
                        .build();

        otpRepository.save(verificationOtp);

        emailService.sendOtp(user.getEmail(), otp);

        logger.info("OTP sent to {}", user.getEmail());

        return user;
    }

    public void verifyOtp(String email, String otp) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );

        EmailVerificationOTP verification =
                otpRepository.findByUserIdAndOtp(
                                user.getId(),
                                otp
                        )
                        .orElseThrow(
                                () -> new RuntimeException("Invalid OTP")
                        );

        if (verification.isVerified()) {
            throw new RuntimeException("OTP already used");
        }

        if (verification.getExpiryTime()
                .isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        verification.setVerified(true);

        user.setStatus(String.valueOf(AccountStatus.ACTIVE));

        otpRepository.save(verification);
        userRepository.save(user);
    }

    public void resendOtp(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );

        String otp = OtpUtil.generateOtp();

        EmailVerificationOTP verification =
                otpRepository.findByUserId(user.getId())
                        .orElse(
                                EmailVerificationOTP.builder()
                                        .userId(user.getId())
                                        .build()
                        );

        verification.setOtp(otp);
        verification.setVerified(false);
        verification.setExpiryTime(
                LocalDateTime.now().plusMinutes(5)
        );

        otpRepository.save(verification);

        emailService.sendOtp(email, otp);
    }

    public LoginResponse login(LoginRequest loginRequest) {

        System.out.println("Login request: "
                + loginRequest.getUserNameOrEmail());

        User user = userRepository
                .findByEmailOrUsername(
                        loginRequest.getUserNameOrEmail(),
                        loginRequest.getUserNameOrEmail()
                )
                .orElseThrow(() -> new RuntimeException("User not found"));

        System.out.println("User found: " + user.getEmail());

        System.out.println(
                "Password match: "
                        + passwordEncoder.matches(
                        loginRequest.getPassword(),
                        user.getPassword()
                )
        );

        System.out.println("Status: " + user.getStatus());

        if (!passwordEncoder.matches(
                loginRequest.getPassword(),
                user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(user);

        return LoginResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
    public User getCurrentUser(String email) {

        return userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );
    }

    public User updateProfile(
            String email,
            RegisterRequest request
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );

        user.setUsername(request.getUsername());
        user.setBio(request.getBio());
        user.setDob(request.getDob());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setSubscribeNewsletter(
                request.getSubscribeNewsletter()
        );

        return userRepository.save(user);
    }

    public void changePassword(
            String email,
            ChangePasswordRequest request
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(
                        () -> new RuntimeException("User not found")
                );

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword()
        )) {
            throw new RuntimeException("Wrong current password");
        }

        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(user);
    }

    public List<UserSearchDTO> searchUsers(String query) {

        List<User> users =
                userRepository
                        .findTop10ByUsernameContainingIgnoreCase(query);

        return users.stream()
                .map(user -> new UserSearchDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getAvatarUrl()
                ))
                .toList();
    }

}
