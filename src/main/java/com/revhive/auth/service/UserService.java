package com.revhive.auth.service;



import com.revhive.auth.dto.request.ChangePasswordRequest;
import com.revhive.auth.dto.request.LoginRequest;
import com.revhive.auth.dto.request.RegisterRequest;
import com.revhive.auth.dto.response.LoginResponse;
import com.revhive.auth.dto.response.UserSearchDTO;
import com.revhive.auth.enums.Role;
import com.revhive.auth.model.User;
import com.revhive.auth.repository.UserRepository;
import com.revhive.auth.security.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Logger logger=  LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil JWTUtil;

  public User register(RegisterRequest registerRequest)
  {

      logger.info("Register request received for email: {} ");
      if(userRepository.findByEmail(registerRequest.getEmail()).isPresent())
      {
          logger.warn("Email already exists");
          throw new RuntimeException("Email already exists");
      }
      if(userRepository.findByUsername(registerRequest.getUsername()).isPresent())
      {
          logger.warn("Username already exists");
          throw new RuntimeException("User name is already present");
      }

      User user=User.builder().username(registerRequest.getUsername())
              .email(registerRequest.getEmail())
              .password(passwordEncoder.encode(registerRequest.getPassword()))
              .role(Role.USER)
              .bio(registerRequest.getBio())
              .dob(registerRequest.getDob())
//              .avatarUrl(registerRequest.getAvatarUrl())
              .build();

      logger.info("User successfully registered");

      return userRepository.save(user);

  }


    public LoginResponse login(LoginRequest loginRequest) {
        User user = userRepository
                .findByEmailOrUsername(
                        loginRequest.getUserNameOrEmail(),
                        loginRequest.getUserNameOrEmail()
                )
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token;
        try {
            token = JWTUtil.generateToken(user);
        } catch (Exception e) {
            throw new RuntimeException("Token generation failed: " + e.getMessage());
        }

        return LoginResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public User getCurrentUser(String email)
    {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }


    public User updateProfile(
            String email,
            RegisterRequest request
    )
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
    )
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if(!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword()
        ))
        {
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
                userRepository.findTop10ByUsernameContainingIgnoreCase(query);

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
