package com.jsr.tasktrackerapi.service;

import com.jsr.tasktrackerapi.dto.auth.RegisterRequest;
import com.jsr.tasktrackerapi.exception.EmailAlreadyExistsException;
import com.jsr.tasktrackerapi.mapper.UserMapper;
import com.jsr.tasktrackerapi.repo.UserRepository;
import com.jsr.tasktrackerapi.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_withDuplicateEmail_throwsAndDoesNotSave() {
        RegisterRequest request = new RegisterRequest("taken@example.com", "SecurePass123", "Alice");
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any());
    }
}
