package com.applebay.attendance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record MarkAttendanceRequest(
        @NotNull(message = "Choose a course.")
        Long courseId,

        @NotNull(message = "Choose a date.")
        LocalDate date,

        @NotEmpty(message = "Mark at least one student.")
        List<@Valid Entry> records
) {
    public record Entry(
            @NotNull(message = "Student is required.")
            Long studentId,

            @NotBlank(message = "Mark every student as Present or Absent.")
            String status
    ) {
    }
}
