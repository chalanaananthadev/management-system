package com.applebay.student;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StudentOnboardingResponse(
        Long studentId,
        String studentName,
        String studentNic,
        String studentEmail,
        String studentPhone,
        String studentAddress,
        Long enrolmentId,
        Long courseId,
        String courseCode,
        String courseName,
        BigDecimal courseFee,
        LocalDate enrolledOn,
        Long paymentId,
        String receiptNumber,
        BigDecimal paymentAmount,
        LocalDate paymentDate,
        String paymentMethod,
        BigDecimal totalPaid,
        BigDecimal balanceRemaining
) {
}

