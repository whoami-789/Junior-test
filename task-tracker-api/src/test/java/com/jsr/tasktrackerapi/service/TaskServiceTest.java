package com.jsr.tasktrackerapi.service;

import com.jsr.tasktrackerapi.domain.entity.Task;
import com.jsr.tasktrackerapi.domain.entity.User;
import org.mockito.ArgumentMatchers;
import com.jsr.tasktrackerapi.domain.enums.Role;
import com.jsr.tasktrackerapi.domain.enums.TaskStatus;
import com.jsr.tasktrackerapi.dto.task.TaskUpdateRequest;
import com.jsr.tasktrackerapi.exception.BusinessRuleException;
import com.jsr.tasktrackerapi.mapper.TaskMapper;
import com.jsr.tasktrackerapi.repo.TaskRepository;
import com.jsr.tasktrackerapi.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskService taskService;

    @Test
    void update_toDoneWithoutAssignee_throwsBusinessRule() {
        User current = user(UUID.randomUUID(), "alice@example.com", Role.USER);
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setCreator(current);
        task.setStatus(TaskStatus.TODO);

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(current));

        TaskUpdateRequest request =
                new TaskUpdateRequest(null, null, TaskStatus.DONE, null, null);

        assertThrows(BusinessRuleException.class,
                () -> taskService.update(task.getId(), request, "alice@example.com"));
    }

    @Test
    void delete_foreignTaskAsUser_throwsAccessDenied() {
        User current = user(UUID.randomUUID(), "alice@example.com", Role.USER);
        User owner = user(UUID.randomUUID(), "bob@example.com", Role.USER);
        Task task = new Task();
        task.setId(UUID.randomUUID());
        task.setCreator(owner);

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(current));

        assertThrows(AccessDeniedException.class,
                () -> taskService.delete(task.getId(), "alice@example.com"));

        verify(taskRepository, never()).delete(ArgumentMatchers.any(Task.class));
    }

    private User user(UUID id, String email, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName("Test");
        user.setPassword("hash");
        user.setRole(role);
        return user;
    }
}
