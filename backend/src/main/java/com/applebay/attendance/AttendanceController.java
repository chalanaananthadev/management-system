package com.applebay.attendance;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping
    public AttendanceSheetResponse sheet(
            @RequestParam Long courseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return attendanceService.sheet(courseId, date);
    }

    @PostMapping
    public AttendanceSheetResponse mark(@Valid @RequestBody MarkAttendanceRequest request) {
        return attendanceService.mark(request);
    }

    @GetMapping("/student/{studentId}")
    public StudentAttendanceResponse studentSummary(@PathVariable Long studentId) {
        return attendanceService.summary(studentId);
    }
}
