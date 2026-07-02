package com.jsr.tasktrackerapi.dto.task;

import com.jsr.tasktrackerapi.domain.enums.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TaskCreateRequest(
        @NotBlank(message = "Необходимо указать заголовок")
        @Size(min = 3, max = 200, message = "Заголовок должен содержать от 3 до 200 символов")
        String title,

        @Size(max = 5000, message = "Описание не может превышать 5000 символов")
        String description,

        @NotNull(message = "Необходимо указать приоритет")
        TaskPriority priority,

        UUID assigneeId
) {

}
