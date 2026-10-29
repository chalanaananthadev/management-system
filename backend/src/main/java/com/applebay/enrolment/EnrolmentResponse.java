package com.applebay.enrolment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EnrolmentResponse(
        Long id,
        Long studentId,
        String studentName,
        String studentNic,
        String studentEmail,
        Long courseId,
        String courseCode,
        String courseName,
        LocalDate enrolledOn,
        BigDecimal courseFee,
        BigDecimal totalPaid,
        BigDecimal balanceRemaining,
        String latestReceiptNumber
) {
}
