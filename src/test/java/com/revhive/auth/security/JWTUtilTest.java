package com.revhive.auth.security;

import com.revhive.auth.enums.Role;
import com.revhive.auth.model.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class JWTUtilTest {

    private JWTUtil jwtUtil;

    @BeforeEach
    public void setUp() throws Exception {
        jwtUtil = new JWTUtil();
        // Set the private SECRET_KEY using reflection since we are running a pure unit test
        Field field = JWTUtil.class.getDeclaredField("SECRET_KEY");
        field.setAccessible(true);
        field.set(jwtUtil, "mySuperSecretKeyThatIsAtLeast32BytesLong!");
    }

    @Test
    @DisplayName("GenerateToken should generate a valid JWT containing claims")
    public void generateToken_ShouldGenerateValidToken() {
        // Arrange
        User user = User.builder()
                .id(123L)
                .email("user@example.com")
                .role(Role.USER)
                .premium(true)
                .build();

        // Act
        String token = jwtUtil.generateToken(user);

        // Assert
        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("user@example.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("USER");
        assertThat(jwtUtil.isTokenValid(token, "user@example.com")).isTrue();
        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("ExtractClaim should retrieve specific claims correctly")
    public void extractClaim_ShouldRetrieveClaimsCorrectly() {
        // Arrange
        User user = User.builder()
                .id(456L)
                .email("admin@example.com")
                .role(Role.ADMIN)
                .premium(false)
                .build();

        String token = jwtUtil.generateToken(user);

        // Act & Assert
        String subject = jwtUtil.extractUsername(token);
        Date expiration = jwtUtil.extractExpiration(token);
        Claims allClaims = jwtUtil.extractAllClaims(token);

        assertThat(subject).isEqualTo("admin@example.com");
        assertThat(expiration).isAfter(new Date());
        assertThat(allClaims.get("userId", Integer.class)).isEqualTo(456);
        assertThat(allClaims.get("premium", Boolean.class)).isFalse();
    }

    @Test
    @DisplayName("IsTokenValid should return false when username does not match")
    public void isTokenValid_WhenUsernameMismatches_ShouldReturnFalse() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .role(Role.USER)
                .build();

        String token = jwtUtil.generateToken(user);

        // Act
        boolean isValid = jwtUtil.isTokenValid(token, "different@example.com");

        // Assert
        assertThat(isValid).isFalse();
    }
}
