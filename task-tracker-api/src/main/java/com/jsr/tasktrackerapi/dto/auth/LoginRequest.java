package com.jsr.tasktrackerapi.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Необходимо указать email")
        String email,

        @NotBlank(message = "Необходимо указать пароль")
        String password
) {
}
