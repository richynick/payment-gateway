package com.richard.paymentgateway.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "reference_id", unique = true, nullable = false, length = 50)
    private String referenceId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;
    
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;
    
    @Column(name = "payment_provider", length = 50)
    private String paymentProvider;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;
    
    @Column(name = "idempotency_key", unique = true, length = 255)
    private String idempotencyKey;
    
    @Column(name = "fraud_score", precision = 3, scale = 2)
    private BigDecimal fraudScore;
    
    @Column(name = "error_code", length = 50)
    private String errorCode;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;
    
    @Column(name = "webhook_attempts")
    @Builder.Default
    private Integer webhookAttempts = 0;
    
    @Column(name = "webhook_last_attempt")
    private LocalDateTime webhookLastAttempt;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    public enum PaymentMethod {
        CARD, WALLET, BANK
    }
    
    public enum TransactionStatus {
        PENDING, PROCESSING, SUCCESS, FAILED, CANCELLED
    }
} 