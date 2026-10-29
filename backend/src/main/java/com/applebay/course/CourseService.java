package com.applebay.course;

import com.applebay.attendance.AttendanceRepository;
import com.applebay.common.BadRequestException;
import com.applebay.common.NotFoundException;
import com.applebay.enrolment.EnrolmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final AttendanceRepository attendanceRepository;

    public CourseService(
            CourseRepository courseRepository,
            EnrolmentRepository enrolmentRepository,
            AttendanceRepository attendanceRepository
    ) {
        this.courseRepository = courseRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.attendanceRepository = attendanceRepository;
    }

    public List<CourseResponse> list() {
        return courseRepository.findAllByOrderByCourseCodeAsc().stream()
                .map(CourseService::toResponse)
                .toList();
    }

    public Course getEntity(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Course not found."));
    }

    @Transactional
    public CourseResponse create(CourseRequest request) {
        String code = normaliseCode(request.courseCode());
        if (courseRepository.existsByCourseCodeIgnoreCase(code)) {
            throw new BadRequestException("A course with this code already exists.");
        }
        Course course = new Course();
        apply(course, request, code);
        return toResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse update(Long id, CourseRequest request) {
        Course course = getEntity(id);
        String code = normaliseCode(request.courseCode());
        if (courseRepository.existsByCourseCodeIgnoreCaseAndIdNot(code, id)) {
            throw new BadRequestException("A course with this code already exists.");
        }
        apply(course, request, code);
        return toResponse(courseRepository.save(course));
    }

    @Transactional
    public void delete(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new NotFoundException("Course not found.");
        }
        attendanceRepository.deleteByCourseId(id);
        enrolmentRepository.deleteByCourseId(id);
        courseRepository.deleteById(id);
    }

    private void apply(Course course, CourseRequest request, String code) {
        course.setCourseCode(code);
        course.setName(request.name().trim());
        course.setLevel(request.level().trim());
        course.setDuration(request.duration().trim());
        course.setCourseFee(request.courseFee() != null ? request.courseFee() : java.math.BigDecimal.valueOf(50000.00));
    }

    private String normaliseCode(String courseCode) {
        if (courseCode == null || courseCode.isBlank()) {
            throw new BadRequestException("Course code is required.");
        }
        return courseCode.trim().toUpperCase();
    }

    public static CourseResponse toResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getCourseCode(),
                course.getName(),
                course.getLevel(),
                course.getDuration(),
                course.getCourseFee()
        );
    }
}
