package com.applebay.student;

import com.applebay.attendance.AttendanceRepository;
import com.applebay.common.BadRequestException;
import com.applebay.common.NotFoundException;
import com.applebay.enrolment.EnrolmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final AttendanceRepository attendanceRepository;

    public StudentService(
            StudentRepository studentRepository,
            EnrolmentRepository enrolmentRepository,
            AttendanceRepository attendanceRepository
    ) {
        this.studentRepository = studentRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.attendanceRepository = attendanceRepository;
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
