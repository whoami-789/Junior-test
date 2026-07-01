package com.jsr.tasktrackerapi.dto.task;

import com.jsr.tasktrackerapi.domain.enums.TaskPriority;

import java.util.UUID;

public record TaskCreateRequest(
        String title,
        String description,
        TaskPriority priority,
        UUID assigneeId

) {

}
