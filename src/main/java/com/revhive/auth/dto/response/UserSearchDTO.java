
package com.revhive.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserSearchDTO {

    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
}
