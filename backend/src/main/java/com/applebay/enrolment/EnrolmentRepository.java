package com.applebay.enrolment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnrolmentRepository extends JpaRepository<Enrolment, Long> {

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    List<Enrolment> findByStudentId(Long studentId);

    @Query("""
            select e from Enrolment e
            join fetch e.student
            join fetch e.course
            order by e.enrolledOn desc, e.id desc
            """)
    List<Enrolment> findAllWithDetails();

    @Query("""
            select e from Enrolment e
            join fetch e.student
            where e.course.id = :courseId
            order by e.student.name asc
            """)
    List<Enrolment> findByCourseIdWithStudent(@Param("courseId") Long courseId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Enrolment e where e.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Enrolment e where e.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);
}
