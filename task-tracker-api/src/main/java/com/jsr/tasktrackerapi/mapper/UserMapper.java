package com.jsr.tasktrackerapi.mapper;

import com.jsr.tasktrackerapi.domain.entity.User;
import com.jsr.tasktrackerapi.dto.user.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserResponse toResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
