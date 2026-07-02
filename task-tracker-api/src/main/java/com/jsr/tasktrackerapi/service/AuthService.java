package com.jsr.tasktrackerapi.service;

import com.jsr.tasktrackerapi.domain.entity.User;
import com.jsr.tasktrackerapi.domain.enums.Role;
import com.jsr.tasktrackerapi.dto.auth.AuthResponse;
import com.jsr.tasktrackerapi.dto.auth.LoginRequest;
import com.jsr.tasktrackerapi.dto.auth.RegisterRequest;
import com.jsr.tasktrackerapi.dto.user.UserResponse;
import com.jsr.tasktrackerapi.exception.EmailAlreadyExistsException;
import com.jsr.tasktrackerapi.mapper.UserMapper;
import com.jsr.tasktrackerapi.repo.UserRepository;
import com.jsr.tasktrackerapi.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email уже используется");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setName(request.name());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        User savedUser = userRepository.save(user);

        return buildAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        return buildAuthResponse(user);
    }

    public UserResponse me(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        return userMapper.toResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {

        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpiresIn(),
                userMapper.toResponse(user)
        );
    }

}
