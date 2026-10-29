package com.applebay.enrolment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EnrolmentRequest(
        @NotNull(message = "Choose a student.")
        Long studentId,

        @NotNull(message = "Choose a course.")
        Long courseId,

        LocalDate enrolledOn,

        @NotNull(message = "First payment amount is required.")
        @DecimalMin(value = "0.01", message = "First payment amount must be greater than zero.")
        BigDecimal paymentAmount,

        @NotBlank(message = "Payment method is required.")
        String paymentMethod,

        LocalDate paymentDate
) {
}
