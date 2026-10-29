package com.applebay.payment;

import java.math.BigDecimal;
import java.util.List;

public record EnrolmentPaymentsResponse(
        Long enrolmentId,
        Long studentId,
        String studentName,
        String studentNic,
        Long courseId,
        String courseCode,
        String courseName,
        BigDecimal courseFee,
        BigDecimal totalPaid,
        BigDecimal balanceRemaining,
        List<PaymentResponse> payments
) {
}

