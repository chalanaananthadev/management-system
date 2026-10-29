package com.applebay.payment;

import java.time.LocalDateTime;

public record SendEbillResponse(
        boolean success,
        String message,
        String recipientEmail,
        String receiptNumber,
        LocalDateTime sentAt,
        String emailSubject,
        String emailContentPreview
) {
}

