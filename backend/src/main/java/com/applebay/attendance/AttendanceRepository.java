package com.applebay.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByStudentIdAndCourseIdAndAttendanceDate(
            Long studentId,
            Long courseId,
            LocalDate attendanceDate
    );

    @Query("""
            select a from Attendance a
            join fetch a.student
            where a.course.id = :courseId and a.attendanceDate = :attendanceDate
            """)
    List<Attendance> findByCourseIdAndAttendanceDate(
            @Param("courseId") Long courseId,
            @Param("attendanceDate") LocalDate attendanceDate
    );

    @Query("""
            select a from Attendance a
            join fetch a.course
            where a.student.id = :studentId
            order by a.attendanceDate desc, a.course.courseCode asc
            """)
    List<Attendance> findByStudentWithCourse(@Param("studentId") Long studentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Attendance a where a.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Attendance a where a.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);
}
