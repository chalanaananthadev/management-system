package com.applebay.enrolment;

import com.applebay.common.BadRequestException;
import com.applebay.common.NotFoundException;
import com.applebay.course.Course;
import com.applebay.course.CourseService;
import com.applebay.payment.Payment;
import com.applebay.payment.PaymentRepository;
import com.applebay.payment.PaymentService;
import com.applebay.student.Student;
import com.applebay.student.StudentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class EnrolmentService {

    private final EnrolmentRepository enrolmentRepository;
    private final StudentService studentService;
    private final CourseService courseService;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    public EnrolmentService(
            EnrolmentRepository enrolmentRepository,
            StudentService studentService,
            CourseService courseService,
            PaymentRepository paymentRepository,
            PaymentService paymentService
    ) {
        this.enrolmentRepository = enrolmentRepository;
        this.studentService = studentService;
        this.courseService = courseService;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    public List<EnrolmentResponse> list() {
        return enrolmentRepository.findAllWithDetails().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public EnrolmentResponse create(EnrolmentRequest request) {
        Student student = studentService.getEntity(request.studentId());
        Course course = courseService.getEntity(request.courseId());

        if (enrolmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw new BadRequestException(student.getName() + " is already enrolled in " + course.getCourseCode() + ".");
        }

        // Enforce First Payment: Must be paid upon enrolment or transaction rolls back!
        if (request.paymentAmount() == null || request.paymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("First payment amount must be greater than zero. Enrolment cannot be completed without initial payment.");
        }

        BigDecimal courseFee = course.getCourseFee();
        if (request.paymentAmount().compareTo(courseFee) > 0) {
            throw new BadRequestException("First payment amount (" + request.paymentAmount()
                    + ") exceeds course fee (" + courseFee + "). Enrolment cancelled and rolled back.");
        }

        // 1. Create and save Enrolment
        Enrolment enrolment = new Enrolment();
        enrolment.setStudent(student);
        enrolment.setCourse(course);
        enrolment.setEnrolledOn(request.enrolledOn() == null ? LocalDate.now() : request.enrolledOn());
        Enrolment savedEnrolment = enrolmentRepository.save(enrolment);

        // 2. Create and save First Payment
        Payment payment = new Payment();
        payment.setEnrolment(savedEnrolment);
        payment.setAmount(request.paymentAmount());
        payment.setPaymentDate(request.paymentDate() != null ? request.paymentDate() : LocalDate.now());
        payment.setPaymentMethod(request.paymentMethod() != null ? request.paymentMethod().trim() : "CASH");
        payment.setReceiptNumber(paymentService.generateReceiptNumber(payment.getPaymentDate().getYear()));
        Payment savedPayment = paymentRepository.save(payment);

        return toResponse(savedEnrolment, savedPayment.getReceiptNumber());
    }

    @Transactional
    public void delete(Long id) {
        if (!enrolmentRepository.existsById(id)) {
            throw new NotFoundException("Enrolment not found.");
        }
        paymentRepository.deleteByEnrolmentId(id);
        enrolmentRepository.deleteById(id);
    }

    public EnrolmentResponse toResponse(Enrolment enrolment) {
        return toResponse(enrolment, null);
    }

    public EnrolmentResponse toResponse(Enrolment enrolment, String defaultReceipt) {
        BigDecimal courseFee = enrolment.getCourse().getCourseFee();
        List<Payment> payments = paymentRepository.findByEnrolmentId(enrolment.getId());
        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal balance = courseFee.subtract(totalPaid);

        String receipt = defaultReceipt;
        if (receipt == null && !payments.isEmpty()) {
            receipt = payments.get(payments.size() - 1).getReceiptNumber();
        }

        return new EnrolmentResponse(
                enrolment.getId(),
                enrolment.getStudent().getId(),
                enrolment.getStudent().getName(),
                enrolment.getStudent().getNic(),
                enrolment.getStudent().getEmail(),
                enrolment.getCourse().getId(),
                enrolment.getCourse().getCourseCode(),
                enrolment.getCourse().getName(),
                enrolment.getEnrolledOn(),
                courseFee,
                totalPaid,
                balance,
                receipt
        );
    }
}
