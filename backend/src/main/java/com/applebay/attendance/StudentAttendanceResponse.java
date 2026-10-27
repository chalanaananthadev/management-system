package com.applebay.attendance;

import java.time.LocalDate;
import java.util.List;

public record StudentAttendanceResponse(
        Long studentId,
        String studentName,
        String nic,
        long totalSessions,
        long presentCount,
        long absentCount,
        Double percentage,
        List<CourseBreakdown> courses,
        List<RecordItem> records
) {
    public record CourseBreakdown(
            Long courseId,
            String courseCode,
            String courseName,
            long totalSessions,
            long presentCount,
            Double percentage
    ) {
    }

    public record RecordItem(
            Long id,
            Long courseId,
            String courseCode,
            String courseName,
            LocalDate date,
            String status
    ) {
    }
}
