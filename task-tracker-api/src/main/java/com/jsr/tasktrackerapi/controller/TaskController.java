package com.jsr.tasktrackerapi.controller;

import com.jsr.tasktrackerapi.domain.enums.TaskPriority;
import com.jsr.tasktrackerapi.domain.enums.TaskStatus;
import com.jsr.tasktrackerapi.dto.task.TaskCreateRequest;
import com.jsr.tasktrackerapi.dto.task.TaskFilter;
import com.jsr.tasktrackerapi.dto.task.TaskResponse;
import com.jsr.tasktrackerapi.dto.task.TaskUpdateRequest;
import com.jsr.tasktrackerapi.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(
            @Valid @RequestBody TaskCreateRequest request,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        return taskService.create(request, currentUser.getUsername());
    }

    @GetMapping("/{id}")
    public TaskResponse getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        return taskService.getById(id, currentUser.getUsername());
    }

    @PatchMapping("/{id}")
    public TaskResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody TaskUpdateRequest request,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        return taskService.update(id, request, currentUser.getUsername());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        taskService.delete(id, currentUser.getUsername());
    }

    @GetMapping
    public Page<TaskResponse> list(
            @RequestParam(required = false) List<TaskStatus> status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) UUID creatorId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        TaskFilter filter = new TaskFilter(status, priority, assigneeId, creatorId, search);
        return taskService.list(filter, pageable, currentUser.getUsername());
    }
}
