package com.jsr.tasktrackerapi.service;

import com.jsr.tasktrackerapi.domain.entity.Task;
import com.jsr.tasktrackerapi.domain.entity.User;
import com.jsr.tasktrackerapi.domain.enums.Role;
import com.jsr.tasktrackerapi.domain.enums.TaskStatus;
import com.jsr.tasktrackerapi.dto.task.TaskCreateRequest;
import com.jsr.tasktrackerapi.dto.task.TaskFilter;
import com.jsr.tasktrackerapi.dto.task.TaskResponse;
import com.jsr.tasktrackerapi.dto.task.TaskUpdateRequest;
import com.jsr.tasktrackerapi.exception.BusinessRuleException;
import com.jsr.tasktrackerapi.exception.ResourceNotFoundException;
import com.jsr.tasktrackerapi.mapper.TaskMapper;
import com.jsr.tasktrackerapi.repo.TaskRepository;
import com.jsr.tasktrackerapi.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    @Transactional
    public TaskResponse create(TaskCreateRequest request, String currentEmail) {

        User current = loadUser(currentEmail);

        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setStatus(TaskStatus.TODO);
        task.setCreator(current);

        if (request.assigneeId() != null) {
            task.setAssignee(resolveAssignee(request.assigneeId(), current));
        }

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(UUID id, String currentEmail) {

        Task task = loadTask(id);
        User current = loadUser(currentEmail);

        if (!isAdmin(current) && !isCreator(task, current) && !isAssignee(task, current)) {
            throw new AccessDeniedException("Нет доступа к этой задаче");
        }

        return taskMapper.toResponse(task);
    }

    @Transactional
    public TaskResponse update(UUID id, TaskUpdateRequest request, String currentEmail) {

        Task task = loadTask(id);
        User current = loadUser(currentEmail);

        boolean admin = isAdmin(current);
        boolean creator = isCreator(task, current);
        boolean assignee = isAssignee(task, current);

        if (!admin && !creator && !assignee) {
            throw new AccessDeniedException("Нет доступа к этой задаче");
        }

        boolean canEditContent = admin || creator;
        boolean canEditStatus = admin || creator || assignee;

        if (request.title() != null) {
            requireRight(canEditContent, "Изменять заголовок может только creator или ADMIN");
            task.setTitle(request.title());
        }

        if (request.description() != null) {
            requireRight(canEditContent, "Изменять описание может только creator или ADMIN");
            task.setDescription(request.description());
        }

        if (request.priority() != null) {
            requireRight(canEditContent, "Изменять приоритет может только creator или ADMIN");
            task.setPriority(request.priority());
        }

        if (request.assigneeId() != null) {
            requireRight(canEditContent, "Изменять исполнителя может только creator или ADMIN");
            task.setAssignee(resolveAssignee(request.assigneeId(), current));
        }

        if (request.status() != null) {
            requireRight(canEditStatus, "Изменять статус может только creator, assignee или ADMIN");
            if (request.status() == TaskStatus.DONE && task.getAssignee() == null) {
                throw new BusinessRuleException("Cannot mark task as DONE without assignee");
            }
            task.setStatus(request.status());
        }

        return taskMapper.toResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(UUID id, String currentEmail) {

        Task task = loadTask(id);
        User current = loadUser(currentEmail);

        if (!isAdmin(current) && !isCreator(task, current)) {
            throw new AccessDeniedException("Удалить задачу может только creator или ADMIN");
        }

        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> list(TaskFilter filter, Pageable pageable, String currentEmail) {

        User current = loadUser(currentEmail);
        boolean admin = isAdmin(current);

        Specification<Task> spec = (root, query, cb) -> cb.conjunction();

        if (filter.status() != null && !filter.status().isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("status").in(filter.status()));
        }

        if (filter.priority() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), filter.priority()));
        }

        if (filter.assigneeId() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("assignee").get("id"), filter.assigneeId()));
        }

        if (filter.search() != null && !filter.search().isBlank()) {
            String pattern = "%" + filter.search().toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("title")), pattern));
        }

        if (admin) {
            if (filter.creatorId() != null) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("creator").get("id"), filter.creatorId()));
            }
        } else {
            UUID myId = current.getId();
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.equal(root.get("creator").get("id"), myId),
                    cb.equal(root.get("assignee").get("id"), myId)));
        }

        return taskRepository.findAll(spec, pageable).map(taskMapper::toResponse);
    }

    private User resolveAssignee(UUID assigneeId, User current) {

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("Исполнитель не найден"));

        if (!isAdmin(current) && !assignee.getId().equals(current.getId())) {
            throw new AccessDeniedException("USER может назначить исполнителем только себя");
        }

        return assignee;
    }

    private User loadUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    private Task loadTask(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Задача не найдена"));
    }

    private boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    private boolean isCreator(Task task, User user) {
        return task.getCreator().getId().equals(user.getId());
    }

    private boolean isAssignee(Task task, User user) {
        return task.getAssignee() != null && task.getAssignee().getId().equals(user.getId());
    }

    private void requireRight(boolean allowed, String message) {
        if (!allowed) {
            throw new AccessDeniedException(message);
        }
    }
}
