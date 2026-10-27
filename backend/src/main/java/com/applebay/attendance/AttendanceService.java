package com.applebay.attendance;

import com.applebay.common.BadRequestException;
import com.applebay.course.Course;
import com.applebay.course.CourseService;
import com.applebay.enrolment.Enrolment;
import com.applebay.enrolment.EnrolmentRepository;
import com.applebay.student.Student;
import com.applebay.student.StudentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final CourseService courseService;
    private final StudentService studentService;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            EnrolmentRepository enrolmentRepository,
            CourseService courseService,
            StudentService studentService
    ) {
        this.attendanceRepository = attendanceRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.courseService = courseService;
        this.studentService = studentService;
    }

    @Transactional(readOnly = true)
    public AttendanceSheetResponse sheet(Long courseId, LocalDate date) {
        Course course = courseService.getEntity(courseId);
        List<Enrolment> enrolments = enrolmentRepository.findByCourseIdWithStudent(courseId);
        Map<Long, Attendance> marked = new LinkedHashMap<>();
        for (Attendance attendance : attendanceRepository.findByCourseIdAndAttendanceDate(courseId, date)) {
            marked.put(attendance.getStudent().getId(), attendance);
        }

        List<AttendanceSheetResponse.SheetStudent> students = enrolments.stream()
                .map(enrolment -> {
                    Student student = enrolment.getStudent();
                    Attendance attendance = marked.get(student.getId());
                    return new AttendanceSheetResponse.SheetStudent(
                            student.getId(),
                            student.getName(),
                            student.getNic(),
                            attendance == null ? null : attendance.getStatus().name()
                    );
                })
                .toList();

        return new AttendanceSheetResponse(
                course.getId(),
                course.getCourseCode(),
                course.getName(),
                date,
                students
        );
    }

    @Transactional
    public AttendanceSheetResponse mark(MarkAttendanceRequest request) {
        Course course = courseService.getEntity(request.courseId());
        for (MarkAttendanceRequest.Entry entry : request.records()) {
            Student student = studentService.getEntity(entry.studentId());
            if (!enrolmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
                throw new BadRequestException(student.getName() + " is not enrolled in " + course.getCourseCode() + ".");
            }
            Attendance attendance = attendanceRepository
                    .findByStudentIdAndCourseIdAndAttendanceDate(student.getId(), course.getId(), request.date())
                    .orElseGet(Attendance::new);
            attendance.setStudent(student);
            attendance.setCourse(course);
            attendance.setAttendanceDate(request.date());
            attendance.setStatus(parseStatus(entry.status()));
            attendanceRepository.save(attendance);
        }
        return sheet(course.getId(), request.date());
    }

    @Transactional(readOnly = true)
    public StudentAttendanceResponse summary(Long studentId) {
        Student student = studentService.getEntity(studentId);
        List<Attendance> records = attendanceRepository.findByStudentWithCourse(studentId);

        long presentCount = records.stream().filter(record -> record.getStatus() == AttendanceStatus.PRESENT).count();
        long total = records.size();

        Map<Long, List<Attendance>> grouped = new LinkedHashMap<>();
        for (Attendance record : records) {
            grouped.computeIfAbsent(record.getCourse().getId(), key -> new ArrayList<>()).add(record);
        }

        List<StudentAttendanceResponse.CourseBreakdown> courses = grouped.values().stream()
                .map(list -> {
                    Attendance sample = list.get(0);
                    long coursePresent = list.stream().filter(record -> record.getStatus() == AttendanceStatus.PRESENT).count();
                    return new StudentAttendanceResponse.CourseBreakdown(
                            sample.getCourse().getId(),
                            sample.getCourse().getCourseCode(),
                            sample.getCourse().getName(),
                            list.size(),
                            coursePresent,
                            percentage(coursePresent, list.size())
                    );
                })
                .sorted(Comparator.comparing(StudentAttendanceResponse.CourseBreakdown::courseCode))
                .toList();

        List<StudentAttendanceResponse.RecordItem> items = records.stream()
                .map(record -> new StudentAttendanceResponse.RecordItem(
                        record.getId(),
                        record.getCourse().getId(),
                        record.getCourse().getCourseCode(),
                        record.getCourse().getName(),
                        record.getAttendanceDate(),
                        record.getStatus().name()
                ))
                .toList();

        return new StudentAttendanceResponse(
                student.getId(),
                student.getName(),
                student.getNic(),
                total,
                presentCount,
                total - presentCount,
                percentage(presentCount, total),
                courses,
                items
        );
    }

    private AttendanceStatus parseStatus(String status) {
        try {
            return AttendanceStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BadRequestException("Status must be Present or Absent.");
        }
    }

    private Double percentage(long present, long total) {
        if (total == 0) {
            return null;
        }
        return Math.round((present * 1000.0) / total) / 10.0;
    }
}
