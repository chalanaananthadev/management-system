package com.applebay.student;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StudentOnboardingRequest(
        @NotBlank(message = "Student name is required.")
        @Size(max = 150, message = "Student name must be 150 characters or fewer.")
        String name,

        @NotBlank(message = "NIC is required.")
        @Size(max = 20, message = "NIC must be 20 characters or fewer.")
        String nic,

        @NotBlank(message = "Email is required.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 150, message = "Email must be 150 characters or fewer.")
        String email,

        @NotBlank(message = "Phone number is required.")
        @Size(max = 20, message = "Phone must be 20 characters or fewer.")
        String phone,

        @NotBlank(message = "Address is required.")
        @Size(max = 255, message = "Address must be 255 characters or fewer.")
        String address,

        @NotNull(message = "Course selection is required.")
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

