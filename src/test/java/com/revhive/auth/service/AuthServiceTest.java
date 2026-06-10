package com.revhive.auth.service;

import com.revhive.auth.model.User;
import com.revhive.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    // --- forgotPassword Tests ---

    @Test
    @DisplayName("ForgotPassword should set reset token and send email when user exists")
    public void forgotPassword_WhenUserExists_ShouldSetResetTokenAndSendEmail() {
        // Arrange
        String email = "user@example.com";
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        authService.forgotPassword(email);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(captor.capture());
        
        User savedUser = captor.getValue();
        assertThat(savedUser.getResetToken()).isNotNull();
        assertThat(savedUser.getTokenExpiry()).isAfter(LocalDateTime.now());
        
        verify(emailService, times(1)).sendResetEmail(eq(email), eq(savedUser.getResetToken()));
        verifyNoMoreInteractions(userRepository, emailService);
    }

    @Test
    @DisplayName("ForgotPassword should throw RuntimeException when user does not exist")
    public void forgotPassword_WhenUserDoesNotExist_ShouldThrowRuntimeException() {
        // Arrange
        String email = "unknown@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.forgotPassword(email))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(userRepository, times(1)).findByEmail(email);
        verifyNoMoreInteractions(userRepository, emailService);
    }

    // --- verifyResetToken Tests ---

    @Test
    @DisplayName("VerifyResetToken should complete successfully when token is valid and not expired")
    public void verifyResetToken_WhenTokenIsValid_ShouldCompleteSuccessfully() {
        // Arrange
        String token = "valid-token";
        User user = new User();
        user.setResetToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByResetToken(token)).thenReturn(Optional.of(user));

        // Act & Assert (should not throw exception)
        authService.verifyResetToken(token);

        verify(userRepository, times(1)).findByResetToken(token);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("VerifyResetToken should throw RuntimeException when token is invalid")
    public void verifyResetToken_WhenTokenIsInvalid_ShouldThrowRuntimeException() {
        // Arrange
        String token = "invalid-token";
        when(userRepository.findByResetToken(token)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.verifyResetToken(token))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid OTP");

        verify(userRepository, times(1)).findByResetToken(token);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("VerifyResetToken should throw RuntimeException when token has expired")
    public void verifyResetToken_WhenTokenIsExpired_ShouldThrowRuntimeException() {
        // Arrange
        String token = "expired-token";
        User user = new User();
        user.setResetToken(token);
        user.setTokenExpiry(LocalDateTime.now().minusMinutes(1)); // Expired

        when(userRepository.findByResetToken(token)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> authService.verifyResetToken(token))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("OTP expired");

        verify(userRepository, times(1)).findByResetToken(token);
        verifyNoMoreInteractions(userRepository);
    }

    // --- resetPassword Tests ---

    @Test
    @DisplayName("ResetPassword should update user password and clear token when token is valid")
    public void resetPassword_WhenTokenIsValid_ShouldUpdatePasswordAndClearToken() {
        // Arrange
        String token = "valid-token";
        String newPassword = "newSecurePassword";
        String encodedPassword = "encodedPassword";

        User user = new User();
        user.setResetToken(token);
        user.setTokenExpiry(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByResetToken(token)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        authService.resetPassword(token, newPassword);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(captor.capture());

        User savedUser = captor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
        assertThat(savedUser.getResetToken()).isNull();
        assertThat(savedUser.getTokenExpiry()).isNull();

        verify(passwordEncoder, times(1)).encode(newPassword);
        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("ResetPassword should throw RuntimeException when token is invalid")
    public void resetPassword_WhenTokenIsInvalid_ShouldThrowRuntimeException() {
        // Arrange
        String token = "invalid-token";
        when(userRepository.findByResetToken(token)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.resetPassword(token, "pass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid token");

        verify(userRepository, times(1)).findByResetToken(token);
        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("ResetPassword should throw RuntimeException when token is expired")
    public void resetPassword_WhenTokenIsExpired_ShouldThrowRuntimeException() {
        // Arrange
        String token = "expired-token";
        User user = new User();
        user.setResetToken(token);
        user.setTokenExpiry(LocalDateTime.now().minusMinutes(5));

        when(userRepository.findByResetToken(token)).thenReturn(Optional.of(user));

        // Act & Assert
        assertThatThrownBy(() -> authService.resetPassword(token, "pass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Token expired");

        verify(userRepository, times(1)).findByResetToken(token);
        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }
}
