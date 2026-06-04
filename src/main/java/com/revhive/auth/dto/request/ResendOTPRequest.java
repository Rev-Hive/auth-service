package com.revhive.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendOTPRequest {

    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private String email;
}
