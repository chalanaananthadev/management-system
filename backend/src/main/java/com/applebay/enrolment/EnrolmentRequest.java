package com.applebay.enrolment;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EnrolmentRequest(
        @NotNull(message = "Choose a student.")
        Long studentId,

        @NotNull(message = "Choose a course.")
        Long courseId,

        LocalDate enrolledOn
) {
}
