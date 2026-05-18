package com.revhive.auth.controller;


import com.revhive.auth.dto.request.ChangePasswordRequest;
import com.revhive.auth.dto.request.LoginRequest;
import com.revhive.auth.dto.request.RegisterRequest;
import com.revhive.auth.dto.response.LoginResponse;
import com.revhive.auth.dto.response.UserSearchDTO;
import com.revhive.auth.model.User;
import com.revhive.auth.repository.UserRepository;
import com.revhive.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "AUTH API", description = "User Authentication options")
public class UserController {

    private static final Logger logger =
            LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserRepository userRepository;
//    private final FollowService followService;


    // AUTH APIs


    @Operation(summary = "Register new user")
    @PostMapping("/api/auth/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        logger.info(
                "Incoming register request with email: {}",
                request.getEmail()
        );

        User user = userService.register(request);

        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Login user")
    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest loginRequest
    ) {

        try {

            LoginResponse response =
                    userService.login(loginRequest);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {

            String message =
                    e.getMessage() == null
                            ? "Login failed"
                            : e.getMessage();

            return ResponseEntity.status(401).body(
                    Map.of(
                            "error", "Unauthorized",
                            "message", message
                    )
            );
        }
    }



    @Operation(summary = "Get user profile")
    @GetMapping("/api/auth/profile/{userId}")
    public ResponseEntity<?> getUserProfile(
            @PathVariable Long userId
    ) {

        try {

            User user = userRepository.findById(userId)
                    .orElseThrow(
                            () -> new RuntimeException("User not found")
                    );

//            long followersCount =
//                    followService.getFollowersCount(userId);
//
//            long followingCount =
//                    followService.getFollowingCount(userId);

            Map<String, Object> response = new HashMap<>();

            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("bio", user.getBio());
            response.put("avatarUrl", user.getAvatarUrl());
            response.put("followersCount", 0);
            response.put("followingCount", 0);
            response.put("postsCount", 0);
            response.put("role", user.getRole().name());
            response.put("isActive", user.isActive());
            response.put("createdAt", user.getCreatedAt());

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.status(404).body(
                    Map.of(
                            "error", "User not found",
                            "message", e.getMessage()
                    )
            );
        }
    }



    @Operation(summary = "Get current logged in user")
    @GetMapping("/api/users/me")
    public ResponseEntity<?> getCurrentUser(
            Authentication authentication
    ) {

        try {

            User user = userService.getCurrentUser(
                    authentication.getName()
            );

            return ResponseEntity.ok(user);

        } catch (Exception e) {

            return ResponseEntity.status(404).body(
                    Map.of(
                            "error", "User not found",
                            "message", e.getMessage()
                    )
            );
        }
    }

    @Operation(summary = "Update profile")
    @PutMapping("/api/users/settings/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody RegisterRequest request,
            Authentication authentication
    ) {

        try {

            User updatedUser =
                    userService.updateProfile(
                            authentication.getName(),
                            request
                    );

            return ResponseEntity.ok(updatedUser);

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error", "Update failed",
                            "message", e.getMessage()
                    )
            );
        }
    }

    @Operation(summary = "Change password")
    @PutMapping("/api/users/settings/password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {

        try {

            userService.changePassword(
                    authentication.getName(),
                    request
            );

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Password updated successfully"
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error", "Password update failed",
                            "message", e.getMessage()
                    )
            );
        }
    }
    @GetMapping("/api/users/search")
    public ResponseEntity<List<UserSearchDTO>> searchUsers(
            @RequestParam String query
    ) {

        return ResponseEntity.ok(
                userService.searchUsers(query)
        );
    }
}
