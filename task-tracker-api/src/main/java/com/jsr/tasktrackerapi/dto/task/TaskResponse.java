package com.jsr.tasktrackerapi.dto.task;

import com.jsr.tasktrackerapi.domain.enums.TaskPriority;
import com.jsr.tasktrackerapi.domain.enums.TaskStatus;
import com.jsr.tasktrackerapi.dto.user.UserResponse;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        UserResponse creator,
        UserResponse assignee,
        Instant createdAt,
        Instant updateAt
) {
}
