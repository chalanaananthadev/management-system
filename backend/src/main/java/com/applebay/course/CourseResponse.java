package com.applebay.course;

import java.math.BigDecimal;

public record CourseResponse(
        Long id,
        String courseCode,
        String name,
        String level,
        String duration,
        BigDecimal courseFee
) {
}
