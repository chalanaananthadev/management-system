package com.applebay.student;

public record StudentResponse(
        Long id,
        String name,
        String nic,
        String email,
        String phone,
        String address
) {
}
