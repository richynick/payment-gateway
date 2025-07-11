package com.richard.paymentgateway.service;

import com.richard.paymentgateway.model.Transaction;
import com.richard.paymentgateway.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final TransactionRepository transactionRepository;
    
    @Value("${payment.idempotency.ttl-seconds:86400}")
    private long idempotencyTtlSeconds;
    
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    
    /**
     * Check if a request with the given idempotency key has already been processed
     * @param idempotencyKey The idempotency key to check
     * @return Optional containing the existing transaction if found
     */
    public Optional<Transaction> checkIdempotency(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        
        // First check Redis for fast lookup
        String existingTransactionId = redisTemplate.opsForValue().get(redisKey);
        if (existingTransactionId != null) {
            log.info("Found existing transaction in Redis for idempotency key: {}", idempotencyKey);
            return transactionRepository.findById(UUID.fromString(existingTransactionId));
        }
        
        // Fallback to database check
        Optional<Transaction> existingTransaction = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existingTransaction.isPresent()) {
            log.info("Found existing transaction in database for idempotency key: {}", idempotencyKey);
            // Cache the result in Redis
            redisTemplate.opsForValue().set(redisKey, existingTransaction.get().getId().toString(), 
                                          Duration.ofSeconds(idempotencyTtlSeconds));
        }
        
        return existingTransaction;
    }
    
    /**
     * Reserve an idempotency key to prevent duplicate processing
     * @param idempotencyKey The idempotency key to reserve
     * @param transactionId The transaction ID to associate with the key
     * @return true if the key was successfully reserved, false if it already exists
     */
    public boolean reserveIdempotencyKey(String idempotencyKey, UUID transactionId) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return true; // No idempotency key provided, allow processing
        }
        
        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        
        // Use Redis SET with NX (only if not exists) to atomically reserve the key
        Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, transactionId.toString(), 
                                                                 Duration.ofSeconds(idempotencyTtlSeconds));
        
        if (Boolean.TRUE.equals(success)) {
            log.info("Successfully reserved idempotency key: {} for transaction: {}", idempotencyKey, transactionId);
            return true;
        } else {
            log.warn("Idempotency key already exists: {}", idempotencyKey);
            return false;
        }
    }
    
    /**
     * Release an idempotency key reservation (used when transaction fails)
     * @param idempotencyKey The idempotency key to release
     */
    public void releaseIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return;
        }
        
        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        redisTemplate.delete(redisKey);
        log.info("Released idempotency key: {}", idempotencyKey);
    }
    
    /**
     * Generate a unique idempotency key if none is provided
     * @return A unique idempotency key
     */
    public String generateIdempotencyKey() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Check if an idempotency key already exists in the database
     * @param idempotencyKey The idempotency key to check
     * @return true if the key exists, false otherwise
     */
    public boolean existsByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return false;
        }
        
        return transactionRepository.existsByIdempotencyKey(idempotencyKey);
    }
} 