package com.sonny.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TodoRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 255, message = "Title must have at least 3 and not exceed 255 characters")
        String title,

        String description,

        boolean completed
) {}
