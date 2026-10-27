package com.applebay.enrolment;

import com.applebay.common.BadRequestException;
import com.applebay.common.NotFoundException;
import com.applebay.course.Course;
import com.applebay.course.CourseService;
import com.applebay.student.Student;
import com.applebay.student.StudentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class EnrolmentService {

    private final EnrolmentRepository enrolmentRepository;
    private final StudentService studentService;
    private final CourseService courseService;

    public EnrolmentService(
            EnrolmentRepository enrolmentRepository,
            StudentService studentService,
            CourseService courseService
    ) {
        this.enrolmentRepository = enrolmentRepository;
        this.studentService = studentService;
        this.courseService = courseService;
    }

    public List<EnrolmentResponse> list() {
        return enrolmentRepository.findAllWithDetails().stream()
                .map(EnrolmentService::toResponse)
                .toList();
    }

    @Transactional
    public EnrolmentResponse create(EnrolmentRequest request) {
        Student student = studentService.getEntity(request.studentId());
        Course course = courseService.getEntity(request.courseId());
        if (enrolmentRepository.existsByStudentIdAndCourseId(student.getId(), course.getId())) {
            throw new BadRequestException(student.getName() + " is already enrolled in " + course.getCourseCode() + ".");
        }
        Enrolment enrolment = new Enrolment();
        enrolment.setStudent(student);
        enrolment.setCourse(course);
        enrolment.setEnrolledOn(request.enrolledOn() == null ? LocalDate.now() : request.enrolledOn());
        return toResponse(enrolmentRepository.save(enrolment));
    }

    @Transactional
    public void delete(Long id) {
        if (!enrolmentRepository.existsById(id)) {
            throw new NotFoundException("Enrolment not found.");
        }
        enrolmentRepository.deleteById(id);
    }

    public static EnrolmentResponse toResponse(Enrolment enrolment) {
        return new EnrolmentResponse(
                enrolment.getId(),
                enrolment.getStudent().getId(),
                enrolment.getStudent().getName(),
                enrolment.getStudent().getNic(),
                enrolment.getCourse().getId(),
                enrolment.getCourse().getCourseCode(),
                enrolment.getCourse().getName(),
                enrolment.getEnrolledOn()
        );
    }
}
