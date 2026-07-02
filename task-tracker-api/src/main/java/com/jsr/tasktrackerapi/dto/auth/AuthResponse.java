package com.jsr.tasktrackerapi.dto.auth;

import com.jsr.tasktrackerapi.dto.user.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Long expiresIn,
        UserResponse user
) {

}
