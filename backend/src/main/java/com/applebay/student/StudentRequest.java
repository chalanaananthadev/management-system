package com.applebay.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentRequest(
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

        @NotBlank(message = "Phone is required.")
        @Size(max = 20, message = "Phone must be 20 characters or fewer.")
        String phone,

        @NotBlank(message = "Address is required.")
        @Size(max = 255, message = "Address must be 255 characters or fewer.")
        String address
) {
}
