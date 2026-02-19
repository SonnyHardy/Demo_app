package com.sonny.todo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonny.config.SecurityConfig;
import com.sonny.exception.GlobalExceptionHandler;
import com.sonny.exception.TodoNotFoundException;
import com.sonny.todo.dto.TodoRequest;
import com.sonny.todo.dto.TodoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TodoController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("TodoController")
class TodoControllerTest {

    @Autowired
    MockMvc mockMvc;

    // ObjectMapper is not auto-configured by @WebMvcTest in Spring Boot 4 - create directly
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    TodoService todoService;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private TodoResponse sampleResponse() {
        return new TodoResponse(1L, "Buy groceries", "Milk, Eggs", false, NOW, NOW);
    }

    @Nested
    @DisplayName("GET /api/todos")
    class GetAll {

        @Test
        @DisplayName("should return 200 with list of todos")
        void should_return200WithList() throws Exception {
            given(todoService.findAll()).willReturn(List.of(sampleResponse()));

            mockMvc.perform(get("/api/todos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].title").value("Buy groceries"))
                    .andExpect(jsonPath("$[0].completed").value(false));
        }

        @Test
        @DisplayName("should return 200 with empty list when no todos exist")
        void should_return200WithEmptyList_when_noTodosExist() throws Exception {
            given(todoService.findAll()).willReturn(List.of());

            mockMvc.perform(get("/api/todos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/todos/{id}")
    class GetById {

        @Test
        @DisplayName("should return 200 with the todo")
        void should_return200WithTodo_when_idExists() throws Exception {
            given(todoService.findById(1L)).willReturn(sampleResponse());

            mockMvc.perform(get("/api/todos/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(6)) // 6 fields in the response
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Buy groceries"))
                    .andExpect(jsonPath("$.description").value("Milk, Eggs"))
                    .andExpect(jsonPath("$.completed").value(false));
        }

        @Test
        @DisplayName("should return 404 with error body when id does not exist")
        void should_return404_when_idNotFound() throws Exception {
            given(todoService.findById(99L)).willThrow(new TodoNotFoundException(99L));

            mockMvc.perform(get("/api/todos/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Todo not found with id: 99"));
        }
    }

    @Nested
    @DisplayName("POST /api/todos")
    class Create {

        @Test
        @DisplayName("should return 201 with Location header and created todo")
        void should_return201WithLocation_when_requestIsValid() throws Exception {
            TodoRequest request = new TodoRequest("Buy groceries", "Milk, Eggs", false);
            //given(todoService.create(any(TodoRequest.class))).willReturn(sampleResponse());
            given(todoService.create(request)).willReturn(sampleResponse());

            mockMvc.perform(post("/api/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", endsWith("/api/todos/1")))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Buy groceries"));
        }

        @Test
        @DisplayName("should return 400 when title is blank")
        void should_return400_when_titleIsBlank() throws Exception {
            TodoRequest request = new TodoRequest("", null, false);

            mockMvc.perform(post("/api/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when title is shorter than 3 characters")
        void should_return400_when_titleIsTooShort() throws Exception {
            TodoRequest request = new TodoRequest("AB", null, false);

            mockMvc.perform(post("/api/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 400 when title exceeds 255 characters")
        void should_return400_when_titleExceedsMaxLength() throws Exception {
            TodoRequest request = new TodoRequest("A".repeat(256), null, false);

            mockMvc.perform(post("/api/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("should return 500 when request body is missing")
        void should_return500_when_bodyIsMissing() throws Exception {
            // HttpMessageNotReadableException is caught by the generic handler, returning 500
            mockMvc.perform(post("/api/todos")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
        }

        @Test
        @DisplayName("should accept todo with null description")
        void should_return201_when_descriptionIsNull() throws Exception {
            TodoRequest request = new TodoRequest("Buy groceries", null, false);
            TodoResponse response = new TodoResponse(1L, "Buy groceries", null, false, NOW, NOW);
            given(todoService.create(any(TodoRequest.class))).willReturn(response);

            mockMvc.perform(post("/api/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.description").doesNotExist());
        }
    }

    @Nested
    @DisplayName("PUT /api/todos/{id}")
    class Update {

        @Test
        @DisplayName("should return 200 with updated todo")
        void should_return200WithUpdatedTodo_when_idExists() throws Exception {
            TodoRequest request = new TodoRequest("Updated title", "Updated desc", true);
            TodoResponse updated = new TodoResponse(1L, "Updated title", "Updated desc", true, NOW, NOW);
            given(todoService.update(eq(1L), any(TodoRequest.class))).willReturn(updated);

            mockMvc.perform(put("/api/todos/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated title"))
                    .andExpect(jsonPath("$.description").value("Updated desc"))
                    .andExpect(jsonPath("$.completed").value(true));
        }

        @Test
        @DisplayName("should return 404 when id does not exist")
        void should_return404_when_idNotFound() throws Exception {
            TodoRequest request = new TodoRequest("Title", null, false);
            given(todoService.update(eq(99L), any(TodoRequest.class)))
                    .willThrow(new TodoNotFoundException(99L));

            mockMvc.perform(put("/api/todos/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("should return 400 when title is invalid")
        void should_return400_when_titleIsInvalid() throws Exception {
            TodoRequest request = new TodoRequest("AB", null, false);

            mockMvc.perform(put("/api/todos/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/todos/{id}")
    class Delete {

        @Test
        @DisplayName("should return 204 when todo is deleted")
        void should_return204_when_idExists() throws Exception {
            willDoNothing().given(todoService).delete(1L);

            mockMvc.perform(delete("/api/todos/1"))
                    .andExpect(status().isNoContent());

            then(todoService).should().delete(1L);
        }

        @Test
        @DisplayName("should return 404 when id does not exist")
        void should_return404_when_idNotFound() throws Exception {
            willThrow(new TodoNotFoundException(99L)).given(todoService).delete(99L);

            mockMvc.perform(delete("/api/todos/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Todo not found with id: 99"));
        }
    }
}
