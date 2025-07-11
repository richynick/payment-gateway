package com.richard.paymentgateway.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;
    
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", columnDefinition = "jsonb")
    private String eventData;
    
    @Column(name = "user_id")
    private UUID userId;
    
    @Column(name = "ip_address")
    private InetAddress ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    public enum EventType {
        PAYMENT_INITIATED,
        PAYMENT_PROCESSED,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        WEBHOOK_SENT,
        WEBHOOK_FAILED,
        FRAUD_CHECK,
        IDEMPOTENCY_CHECK
    }
} 