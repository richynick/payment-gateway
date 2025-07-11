package com.richard.paymentgateway.controller;

import com.richard.paymentgateway.dto.PaymentRequest;
import com.richard.paymentgateway.dto.PaymentResponse;
import com.richard.paymentgateway.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;

    /**
     * Initiate a payment request
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Get payment status by transaction ID (UUID or referenceId)
     */
    @GetMapping("/status/{id}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(@PathVariable("id") String id) {
        Optional<PaymentResponse> response;
        try {
            // Try as UUID first
            response = paymentService.getPaymentStatus(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            // Fallback to referenceId
            response = paymentService.getPaymentStatus(id);
        }
        return response.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
} 