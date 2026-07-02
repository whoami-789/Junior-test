package com.jsr.tasktrackerapi.config;

import com.jsr.tasktrackerapi.domain.entity.User;
import com.jsr.tasktrackerapi.domain.enums.Role;
import com.jsr.tasktrackerapi.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        createUser("admin@example.com", "Admin", Role.ADMIN);
        createUser("user1@example.com", "User One", Role.USER);
        createUser("user2@example.com", "User Two", Role.USER);
    }

    private void createUser(String email, String name, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(passwordEncoder.encode("SecurePass123"));
        user.setRole(role);
        userRepository.save(user);
    }
}
