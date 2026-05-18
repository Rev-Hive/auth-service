package com.revhive.auth.controller;


import com.revhive.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgot(
            @RequestParam String email
    ) {

        authService.forgotPassword(email);

        return ResponseEntity.ok(
                "Reset link sent to email"
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> reset(
            @RequestParam String token,
            @RequestParam String newPassword
    ) {

        authService.resetPassword(
                token,
                newPassword
        );

        return ResponseEntity.ok(
                "Password updated"
        );
    }
}
