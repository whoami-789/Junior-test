package com.jsr.tasktrackerapi.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Необходимо указать email")
        @Email(message = "Email должен быть валидным")
        String email,

        @NotBlank(message = "Необходимо указать пароль")
        @Size(min = 8, message = "Пароль не может содержать менее 8 символов")
        @Pattern(
                regexp = "(?=.*[A-Za-z])(?=.*\\d).*",
                message = "Пароль должен содержать как минимум одну букву и одну цифру."
        )
        String password,

        @NotBlank(message = "Необходимо указать имя")
        @Size(min = 2, max = 100, message = "Имя должно содержать от 2 до 100 символов")
        String name
) {

}
