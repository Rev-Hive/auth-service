package com.revhive.auth.controller;


import com.revhive.auth.model.User;
import com.revhive.auth.repository.UserRepository;
import com.revhive.auth.security.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/premium")
@RequiredArgsConstructor
public class PremiumController {

    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/upgrade")
    public ResponseEntity<?> upgradePremium(
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.substring(7);

            String email = jwtUtil.extractUsername(token);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setPremium(true);

            userRepository.save(user);

            String newToken = jwtUtil.generateToken(user);

            return ResponseEntity.ok(newToken);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}
