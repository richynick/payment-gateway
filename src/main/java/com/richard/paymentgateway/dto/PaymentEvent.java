package com.richard.paymentgateway.dto;

import com.richard.paymentgateway.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    
    private UUID transactionId;
    private String referenceId;
    private UUID userId;
    private UUID merchantId;
    private BigDecimal amount;
    private String currency;
    private Transaction.PaymentMethod paymentMethod;
    private String paymentProvider;
    private Transaction.TransactionStatus status;
    private String description;
    private String idempotencyKey;
    private BigDecimal fraudScore;
    private String errorCode;
    private String errorMessage;
    private String webhookUrl;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum EventType {
        PAYMENT_INITIATED,
        PAYMENT_PROCESSED,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        WEBHOOK_SENT,
        WEBHOOK_FAILED
    }
    
    private EventType eventType;
    private LocalDateTime eventTimestamp;
} 