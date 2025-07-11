package com.richard.paymentgateway.repository;

import com.richard.paymentgateway.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    List<AuditLog> findByTransactionId(UUID transactionId);
    
    Page<AuditLog> findByTransactionId(UUID transactionId, Pageable pageable);
    
    List<AuditLog> findByEventType(String eventType);
    
    List<AuditLog> findByUserId(UUID userId);
    
    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);
    
    @Query("SELECT a FROM AuditLog a WHERE a.transaction.id = :transactionId AND a.eventType = :eventType")
    List<AuditLog> findByTransactionIdAndEventType(@Param("transactionId") UUID transactionId,
                                                  @Param("eventType") String eventType);
    
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    List<AuditLog> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.createdAt BETWEEN :startDate AND :endDate")
    List<AuditLog> findByUserIdAndCreatedAtBetween(@Param("userId") UUID userId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT a FROM AuditLog a WHERE a.eventType = :eventType AND a.createdAt BETWEEN :startDate AND :endDate")
    List<AuditLog> findByEventTypeAndCreatedAtBetween(@Param("eventType") String eventType,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);
} 