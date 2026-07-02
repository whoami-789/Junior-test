package com.jsr.tasktrackerapi.dto.task;

import com.jsr.tasktrackerapi.domain.enums.TaskPriority;
import com.jsr.tasktrackerapi.domain.enums.TaskStatus;

import java.util.List;
import java.util.UUID;

public record TaskFilter(
        List<TaskStatus> status,
        TaskPriority priority,
        UUID assigneeId,
        UUID creatorId,
        String search
) {

}
