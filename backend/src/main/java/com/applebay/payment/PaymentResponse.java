package com.applebay.payment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentResponse(
        Long id,
        String receiptNumber,
        BigDecimal amount,
        LocalDate paymentDate,
        String paymentMethod,
        Long enrolmentId,
        Long studentId,
        String studentName,
        String studentNic,
        Long courseId,
        String courseCode,
        String courseName,
        BigDecimal courseFee,
        BigDecimal totalPaid,
        BigDecimal balanceRemaining
) {
}

