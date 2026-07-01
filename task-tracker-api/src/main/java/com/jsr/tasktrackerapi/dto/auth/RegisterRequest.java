package com.jsr.tasktrackerapi.dto.auth;

public record RegisterRequest(
        String email,
        String password,
        String name
) {

}
