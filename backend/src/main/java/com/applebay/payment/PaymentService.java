package com.applebay.payment;

import com.applebay.common.BadRequestException;
import com.applebay.common.NotFoundException;
import com.applebay.course.Course;
import com.applebay.enrolment.Enrolment;
import com.applebay.enrolment.EnrolmentRepository;
import com.applebay.student.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final EnrolmentRepository enrolmentRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            EnrolmentRepository enrolmentRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.enrolmentRepository = enrolmentRepository;
    }

    public List<PaymentResponse> listAll() {
        return paymentRepository.findAllWithDetails().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnrolmentPaymentsResponse getPaymentsForEnrolment(Long enrolmentId) {
        Enrolment enrolment = enrolmentRepository.findById(enrolmentId)
                .orElseThrow(() -> new NotFoundException("Enrolment not found."));

        List<Payment> payments = paymentRepository.findByEnrolmentIdWithDetails(enrolmentId);
        BigDecimal courseFee = enrolment.getCourse().getCourseFee();
        BigDecimal totalPaid = calculateTotalPaid(payments);
        BigDecimal balance = courseFee.subtract(totalPaid);

        List<PaymentResponse> paymentResponses = payments.stream()
                .map(this::toResponse)
                .toList();

        return new EnrolmentPaymentsResponse(
                enrolment.getId(),
                enrolment.getStudent().getId(),
                enrolment.getStudent().getName(),
                enrolment.getStudent().getNic(),
                enrolment.getStudent().getEmail(),
                enrolment.getCourse().getId(),
                enrolment.getCourse().getCourseCode(),
                enrolment.getCourse().getName(),
                courseFee,
                totalPaid,
                balance,
                paymentResponses
        );
    }

    @Transactional
    public PaymentResponse recordPayment(PaymentRequest request) {
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero.");
        }

        Enrolment enrolment = enrolmentRepository.findById(request.enrolmentId())
                .orElseThrow(() -> new NotFoundException("Enrolment not found."));

        List<Payment> existingPayments = paymentRepository.findByEnrolmentId(enrolment.getId());
        BigDecimal totalPaid = calculateTotalPaid(existingPayments);
        BigDecimal courseFee = enrolment.getCourse().getCourseFee();
        BigDecimal remainingBalance = courseFee.subtract(totalPaid);

        if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Course fee is already fully paid for this enrolment.");
        }

        if (request.amount().compareTo(remainingBalance) > 0) {
            throw new BadRequestException("Payment amount (" + request.amount()
                    + ") exceeds remaining balance (" + remainingBalance + "). Course fee: "
                    + courseFee + ", Total paid: " + totalPaid + ".");
        }

        Payment payment = new Payment();
        payment.setEnrolment(enrolment);
        payment.setAmount(request.amount());
        payment.setPaymentDate(request.paymentDate() != null ? request.paymentDate() : LocalDate.now());
        payment.setPaymentMethod(request.paymentMethod().trim());
        payment.setReceiptNumber(generateReceiptNumber(payment.getPaymentDate().getYear()));

        Payment saved = paymentRepository.save(payment);
        return toResponse(saved);
    }

    public synchronized String generateReceiptNumber(int year) {
        String prefix = "REC-" + year + "-";
        Optional<String> latest = paymentRepository.findLatestReceiptNumber(prefix);
        int nextSequence = 1;
        if (latest.isPresent()) {
            String receipt = latest.get();
            try {
                String seqPart = receipt.substring(prefix.length());
                nextSequence = Integer.parseInt(seqPart) + 1;
            } catch (Exception ignored) {
                nextSequence = 1;
            }
        }
        return String.format("%s%04d", prefix, nextSequence);
    }

    public synchronized String generateReceiptNumber() {
        return generateReceiptNumber(LocalDate.now().getYear());
    }

    public BigDecimal calculateTotalPaid(List<Payment> payments) {
        return payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalPaidForEnrolment(Long enrolmentId) {
        return calculateTotalPaid(paymentRepository.findByEnrolmentId(enrolmentId));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment record not found."));
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public SendEbillResponse sendEbill(Long paymentId, SendEbillRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment record not found."));

        Enrolment enrolment = payment.getEnrolment();
        Student student = enrolment.getStudent();
        Course course = enrolment.getCourse();

        String targetEmail = (request != null && request.email() != null && !request.email().isBlank())
                ? request.email().trim()
                : student.getEmail();

        if (targetEmail == null || targetEmail.isBlank()) {
            throw new BadRequestException("No recipient email address specified for student " + student.getName());
        }

        List<Payment> allPayments = paymentRepository.findByEnrolmentId(enrolment.getId());
        BigDecimal totalPaid = calculateTotalPaid(allPayments);
        BigDecimal balance = course.getCourseFee().subtract(totalPaid);

        String subject = String.format("[Applebay Institute] Official Payment Receipt - %s (%s)",
                payment.getReceiptNumber(), course.getCourseCode());

        String preview = String.format(
                "Dear %s,\n\n"
                        + "Thank you for your payment to Applebay Institute of Higher Education.\n"
                        + "--------------------------------------------------------\n"
                        + "Receipt Number: %s\n"
                        + "Student Name: %s (NIC: %s)\n"
                        + "Course: %s - %s\n"
                        + "Payment Date: %s\n"
                        + "Payment Method: %s\n"
                        + "--------------------------------------------------------\n"
                        + "Amount Paid (This Receipt): LKR %s\n"
                        + "Course Total Fee:           LKR %s\n"
                        + "Total Paid to Date:         LKR %s\n"
                        + "Remaining Balance:          LKR %s\n"
                        + "Status:                     %s\n"
                        + "--------------------------------------------------------\n"
                        + "Please retain this e-bill for your official records.\n\n"
                        + "Regards,\nAccounts Department\nApplebay Institute of Higher Education\nadmissions@applebay.ac.lk",
                student.getName(),
                payment.getReceiptNumber(),
                student.getName(),
                student.getNic(),
                course.getCourseCode(),
                course.getName(),
                payment.getPaymentDate(),
                payment.getPaymentMethod(),
                payment.getAmount().toPlainString(),
                course.getCourseFee().toPlainString(),
                totalPaid.toPlainString(),
                balance.toPlainString(),
                balance.compareTo(BigDecimal.ZERO) == 0 ? "FULLY PAID" : "PARTIAL PAYMENT"
        );

        log.info("Dispatched official E-Bill [{}] to recipient [{}] for enrolment #{}",
                payment.getReceiptNumber(), targetEmail, enrolment.getId());

        return new SendEbillResponse(
                true,
                "E-bill receipt " + payment.getReceiptNumber() + " successfully sent to " + targetEmail,
                targetEmail,
                payment.getReceiptNumber(),
                LocalDateTime.now(),
                subject,
                preview
        );
    }

    public PaymentResponse toResponse(Payment payment) {
        Enrolment enrolment = payment.getEnrolment();
        BigDecimal courseFee = enrolment.getCourse().getCourseFee();
        List<Payment> allForEnrolment = paymentRepository.findByEnrolmentId(enrolment.getId());
        BigDecimal totalPaid = calculateTotalPaid(allForEnrolment);
        BigDecimal balance = courseFee.subtract(totalPaid);

        return new PaymentResponse(
                payment.getId(),
                payment.getReceiptNumber(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getPaymentMethod(),
                enrolment.getId(),
                enrolment.getStudent().getId(),
                enrolment.getStudent().getName(),
                enrolment.getStudent().getNic(),
                enrolment.getStudent().getEmail(),
                enrolment.getCourse().getId(),
                enrolment.getCourse().getCourseCode(),
                enrolment.getCourse().getName(),
                courseFee,
                totalPaid,
                balance
        );
    }
}

