package com.sonny.todo;

import com.sonny.exception.TodoNotFoundException;
import com.sonny.todo.dto.TodoRequest;
import com.sonny.todo.dto.TodoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TodoService")
class TodoServiceTest {

    @Mock
    TodoRepository todoRepository;

    @InjectMocks
    TodoService todoService;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private Todo buildTodo(Long id, String title, String description, boolean completed) {
        Todo todo = Todo.builder()
                .id(id)
                .title(title)
                .description(description)
                .completed(completed)
                .build();
        todo.setCreatedAt(NOW);
        todo.setUpdatedAt(NOW);
        return todo;
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return mapped responses for all todos")
        void should_returnAllTodos() {
            Todo t1 = buildTodo(1L, "Buy groceries", "Milk, Eggs", false);
            Todo t2 = buildTodo(2L, "Read book", null, true);
            given(todoRepository.findAll()).willReturn(List.of(t1, t2));

            List<TodoResponse> result = todoService.findAll();

            assertThat(result)
                    .hasSize(2)
                    .extracting(TodoResponse::id, TodoResponse::title, TodoResponse::completed)
                    .containsExactly(
                            tuple(1L, "Buy groceries", false),
                            tuple(2L, "Read book", true)
                    );
        }

        @Test
        @DisplayName("should return empty list when no todos exist")
        void should_returnEmptyList_when_noTodosExist() {
            given(todoRepository.findAll()).willReturn(List.of());

            List<TodoResponse> result = todoService.findAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return matching todo response")
        void should_returnTodoResponse_when_idExists() {
            Todo todo = buildTodo(1L, "Buy groceries", "Milk, Eggs", false);
            given(todoRepository.findById(1L)).willReturn(Optional.of(todo));

            TodoResponse result = todoService.findById(1L);

            assertThat(result)
                    .extracting(TodoResponse::id, TodoResponse::title, TodoResponse::description,
                            TodoResponse::completed, TodoResponse::createdAt, TodoResponse::updatedAt)
                    .containsExactly(1L, "Buy groceries", "Milk, Eggs", false, NOW, NOW);
        }

        @Test
        @DisplayName("should throw TodoNotFoundException when id does not exist")
        void should_throwTodoNotFoundException_when_idNotFound() {
            given(todoRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> todoService.findById(99L))
                    .isInstanceOf(TodoNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should persist a new todo and return its response")
        void should_saveAndReturnResponse() {
            TodoRequest request = new TodoRequest("Buy groceries", "Milk, Eggs", false);
            Todo saved = buildTodo(1L, "Buy groceries", "Milk, Eggs", false);
            given(todoRepository.save(any(Todo.class))).willReturn(saved);

            TodoResponse result = todoService.create(request);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("Buy groceries");
            assertThat(result.description()).isEqualTo("Milk, Eggs");
            assertThat(result.completed()).isFalse();
            then(todoRepository).should().save(any(Todo.class));
        }

        @Test
        @DisplayName("should persist a todo without description")
        void should_saveAndReturn_when_descriptionIsNull() {
            TodoRequest request = new TodoRequest("Read book", null, true);
            Todo saved = buildTodo(2L, "Read book", null, true);
            given(todoRepository.save(any(Todo.class))).willReturn(saved);

            TodoResponse result = todoService.create(request);

            assertThat(result.description()).isNull();
            assertThat(result.completed()).isTrue();
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        private Todo existingTodo;

        @BeforeEach
        void setUp() {
            existingTodo = buildTodo(1L, "Old title", "Old desc", false);
        }

        @Test
        @DisplayName("should update fields and return updated response")
        void should_modifyAndReturn_when_idExists() {
            TodoRequest request = new TodoRequest("New title", "New desc", true);
            given(todoRepository.findById(1L)).willReturn(Optional.of(existingTodo));
            given(todoRepository.save(existingTodo)).willReturn(existingTodo);

            todoService.update(1L, request);

            assertThat(existingTodo.getTitle()).isEqualTo("New title");
            assertThat(existingTodo.getDescription()).isEqualTo("New desc");
            assertThat(existingTodo.isCompleted()).isTrue();
            then(todoRepository).should().save(existingTodo);
        }

        @Test
        @DisplayName("should throw TodoNotFoundException when id does not exist")
        void should_throwTodoNotFoundException_when_idNotFound() {
            TodoRequest request = new TodoRequest("Title", null, false);
            given(todoRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> todoService.update(99L, request))
                    .isInstanceOf(TodoNotFoundException.class)
                    .hasMessageContaining("99");

            then(todoRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete todo when it exists")
        void should_deleteTodo_when_idExists() {
            given(todoRepository.existsById(1L)).willReturn(true);
            willDoNothing().given(todoRepository).deleteById(1L);

            todoService.delete(1L);

            then(todoRepository).should().existsById(1L);
            then(todoRepository).should().deleteById(1L);
        }

        @Test
        @DisplayName("should throw TodoNotFoundException without deleting when id does not exist")
        void should_throwTodoNotFoundException_andNotDelete_when_idNotFound() {
            given(todoRepository.existsById(99L)).willReturn(false);

            assertThatThrownBy(() -> todoService.delete(99L))
                    .isInstanceOf(TodoNotFoundException.class)
                    .hasMessageContaining("99");

            then(todoRepository).should(never()).deleteById(any());
        }
    }
}
