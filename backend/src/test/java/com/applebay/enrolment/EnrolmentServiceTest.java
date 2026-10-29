package com.applebay.enrolment;

import com.applebay.common.BadRequestException;
import com.applebay.course.Course;
import com.applebay.course.CourseService;
import com.applebay.payment.Payment;
import com.applebay.payment.PaymentRepository;
import com.applebay.payment.PaymentService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrolmentServiceTest {

    @Mock
    private EnrolmentRepository enrolmentRepository;

    @Mock
    private StudentService studentService;

    @Mock
    private CourseService courseService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private EnrolmentService enrolmentService;

    private Student student;
    private Course course;

    @BeforeEach
    void setUp() {
        student = new Student();
        student.setId(1L);
        student.setName("Nethmi Perera");
        student.setNic("200145678901");

        course = new Course();
        course.setId(10L);
        course.setCourseCode("HND-SE");
        course.setName("HND in Software Engineering");
        course.setCourseFee(BigDecimal.valueOf(150000.00));
    }

    @Test
    @DisplayName("Enrolling a student with valid first payment creates both enrolment and payment atomically")
    void testCreateEnrolment_WithValidFirstPayment_Success() {
        EnrolmentRequest request = new EnrolmentRequest(
                1L,
                10L,
                LocalDate.of(2026, 10, 29),
                BigDecimal.valueOf(50000.00),
                "BANK_TRANSFER",
                LocalDate.of(2026, 10, 29)
        );

        when(studentService.getEntity(1L)).thenReturn(student);
        when(courseService.getEntity(10L)).thenReturn(course);
        when(enrolmentRepository.existsByStudentIdAndCourseId(1L, 10L)).thenReturn(false);
        when(paymentService.generateReceiptNumber(2026)).thenReturn("REC-2026-0009");

        when(enrolmentRepository.save(any(Enrolment.class))).thenAnswer(inv -> {
            Enrolment e = inv.getArgument(0);
            e.setId(100L);
            return e;
        });

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(200L);
            return p;
        });

        when(paymentRepository.findByEnrolmentId(100L)).thenReturn(Collections.emptyList());

        EnrolmentResponse response = enrolmentService.create(request);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("REC-2026-0009", response.latestReceiptNumber());
        assertEquals(BigDecimal.valueOf(150000.00), response.courseFee());

        verify(enrolmentRepository).save(any(Enrolment.class));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Enrolling a student with payment exceeding course fee throws BadRequestException and cancels enrolment")
    void testCreateEnrolment_PaymentExceedsCourseFee_FailsAndRollsBack() {
        EnrolmentRequest request = new EnrolmentRequest(
                1L,
                10L,
                LocalDate.now(),
                BigDecimal.valueOf(200000.00), // Exceeds 150000.00
                "CASH",
                LocalDate.now()
        );

        when(studentService.getEntity(1L)).thenReturn(student);
        when(courseService.getEntity(10L)).thenReturn(course);
        when(enrolmentRepository.existsByStudentIdAndCourseId(1L, 10L)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> enrolmentService.create(request));

        assertTrue(exception.getMessage().contains("exceeds course fee"));
        // Enrolment must NOT be saved!
        verify(enrolmentRepository, never()).save(any(Enrolment.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Enrolling a student with zero or negative payment fails immediately")
    void testCreateEnrolment_ZeroOrNegativePayment_Fails() {
        EnrolmentRequest request = new EnrolmentRequest(
                1L,
                10L,
                LocalDate.now(),
                BigDecimal.ZERO,
                "CASH",
                LocalDate.now()
        );

        when(studentService.getEntity(1L)).thenReturn(student);
        when(courseService.getEntity(10L)).thenReturn(course);
        when(enrolmentRepository.existsByStudentIdAndCourseId(1L, 10L)).thenReturn(false);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> enrolmentService.create(request));

        assertTrue(exception.getMessage().contains("must be greater than zero"));
        verify(enrolmentRepository, never()).save(any(Enrolment.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("Attempting to enrol student already enrolled in course throws BadRequestException")
    void testCreateEnrolment_DuplicateEnrolment_Fails() {
        EnrolmentRequest request = new EnrolmentRequest(
                1L,
                10L,
                LocalDate.now(),
                BigDecimal.valueOf(50000.00),
                "CASH",
                LocalDate.now()
        );

        when(studentService.getEntity(1L)).thenReturn(student);
        when(courseService.getEntity(10L)).thenReturn(course);
        when(enrolmentRepository.existsByStudentIdAndCourseId(1L, 10L)).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> enrolmentService.create(request));

        assertTrue(exception.getMessage().contains("already enrolled"));
        verify(enrolmentRepository, never()).save(any(Enrolment.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }
}

