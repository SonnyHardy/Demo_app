package com.sonny.todo;

import com.sonny.exception.TodoNotFoundException;
import com.sonny.todo.dto.TodoRequest;
import com.sonny.todo.dto.TodoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;

    public List<TodoResponse> findAll() {
        return todoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public TodoResponse findById(Long id) {
        return todoRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    @Transactional
    public TodoResponse create(TodoRequest request) {
        Todo todo = Todo.builder()
                .title(request.title())
                .description(request.description())
                .completed(request.completed())
                .build();
        return toResponse(todoRepository.save(todo));
    }

    @Transactional
    public TodoResponse update(Long id, TodoRequest request) {
        Todo todo = todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));
        todo.setTitle(request.title());
        if (request.description() != null && !request.description().isBlank()) {
            todo.setDescription(request.description());
        }
        todo.setCompleted(request.completed());
        return toResponse(todoRepository.save(todo));
    }

    @Transactional
    public void delete(Long id) {
        if (!todoRepository.existsById(id)) {
            throw new TodoNotFoundException(id);
        }
        todoRepository.deleteById(id);
    }

    private TodoResponse toResponse(Todo todo) {
        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getDescription(),
                todo.isCompleted(),
                todo.getCreatedAt(),
                todo.getUpdatedAt()
        );
    }
}
