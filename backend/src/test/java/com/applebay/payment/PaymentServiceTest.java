package com.applebay.payment;

import com.applebay.common.BadRequestException;
import com.applebay.course.Course;
import com.applebay.enrolment.Enrolment;
import com.applebay.enrolment.EnrolmentRepository;
import com.applebay.student.Student;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EnrolmentRepository enrolmentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Student student;
    private Course course;
    private Enrolment enrolment;

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

        enrolment = new Enrolment();
        enrolment.setId(100L);
        enrolment.setStudent(student);
        enrolment.setCourse(course);
        enrolment.setEnrolledOn(LocalDate.of(2026, 9, 1));
    }

    @Test
    @DisplayName("Recording a valid payment succeeds and reduces the balance")
    void testRecordPayment_ValidAmount_Success() {
        // Course fee: 150000.00, Existing payments: 50000.00 -> Balance: 100000.00
        Payment p1 = new Payment();
        p1.setId(1L);
        p1.setAmount(BigDecimal.valueOf(50000.00));
        p1.setEnrolment(enrolment);

        when(enrolmentRepository.findById(100L)).thenReturn(Optional.of(enrolment));
        when(paymentRepository.findByEnrolmentId(100L))
                .thenReturn(List.of(p1))
                .thenReturn(List.of(p1, createSavedPayment(2L, BigDecimal.valueOf(30000.00))));
        when(paymentRepository.findLatestReceiptNumber("REC-2026-")).thenReturn(Optional.of("REC-2026-0001"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        PaymentRequest request = new PaymentRequest(
                100L,
                BigDecimal.valueOf(30000.00),
                LocalDate.of(2026, 10, 29),
                "CARD"
        );

        PaymentResponse response = paymentService.recordPayment(request);

        assertNotNull(response);
        assertEquals("REC-2026-0002", response.receiptNumber());
        assertEquals(BigDecimal.valueOf(30000.00), response.amount());
        assertEquals(BigDecimal.valueOf(150000.00), response.courseFee());
        assertEquals(BigDecimal.valueOf(80000.00), response.totalPaid());
        assertEquals(BigDecimal.valueOf(70000.00), response.balanceRemaining());
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Payment amount exceeding remaining balance throws BadRequestException")
    void testRecordPayment_AmountExceedsRemainingBalance_ThrowsBadRequestException() {
        // Course fee: 150000.00, Existing payments: 120000.00 -> Balance: 30000.00
        Payment p1 = new Payment();
        p1.setAmount(BigDecimal.valueOf(120000.00));
        p1.setEnrolment(enrolment);

        when(enrolmentRepository.findById(100L)).thenReturn(Optional.of(enrolment));
        when(paymentRepository.findByEnrolmentId(100L)).thenReturn(List.of(p1));

        PaymentRequest request = new PaymentRequest(
                100L,
                BigDecimal.valueOf(40000.00), // 40000 > 30000 balance!
                LocalDate.of(2026, 10, 29),
                "CASH"
        );

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> paymentService.recordPayment(request));

        assertTrue(exception.getMessage().contains("exceeds remaining balance"));
    }

    @Test
    @DisplayName("Payment with zero or negative amount throws BadRequestException")
    void testRecordPayment_ZeroOrNegativeAmount_ThrowsBadRequestException() {
        PaymentRequest zeroRequest = new PaymentRequest(100L, BigDecimal.ZERO, LocalDate.now(), "CASH");
        PaymentRequest negRequest = new PaymentRequest(100L, BigDecimal.valueOf(-100.00), LocalDate.now(), "CASH");

        assertThrows(BadRequestException.class, () -> paymentService.recordPayment(zeroRequest));
        assertThrows(BadRequestException.class, () -> paymentService.recordPayment(negRequest));
    }

    @Test
    @DisplayName("Attempting to record payment on a fully paid course throws BadRequestException")
    void testRecordPayment_FullyPaid_ThrowsBadRequestException() {
        Payment p1 = new Payment();
        p1.setAmount(BigDecimal.valueOf(150000.00)); // Fully paid
        p1.setEnrolment(enrolment);

        when(enrolmentRepository.findById(100L)).thenReturn(Optional.of(enrolment));
        when(paymentRepository.findByEnrolmentId(100L)).thenReturn(List.of(p1));

        PaymentRequest request = new PaymentRequest(
                100L,
                BigDecimal.valueOf(1000.00),
                LocalDate.now(),
                "ONLINE"
        );

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> paymentService.recordPayment(request));

        assertTrue(exception.getMessage().contains("already fully paid"));
    }

    @Test
    @DisplayName("Receipt number auto-generation follows REC-YYYY-XXXX format sequentially")
    void testReceiptNumberGeneration_IncrementsSequence() {
        when(paymentRepository.findLatestReceiptNumber("REC-2026-")).thenReturn(Optional.empty());
        String firstReceipt = paymentService.generateReceiptNumber(2026);
        assertEquals("REC-2026-0001", firstReceipt);

        when(paymentRepository.findLatestReceiptNumber("REC-2026-")).thenReturn(Optional.of("REC-2026-0042"));
        String nextReceipt = paymentService.generateReceiptNumber(2026);
        assertEquals("REC-2026-0043", nextReceipt);
    }

    private Payment createSavedPayment(Long id, BigDecimal amount) {
        Payment p = new Payment();
        p.setId(id);
        p.setEnrolment(enrolment);
        p.setAmount(amount);
        p.setPaymentDate(LocalDate.of(2026, 10, 29));
        p.setPaymentMethod("CARD");
        p.setReceiptNumber("REC-2026-0002");
        return p;
    }
}

