package com.applebay.attendance;

import com.applebay.common.BadRequestException;
import com.applebay.course.Course;
import com.applebay.course.CourseService;
import com.applebay.enrolment.EnrolmentRepository;
import com.applebay.student.Student;
import com.applebay.student.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EnrolmentRepository enrolmentRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private StudentService studentService;

    @InjectMocks
    private AttendanceService attendanceService;

    private Student student;
    private Course course;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);
        student.setName("Nethmi Perera");
        student.setNic("200145678901");
        student.setEmail("nethmi@applebay.edu");
        student.setPhone("0771234567");
        student.setAddress("Colombo");

        course = new Course();
        course.setId(10L);
        course.setCourseCode("HND-SE");
        course.setName("HND in Software Engineering");
        course.setLevel("HND");
        course.setDuration("2 years");
        course.setCourseFee(BigDecimal.valueOf(150000.00));
    }

    @Test
    @DisplayName("Attendance Percentage >= 80% marks student as eligible for exams")
    void testAttendancePercentage_AboveOrEqual80Percent_IsEligible() {
        // 8 present out of 10 sessions = 80.0% -> Eligible
        Attendance a1 = createAttendance(1L, AttendanceStatus.PRESENT);
        Attendance a2 = createAttendance(2L, AttendanceStatus.PRESENT);
        Attendance a3 = createAttendance(3L, AttendanceStatus.PRESENT);
        Attendance a4 = createAttendance(4L, AttendanceStatus.PRESENT);
        Attendance a5 = createAttendance(5L, AttendanceStatus.PRESENT);
        Attendance a6 = createAttendance(6L, AttendanceStatus.PRESENT);
        Attendance a7 = createAttendance(7L, AttendanceStatus.PRESENT);
        Attendance a8 = createAttendance(8L, AttendanceStatus.PRESENT);
        Attendance a9 = createAttendance(9L, AttendanceStatus.ABSENT);
        Attendance a10 = createAttendance(10L, AttendanceStatus.ABSENT);

        List<Attendance> records = List.of(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);

        when(studentService.getEntity(1L)).thenReturn(student);
        when(attendanceRepository.findByStudentWithCourse(1L)).thenReturn(records);

        StudentAttendanceResponse summary = attendanceService.summary(1L);

        assertEquals(10, summary.totalSessions());
        assertEquals(8, summary.presentCount());
        assertEquals(2, summary.absentCount());
        assertEquals(80.0, summary.percentage());
        assertNotNull(summary.isEligible());
        assertTrue(summary.isEligible(), "Student with 80.0% attendance must be eligible");

        StudentAttendanceResponse.CourseBreakdown breakdown = summary.courses().get(0);
        assertEquals(80.0, breakdown.percentage());
        assertTrue(breakdown.isEligible());
    }

    @Test
    @DisplayName("Attendance Percentage < 80% marks student as ineligible for exams")
    void testAttendancePercentage_Below80Percent_IsIneligible() {
        // 3 present out of 4 sessions = 75.0% -> Ineligible
        Attendance a1 = createAttendance(1L, AttendanceStatus.PRESENT);
        Attendance a2 = createAttendance(2L, AttendanceStatus.PRESENT);
        Attendance a3 = createAttendance(3L, AttendanceStatus.PRESENT);
        Attendance a4 = createAttendance(4L, AttendanceStatus.ABSENT);

        List<Attendance> records = List.of(a1, a2, a3, a4);

        when(studentService.getEntity(1L)).thenReturn(student);
        when(attendanceRepository.findByStudentWithCourse(1L)).thenReturn(records);

        StudentAttendanceResponse summary = attendanceService.summary(1L);

        assertEquals(4, summary.totalSessions());
        assertEquals(3, summary.presentCount());
        assertEquals(1, summary.absentCount());
        assertEquals(75.0, summary.percentage());
        assertNotNull(summary.isEligible());
        assertFalse(summary.isEligible(), "Student with 75.0% attendance must be ineligible (< 80%)");
    }

    @Test
    @DisplayName("Attendance summary with zero sessions returns null percentage and null eligibility")
    void testAttendanceSummary_ZeroSessions_ReturnsNullPercentageAndEligibility() {
        when(studentService.getEntity(1L)).thenReturn(student);
        when(attendanceRepository.findByStudentWithCourse(1L)).thenReturn(Collections.emptyList());

        StudentAttendanceResponse summary = attendanceService.summary(1L);

        assertEquals(0, summary.totalSessions());
        assertEquals(0, summary.presentCount());
        assertEquals(0, summary.absentCount());
        assertNull(summary.percentage());
        assertNull(summary.isEligible());
    }

    @Test
    @DisplayName("Marking attendance for student not enrolled in the course throws BadRequestException")
    void testMarkAttendance_NonEnrolledStudent_ThrowsBadRequestException() {
        when(courseService.getEntity(10L)).thenReturn(course);
        when(studentService.getEntity(1L)).thenReturn(student);
        when(enrolmentRepository.existsByStudentIdAndCourseId(1L, 10L)).thenReturn(false);

        MarkAttendanceRequest request = new MarkAttendanceRequest(
                10L,
                LocalDate.now(),
                List.of(new MarkAttendanceRequest.Entry(1L, "PRESENT"))
        );

        BadRequestException exception = assertThrows(BadRequestException.class, () -> attendanceService.mark(request));
        assertTrue(exception.getMessage().contains("is not enrolled in"));
    }

    @Test
    @DisplayName("Marking attendance for existing record updates status without duplicates")
    void testMarkAttendance_UpdatesExistingRecord() {
        LocalDate date = LocalDate.of(2026, 10, 20);
        when(courseService.getEntity(10L)).thenReturn(course);
        when(studentService.getEntity(1L)).thenReturn(student);
        when(enrolmentRepository.existsByStudentIdAndCourseId(1L, 10L)).thenReturn(true);

        Attendance existing = createAttendance(55L, AttendanceStatus.ABSENT);
        when(attendanceRepository.findByStudentIdAndCourseIdAndAttendanceDate(1L, 10L, date))
                .thenReturn(Optional.of(existing));
        when(enrolmentRepository.findByCourseIdWithStudent(10L)).thenReturn(Collections.emptyList());
        when(attendanceRepository.findByCourseIdAndAttendanceDate(10L, date)).thenReturn(Collections.emptyList());

        MarkAttendanceRequest request = new MarkAttendanceRequest(
                10L,
                date,
                List.of(new MarkAttendanceRequest.Entry(1L, "PRESENT"))
        );

        attendanceService.mark(request);

        assertEquals(AttendanceStatus.PRESENT, existing.getStatus());
        verify(attendanceRepository).save(existing);
    }

    private Attendance createAttendance(Long id, AttendanceStatus status) {
        Attendance attendance = new Attendance();
        attendance.setId(id);
        attendance.setStudent(student);
        attendance.setCourse(course);
        attendance.setAttendanceDate(LocalDate.of(2026, 10, 15));
        attendance.setStatus(status);
        return attendance;
    }
}

