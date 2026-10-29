package com.applebay.payment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/payments")
    public List<PaymentResponse> listAll() {
        return paymentService.listAll();
    }

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse recordPayment(@Valid @RequestBody PaymentRequest request) {
        return paymentService.recordPayment(request);
    }

    @GetMapping("/payments/{id}")
    public PaymentResponse getPaymentById(@PathVariable Long id) {
        return paymentService.getById(id);
    }

    @PostMapping("/payments/{id}/email")
    public SendEbillResponse sendEbill(
            @PathVariable Long id,
            @RequestBody(required = false) SendEbillRequest request
    ) {
        return paymentService.sendEbill(id, request);
    }

    @GetMapping("/enrolments/{id}/payments")
    public EnrolmentPaymentsResponse getPaymentsByEnrolment(@PathVariable Long id) {
        return paymentService.getPaymentsForEnrolment(id);
    }
}

