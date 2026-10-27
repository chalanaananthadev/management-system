package com.applebay.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllByOrderByCourseCodeAsc();

    Optional<Course> findByCourseCodeIgnoreCase(String courseCode);

    boolean existsByCourseCodeIgnoreCase(String courseCode);

    boolean existsByCourseCodeIgnoreCaseAndIdNot(String courseCode, Long id);
}
