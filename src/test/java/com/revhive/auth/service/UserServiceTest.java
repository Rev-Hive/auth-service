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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private EmailVerificationOtpRepository otpRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    // --- register Tests ---

    @Test
    @DisplayName("Register should save user and generate/send OTP on success")
    public void register_OnSuccess_ShouldSaveUserAndSendOtp() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john_doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setBio("Hello, I am John.");
        request.setDob(LocalDate.of(2000, 1, 1));

        User savedUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("encoded_pass")
                .role(Role.USER)
                .status(AccountStatus.PENDING_VERIFICATION.name())
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(otpRepository.save(any(EmailVerificationOTP.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        User result = userService.register(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getRole()).isEqualTo(Role.USER);
        assertThat(result.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION.name());

        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verify(userRepository, times(1)).findByUsername(request.getUsername());
        verify(passwordEncoder, times(1)).encode(request.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
        verify(otpRepository, times(1)).save(any(EmailVerificationOTP.class));
        verify(emailService, times(1)).sendOtp(eq("john@example.com"), anyString());
    }

    @Test
    @DisplayName("Register should throw RuntimeException when email already exists")
    public void register_WhenEmailExists_ShouldThrowException() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("duplicate@example.com");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(new User()));

        // Act & Assert
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already exists");

        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verifyNoMoreInteractions(userRepository, passwordEncoder, otpRepository, emailService);
    }

    @Test
    @DisplayName("Register should throw RuntimeException when username already exists")
    public void register_WhenUsernameExists_ShouldThrowException() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("john@example.com");
        request.setUsername("duplicate_user");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.of(new User()));

        // Act & Assert
        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Username already exists");

        verify(userRepository, times(1)).findByEmail(request.getEmail());
        verify(userRepository, times(1)).findByUsername(request.getUsername());
        verifyNoMoreInteractions(userRepository, passwordEncoder, otpRepository, emailService);
    }

    // --- verifyOtp Tests ---

    @Test
    @DisplayName("VerifyOtp should activate user when OTP is valid and not expired")
    public void verifyOtp_WhenOtpIsValid_ShouldActivateUser() {
        // Arrange
        String email = "john@example.com";
        String otp = "123456";
        User user = User.builder()
                .id(1L)
                .email(email)
                .status(AccountStatus.PENDING_VERIFICATION.name())
                .build();

        EmailVerificationOTP verification = EmailVerificationOTP.builder()
                .id(10L)
                .userId(1L)
                .otp(otp)
                .verified(false)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(otpRepository.findByUserIdAndOtp(1L, otp)).thenReturn(Optional.of(verification));

        // Act
        userService.verifyOtp(email, otp);

        // Assert
        assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE.name());
        assertThat(verification.isVerified()).isTrue();

        verify(otpRepository, times(1)).save(verification);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("VerifyOtp should throw RuntimeException when user is not found")
    public void verifyOtp_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.verifyOtp("unknown@example.com", "123456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("VerifyOtp should throw RuntimeException when OTP is invalid")
    public void verifyOtp_WhenOtpIsInvalid_ShouldThrowException() {
        // Arrange
        User user = User.builder().id(1L).email("john@example.com").build();
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findByUserIdAndOtp(1L, "wrong")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.verifyOtp("john@example.com", "wrong"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid OTP");
    }

    @Test
    @DisplayName("VerifyOtp should throw RuntimeException when OTP is already used")
    public void verifyOtp_WhenOtpIsAlreadyUsed_ShouldThrowException() {
        // Arrange
        User user = User.builder().id(1L).email("john@example.com").build();
        EmailVerificationOTP verification = EmailVerificationOTP.builder()
                .userId(1L)
                .verified(true)
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findByUserIdAndOtp(1L, "123456")).thenReturn(Optional.of(verification));

        // Act & Assert
        assertThatThrownBy(() -> userService.verifyOtp("john@example.com", "123456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("OTP already used");
    }

    @Test
    @DisplayName("VerifyOtp should throw RuntimeException when OTP is expired")
    public void verifyOtp_WhenOtpIsExpired_ShouldThrowException() {
        // Arrange
        User user = User.builder().id(1L).email("john@example.com").build();
        EmailVerificationOTP verification = EmailVerificationOTP.builder()
                .userId(1L)
                .verified(false)
                .expiryTime(LocalDateTime.now().minusSeconds(1))
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(otpRepository.findByUserIdAndOtp(1L, "123456")).thenReturn(Optional.of(verification));

        // Act & Assert
        assertThatThrownBy(() -> userService.verifyOtp("john@example.com", "123456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("OTP expired");
    }

    // --- resendOtp Tests ---

    @Test
    @DisplayName("ResendOtp should send new OTP when user exists")
    public void resendOtp_WhenUserExists_ShouldGenerateAndSendNewOtp() {
        // Arrange
        String email = "john@example.com";
        User user = User.builder().id(1L).email(email).build();
        EmailVerificationOTP existingOtp = EmailVerificationOTP.builder().userId(1L).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(otpRepository.findByUserId(1L)).thenReturn(Optional.of(existingOtp));

        // Act
        userService.resendOtp(email);

        // Assert
        verify(otpRepository, times(1)).save(existingOtp);
        verify(emailService, times(1)).sendOtp(eq(email), anyString());
    }

    // --- login Tests ---

    @Test
    @DisplayName("Login should return LoginResponse with JWT token on success")
    public void login_OnSuccess_ShouldReturnLoginResponse() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUserNameOrEmail("john@example.com");
        request.setPassword("password123");

        User user = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("encoded_pass")
                .role(Role.USER)
                .status(AccountStatus.ACTIVE.name())
                .build();

        when(userRepository.findByEmailOrUsername(request.getUserNameOrEmail(), request.getUserNameOrEmail()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("jwt_token_xyz");

        // Act
        LoginResponse response = userService.login(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt_token_xyz");
        assertThat(response.getUsername()).isEqualTo("john_doe");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getRole()).isEqualTo(Role.USER.name());

        verify(userRepository, times(1)).findByEmailOrUsername(anyString(), anyString());
        verify(passwordEncoder, times(2)).matches(anyString(), anyString());
        verify(jwtUtil, times(1)).generateToken(user);
    }

    @Test
    @DisplayName("Login should throw RuntimeException when user not found")
    public void login_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUserNameOrEmail("nonexistent@example.com");

        when(userRepository.findByEmailOrUsername(anyString(), anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("Login should throw RuntimeException when password does not match")
    public void login_WhenPasswordIncorrect_ShouldThrowException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUserNameOrEmail("john@example.com");
        request.setPassword("wrongpass");

        User user = User.builder().email("john@example.com").password("encoded_pass").build();
        when(userRepository.findByEmailOrUsername(anyString(), anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "encoded_pass")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid password");
    }

    // --- getCurrentUser Tests ---

    @Test
    @DisplayName("GetCurrentUser should return user when email exists")
    public void getCurrentUser_WhenEmailExists_ShouldReturnUser() {
        // Arrange
        String email = "john@example.com";
        User user = User.builder().email(email).build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act
        User result = userService.getCurrentUser(email);

        // Assert
        assertThat(result).isEqualTo(user);
    }

    // --- updateProfile Tests ---

    @Test
    @DisplayName("UpdateProfile should modify and save user properties")
    public void updateProfile_WhenUserExists_ShouldModifyAndSave() {
        // Arrange
        String email = "john@example.com";
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john_new");
        request.setBio("Updated bio");
        request.setDob(LocalDate.of(1995, 5, 5));
        request.setAvatarUrl("http://avatar.com/new");
        request.setSubscribeNewsletter(true);

        User user = User.builder()
                .email(email)
                .username("john_old")
                .bio("Old bio")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        User result = userService.updateProfile(email, request);

        // Assert
        assertThat(result.getUsername()).isEqualTo("john_new");
        assertThat(result.getBio()).isEqualTo("Updated bio");
        assertThat(result.getDob()).isEqualTo(LocalDate.of(1995, 5, 5));
        assertThat(result.getAvatarUrl()).isEqualTo("http://avatar.com/new");
        assertThat(result.getSubscribeNewsletter()).isTrue();

        verify(userRepository, times(1)).save(user);
    }

    // --- changePassword Tests ---

    @Test
    @DisplayName("ChangePassword should encode and save new password on success")
    public void changePassword_OnSuccess_ShouldEncodeAndSave() {
        // Arrange
        String email = "john@example.com";
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old_pass");
        request.setNewPassword("new_pass");

        User user = User.builder().email(email).password("encoded_old").build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old_pass", "encoded_old")).thenReturn(true);
        when(passwordEncoder.encode("new_pass")).thenReturn("encoded_new");

        // Act
        userService.changePassword(email, request);

        // Assert
        assertThat(user.getPassword()).isEqualTo("encoded_new");
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("ChangePassword should throw RuntimeException when current password doesn't match")
    public void changePassword_WhenPasswordIncorrect_ShouldThrowException() {
        // Arrange
        String email = "john@example.com";
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong_old");

        User user = User.builder().email(email).password("encoded_old").build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_old", "encoded_old")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> userService.changePassword(email, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Wrong current password");
    }

    // --- searchUsers Tests ---

    @Test
    @DisplayName("SearchUsers should return mapped search results")
    public void searchUsers_ShouldReturnMappedSearchResults() {
        // Arrange
        String query = "john";
        User user = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .avatarUrl("http://avatar.url")
                .build();

        when(userRepository.findTop10ByUsernameContainingIgnoreCase(query))
                .thenReturn(Collections.singletonList(user));

        // Act
        List<UserSearchDTO> result = userService.searchUsers(query);

        // Assert
        assertThat(result).hasSize(1);
        UserSearchDTO dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUsername()).isEqualTo("john_doe");
        assertThat(dto.getEmail()).isEqualTo("john@example.com");
        assertThat(dto.getAvatarUrl()).isEqualTo("http://avatar.url");
    }
}
