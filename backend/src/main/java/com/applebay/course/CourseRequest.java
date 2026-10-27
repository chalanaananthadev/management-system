package com.applebay.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotBlank(message = "Course code is required.")
        @Size(max = 20, message = "Course code must be 20 characters or fewer.")
        String courseCode,

        @NotBlank(message = "Course name is required.")
        @Size(max = 150, message = "Course name must be 150 characters or fewer.")
        String name,

        @NotBlank(message = "Level is required.")
        @Size(max = 50, message = "Level must be 50 characters or fewer.")
        String level,

        @NotBlank(message = "Duration is required.")
        @Size(max = 50, message = "Duration must be 50 characters or fewer.")
        String duration
) {
}
