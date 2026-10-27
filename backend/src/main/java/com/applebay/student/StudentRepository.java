package com.applebay.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findAllByOrderByNameAsc();

    List<Student> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    boolean existsByNicIgnoreCase(String nic);

    boolean existsByNicIgnoreCaseAndIdNot(String nic, Long id);
}
