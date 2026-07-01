package com.jsr.tasktrackerapi.mapper;

import com.jsr.tasktrackerapi.domain.entity.Task;
import com.jsr.tasktrackerapi.dto.task.TaskResponse;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    private final UserMapper userMapper;


    public TaskMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                userMapper.toResponse(task.getCreator()), //mb LInitException
                task.getAssignee() != null ? userMapper.toResponse(task.getAssignee()) : null,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );

    }
}
