package com.sonny.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TodoNotFoundException")
class TodoNotFoundExceptionTest {

    @Test
    @DisplayName("should produce message containing the given id")
    void should_includeId_in_message() {
        TodoNotFoundException ex = new TodoNotFoundException(42L);

        assertThat(ex.getMessage()).isEqualTo("Todo not found with id: 42");
    }

    @Test
    @DisplayName("should be a RuntimeException")
    void should_extendRuntimeException() {
        assertThat(new TodoNotFoundException(1L)).isInstanceOf(RuntimeException.class);
    }
}
