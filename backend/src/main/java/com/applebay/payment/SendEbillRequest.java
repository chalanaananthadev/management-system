package com.applebay.payment;

public record SendEbillRequest(
        String email,
        String notes
) {
}

