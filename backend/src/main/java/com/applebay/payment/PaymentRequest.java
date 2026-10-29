package com.applebay.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequest(
        @NotNull(message = "Enrolment ID is required.")
        Long enrolmentId,

        @NotNull(message = "Payment amount is required.")
        @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero.")
        BigDecimal amount,

        LocalDate paymentDate,

        @NotBlank(message = "Payment method is required.")
        String paymentMethod
) {
}

