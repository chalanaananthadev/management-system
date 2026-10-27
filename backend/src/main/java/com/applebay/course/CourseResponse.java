package com.applebay.course;

public record CourseResponse(
        Long id,
        String courseCode,
        String name,
        String level,
        String duration
) {
}
