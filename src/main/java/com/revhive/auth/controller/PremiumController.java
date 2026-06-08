package com.revhive.auth.controller;

import com.revhive.auth.enums.Role;
import com.revhive.auth.model.User;
import com.revhive.auth.repository.UserRepository;
import com.revhive.auth.security.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/premium")
@RequiredArgsConstructor
@Slf4j
public class PremiumController {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/upgrade")
    public ResponseEntity<?> upgradePremium(
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.substring(7);

            log.info("JWT claims before premium activation: {}", jwtUtil.extractAllClaims(token));

            String email = jwtUtil.extractUsername(token);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            log.info("User data before premium activation: username={}, email={}, role={}, premium={}",
                    user.getUsername(), user.getEmail(), user.getRole(), user.isPremium());

            user.setPremium(true);
            user.setRole(Role.PREMIUM);

            userRepository.save(user);

            log.info("User data immediately after premium activation: username={}, email={}, role={}, premium={}",
                    user.getUsername(), user.getEmail(), user.getRole(), user.isPremium());

            String newToken = jwtUtil.generateToken(user);

            log.info("JWT claims after premium activation: {}", jwtUtil.extractAllClaims(newToken));

            return ResponseEntity.ok(newToken);

        } catch (Exception e) {
            log.error("Error upgrading user to premium", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
