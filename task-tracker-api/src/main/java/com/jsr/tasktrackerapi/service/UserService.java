package com.jsr.tasktrackerapi.service;

import com.jsr.tasktrackerapi.domain.entity.User;
import com.jsr.tasktrackerapi.domain.enums.Role;
import com.jsr.tasktrackerapi.dto.user.UserResponse;
import com.jsr.tasktrackerapi.exception.ResourceNotFoundException;
import com.jsr.tasktrackerapi.mapper.UserMapper;
import com.jsr.tasktrackerapi.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse getById(UUID id, String currentEmail) {

        User current = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        boolean isAdmin = current.getRole() == Role.ADMIN;

        if (!isAdmin && !current.getId().equals(id)) {
            throw new AccessDeniedException("Доступ разрешён только к своему профилю");
        }

        User target = isAdmin
                ? userRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"))
                : current;

        return userMapper.toResponse(target);
    }

    public Page<UserResponse> getAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }
}
