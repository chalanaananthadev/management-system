package com.applebay.student;

import com.applebay.attendance.AttendanceRepository;
import com.applebay.common.BadRequestException;
import com.applebay.common.NotFoundException;
import com.applebay.course.Course;
import com.applebay.course.CourseRepository;
import com.applebay.enrolment.Enrolment;
import com.applebay.enrolment.EnrolmentRepository;
import com.applebay.payment.Payment;
import com.applebay.payment.PaymentRepository;
import com.applebay.payment.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final CourseRepository courseRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    public StudentService(
            StudentRepository studentRepository,
            EnrolmentRepository enrolmentRepository,
            AttendanceRepository attendanceRepository,
            CourseRepository courseRepository,
            PaymentRepository paymentRepository,
            PaymentService paymentService
    ) {
        this.studentRepository = studentRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.courseRepository = courseRepository;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    public List<StudentResponse> list(String name) {
        List<Student> students = (name == null || name.isBlank())
                ? studentRepository.findAllByOrderByNameAsc()
                : studentRepository.findByNameContainingIgnoreCaseOrderByNameAsc(name.trim());
        return students.stream().map(StudentService::toResponse).toList();
    }

    public Student getEntity(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student not found."));
    }

    @Transactional
    public StudentResponse create(StudentRequest request) {
        String nic = normaliseNic(request.nic());
        if (studentRepository.existsByNicIgnoreCase(nic)) {
            throw new BadRequestException("A student with this NIC already exists.");
        }
        Student student = new Student();
        apply(student, request, nic);
        return toResponse(studentRepository.save(student));
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentOnboardingResponse onboard(StudentOnboardingRequest request) {
        String nic = normaliseNic(request.nic());
        if (studentRepository.existsByNicIgnoreCase(nic)) {
            throw new BadRequestException("A student with this NIC already exists.");
        }

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new NotFoundException("Course not found."));

        if (request.paymentAmount() == null || request.paymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("First payment amount must be greater than zero.");
        }

        if (request.paymentAmount().compareTo(course.getCourseFee()) > 0) {
            throw new BadRequestException("First payment amount (" + request.paymentAmount()
                    + ") exceeds course fee (" + course.getCourseFee() + ").");
        }

        // 1. Save Student
        Student student = new Student();
        apply(student, new StudentRequest(request.name(), request.nic(), request.email(), request.phone(), request.address()), nic);
        Student savedStudent = studentRepository.save(student);

        // 2. Save Enrolment
        Enrolment enrolment = new Enrolment();
        enrolment.setStudent(savedStudent);
        enrolment.setCourse(course);
        enrolment.setEnrolledOn(request.enrolledOn() != null ? request.enrolledOn() : LocalDate.now());
        Enrolment savedEnrolment = enrolmentRepository.save(enrolment);

        // 3. Save First Payment
        Payment payment = new Payment();
        payment.setEnrolment(savedEnrolment);
        payment.setAmount(request.paymentAmount());
        payment.setPaymentDate(request.paymentDate() != null ? request.paymentDate() : LocalDate.now());
        payment.setPaymentMethod(request.paymentMethod().trim());
        payment.setReceiptNumber(paymentService.generateReceiptNumber(payment.getPaymentDate().getYear()));
        Payment savedPayment = paymentRepository.save(payment);

        BigDecimal remainingBalance = course.getCourseFee().subtract(savedPayment.getAmount());

        return new StudentOnboardingResponse(
                savedStudent.getId(),
                savedStudent.getName(),
                savedStudent.getNic(),
                savedStudent.getEmail(),
                savedStudent.getPhone(),
                savedStudent.getAddress(),
                savedEnrolment.getId(),
                course.getId(),
                course.getCourseCode(),
                course.getName(),
                course.getCourseFee(),
                savedEnrolment.getEnrolledOn(),
                savedPayment.getId(),
                savedPayment.getReceiptNumber(),
                savedPayment.getAmount(),
                savedPayment.getPaymentDate(),
                savedPayment.getPaymentMethod(),
                savedPayment.getAmount(),
                remainingBalance
        );
    }

    @Transactional
    public StudentResponse update(Long id, StudentRequest request) {
        Student student = getEntity(id);
        String nic = normaliseNic(request.nic());
        if (studentRepository.existsByNicIgnoreCaseAndIdNot(nic, id)) {
            throw new BadRequestException("A student with this NIC already exists.");
        }
        apply(student, request, nic);
        return toResponse(studentRepository.save(student));
    }

    @Transactional
    public void delete(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new NotFoundException("Student not found.");
        }
        attendanceRepository.deleteByStudentId(id);
        List<Enrolment> enrolments = enrolmentRepository.findByStudentId(id);
        for (Enrolment e : enrolments) {
            paymentRepository.deleteByEnrolmentId(e.getId());
        }
        enrolmentRepository.deleteByStudentId(id);
        studentRepository.deleteById(id);
    }

    private void apply(Student student, StudentRequest request, String nic) {
        student.setName(request.name().trim());
        student.setNic(nic);
        student.setEmail(request.email().trim().toLowerCase());
        student.setPhone(request.phone().trim());
        student.setAddress(request.address().trim());
    }

    private String normaliseNic(String nic) {
        if (nic == null || nic.isBlank()) {
            throw new BadRequestException("NIC is required.");
        }
        return nic.trim().toUpperCase();
    }

    public static StudentResponse toResponse(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getName(),
                student.getNic(),
                student.getEmail(),
                student.getPhone(),
                student.getAddress()
        );
    }
}
