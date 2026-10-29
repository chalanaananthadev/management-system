package com.applebay.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM Payment p JOIN FETCH p.enrolment e JOIN FETCH e.student JOIN FETCH e.course WHERE e.id = :enrolmentId ORDER BY p.paymentDate DESC, p.id DESC")
    List<Payment> findByEnrolmentIdWithDetails(@Param("enrolmentId") Long enrolmentId);

    @Query("SELECT p FROM Payment p JOIN FETCH p.enrolment e JOIN FETCH e.student JOIN FETCH e.course ORDER BY p.paymentDate DESC, p.id DESC")
    List<Payment> findAllWithDetails();

    List<Payment> findByEnrolmentId(Long enrolmentId);

    @Query(value = "SELECT p.receipt_number FROM payments p WHERE p.receipt_number LIKE :prefix || '%' ORDER BY p.id DESC LIMIT 1", nativeQuery = true)
    Optional<String> findLatestReceiptNumber(@Param("prefix") String prefix);

    void deleteByEnrolmentId(Long enrolmentId);
}

