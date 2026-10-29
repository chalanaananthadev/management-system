package com.applebay.student;

import com.applebay.attendance.AttendanceRepository;
import com.applebay.common.BadRequestException;
import com.applebay.course.Course;
import com.applebay.course.CourseRepository;
import com.applebay.enrolment.Enrolment;
import com.applebay.enrolment.EnrolmentRepository;
import com.applebay.payment.Payment;
import com.applebay.payment.PaymentRepository;
import com.applebay.payment.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentOnboardingServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private EnrolmentRepository enrolmentRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private StudentService studentService;

    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(1L);
        course.setCourseCode("HND-SE");
        course.setName("HND in Software Engineering");
        course.setLevel("HND");
        course.setDuration("2 years");
        course.setCourseFee(BigDecimal.valueOf(150000.00));
    }

    @Test
    @DisplayName("Atomic Onboarding succeeds when student, enrolment and first payment are valid")
    void testOnboardStudent_Success() {
        StudentOnboardingRequest request = new StudentOnboardingRequest(
                "Ruwan Silva",
                "199812345678",
                "ruwan@applebay.edu",
                "0771122334",
                "77 Galle Road",
                1L,
                LocalDate.of(2026, 10, 29),
                BigDecimal.valueOf(50000.00),
                "BANK_TRANSFER",
                LocalDate.of(2026, 10, 29)
        );

        when(studentRepository.existsByNicIgnoreCase("199812345678")).thenReturn(false);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(paymentService.generateReceiptNumber(2026)).thenReturn("REC-2026-0007");

        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(101L);
            return s;
        });

        when(enrolmentRepository.save(any(Enrolment.class))).thenAnswer(inv -> {
            Enrolment e = inv.getArgument(0);
            e.setId(205L);
            return e;
        });

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(301L);
            return p;
        });

        StudentOnboardingResponse response = studentService.onboard(request);

        assertNotNull(response);
        assertEquals(101L, response.studentId());
        assertEquals("Ruwan Silva", response.studentName());
        assertEquals("199812345678", response.studentNic());
        assertEquals(205L, response.enrolmentId());
        assertEquals("REC-2026-0007", response.receiptNumber());
        assertEquals(BigDecimal.valueOf(50000.00), response.paymentAmount());
        assertEquals(BigDecimal.valueOf(100000.00), response.balanceRemaining());

        verify(studentRepository).save(any(Student.class));
        verify(enrolmentRepository).save(any(Enrolment.class));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("First payment amount exceeding course fee triggers BadRequestException and prevents saving")
    void testOnboardStudent_PaymentExceedsCourseFee_FailsAndRollsBack() {
        StudentOnboardingRequest request = new StudentOnboardingRequest(
                "Kasun Perera",
                "199999887766",
                "kasun@applebay.edu",
                "0719998888",
                "Kandy",
                1L,
                LocalDate.now(),
                BigDecimal.valueOf(200000.00), // Exceeds 150000.00 course fee!
                "CASH",
                LocalDate.now()
        );

        when(studentRepository.existsByNicIgnoreCase("199999887766")).thenReturn(false);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> studentService.onboard(request));

        assertTrue(exception.getMessage().contains("exceeds course fee"));
        // Confirm nothing is saved
        verify(studentRepository, never()).save(any(Student.class));
        verify(enrolmentRepository, never()).save(any(Enrolment.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Onboarding with existing student NIC fails before any enrolment or payment")
    void testOnboardStudent_DuplicateNic_FailsEarly() {
        StudentOnboardingRequest request = new StudentOnboardingRequest(
                "Duplicate NIC",
                "200145678901",
                "dup@applebay.edu",
                "0771234567",
                "Colombo",
                1L,
                LocalDate.now(),
                BigDecimal.valueOf(50000.00),
                "CASH",
                LocalDate.now()
        );

        when(studentRepository.existsByNicIgnoreCase("200145678901")).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> studentService.onboard(request));

        assertTrue(exception.getMessage().contains("already exists"));
        verify(studentRepository, never()).save(any(Student.class));
        verify(enrolmentRepository, never()).save(any(Enrolment.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}

