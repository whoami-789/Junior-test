package com.jsr.tasktrackerapi.dto.task;

import com.jsr.tasktrackerapi.domain.enums.TaskPriority;
import com.jsr.tasktrackerapi.domain.enums.TaskStatus;

import java.util.UUID;

public record TaskUpdateRequest(
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        UUID assigneeId
) {

}
