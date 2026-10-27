package com.applebay.enrolment;

import java.time.LocalDate;

public record EnrolmentResponse(
        Long id,
        Long studentId,
        String studentName,
        String studentNic,
        Long courseId,
        String courseCode,
        String courseName,
        LocalDate enrolledOn
) {
}
