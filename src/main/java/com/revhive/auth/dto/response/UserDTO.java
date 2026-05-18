package com.revhive.auth.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private String bio;
    private String avatarUrl;
    private String role;
    private int followerCount;
    private int followingCount;
    private int postCount;
    private LocalDateTime createdAt;
}
