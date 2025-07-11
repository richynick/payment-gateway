package com.richard.paymentgateway.repository;

import com.richard.paymentgateway.model.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {
    
    List<WebhookEvent> findByTransactionId(UUID transactionId);
    
    @Query("SELECT w FROM WebhookEvent w WHERE w.nextRetryAt <= :now AND w.attempts < w.maxAttempts")
    List<WebhookEvent> findPendingRetries(@Param("now") LocalDateTime now);
    
    @Query("SELECT w FROM WebhookEvent w WHERE w.transaction.id = :transactionId AND w.attempts < w.maxAttempts")
    List<WebhookEvent> findPendingByTransactionId(@Param("transactionId") UUID transactionId);
    
    @Query("SELECT w FROM WebhookEvent w WHERE w.responseStatus = :status")
    List<WebhookEvent> findByResponseStatus(@Param("status") Integer status);
    
    @Query("SELECT w FROM WebhookEvent w WHERE w.attempts >= w.maxAttempts")
    List<WebhookEvent> findFailedWebhooks();
    
    @Query("SELECT w FROM WebhookEvent w WHERE w.createdAt BETWEEN :startDate AND :endDate")
    List<WebhookEvent> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
} 