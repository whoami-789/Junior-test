package com.jsr.tasktrackerapi.dto.task;

import com.jsr.tasktrackerapi.domain.enums.TaskPriority;
import com.jsr.tasktrackerapi.domain.enums.TaskStatus;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TaskUpdateRequest(
        @Size(min = 3, max = 200, message = "Заголовок должен содержать от 3 до 200 символов")
        String title,

        @Size(max = 5000, message = "Описание не может превышать 5000 символов")
        String description,

        TaskStatus status,

        TaskPriority priority,

        UUID assigneeId
) {

}
