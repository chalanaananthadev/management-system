package com.applebay.attendance;

import java.time.LocalDate;
import java.util.List;

public record AttendanceSheetResponse(
        Long courseId,
        String courseCode,
        String courseName,
        LocalDate date,
        List<SheetStudent> students
) {
    public record SheetStudent(
            Long studentId,
            String studentName,
            String nic,
            String status
    ) {
    }
}
