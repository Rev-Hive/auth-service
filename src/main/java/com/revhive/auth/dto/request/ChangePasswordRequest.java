package com.revhive.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema
public class ChangePasswordRequest {

    @Schema(example = "oldpassword@123", required = true)
    private String currentPassword;

    @Schema(example = "newpassword@123", required = true)
    private String newPassword;

}
