package com.jsr.tasktrackerapi.dto.auth;

public record LoginRequest(
        String email,
        String password
) {
}
