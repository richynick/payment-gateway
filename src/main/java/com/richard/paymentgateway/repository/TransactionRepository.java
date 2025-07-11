package com.richard.paymentgateway.repository;

import com.richard.paymentgateway.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    
    Optional<Transaction> findByReferenceId(String referenceId);
    
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    
    List<Transaction> findByUserId(UUID userId);
    
    Page<Transaction> findByUserId(UUID userId, Pageable pageable);
    
    List<Transaction> findByMerchantId(UUID merchantId);
    
    Page<Transaction> findByMerchantId(UUID merchantId, Pageable pageable);
    
    List<Transaction> findByStatus(Transaction.TransactionStatus status);
    
    List<Transaction> findByUserIdAndStatus(UUID userId, Transaction.TransactionStatus status);
    
    List<Transaction> findByMerchantIdAndStatus(UUID merchantId, Transaction.TransactionStatus status);
    
    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findByUserIdAndCreatedAtBetween(@Param("userId") UUID userId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM Transaction t WHERE t.merchantId = :merchantId AND t.createdAt BETWEEN :startDate AND :endDate")
    List<Transaction> findByMerchantIdAndCreatedAtBetween(@Param("merchantId") UUID merchantId,
                                                         @Param("startDate") LocalDateTime startDate,
                                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.userId = :userId AND t.status = :status AND t.createdAt >= :since")
    long countByUserIdAndStatusSince(@Param("userId") UUID userId,
                                   @Param("status") Transaction.TransactionStatus status,
                                   @Param("since") LocalDateTime since);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.status = :status AND t.createdAt >= :since")
    java.math.BigDecimal sumAmountByUserIdAndStatusSince(@Param("userId") UUID userId,
                                                        @Param("status") Transaction.TransactionStatus status,
                                                        @Param("since") LocalDateTime since);
    
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    boolean existsByReferenceId(String referenceId);
} 