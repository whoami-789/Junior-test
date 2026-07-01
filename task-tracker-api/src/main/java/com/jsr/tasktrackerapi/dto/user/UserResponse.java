package com.jsr.tasktrackerapi.dto.user;

import com.jsr.tasktrackerapi.domain.enums.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        Role role,
        Instant createdAt
) {
}
