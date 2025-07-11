package com.richard.paymentgateway.dto;

import com.richard.paymentgateway.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    
    private UUID id;
    private String referenceId;
    private UUID userId;
    private UUID merchantId;
    private BigDecimal amount;
    private String currency;
    private Transaction.PaymentMethod paymentMethod;
    private String paymentProvider;
    private Transaction.TransactionStatus status;
    private String description;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional response fields
    private String redirectUrl; // For 3D Secure or payment provider redirects
    private String paymentIntentId; // External payment provider ID
    private String clientSecret; // For client-side payment confirmation
    private Map<String, Object> metadata;
} 