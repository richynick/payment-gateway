package com.richard.paymentgateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.paymentgateway.dto.PaymentEvent;
import com.richard.paymentgateway.dto.PaymentRequest;
import com.richard.paymentgateway.dto.PaymentResponse;
import com.richard.paymentgateway.model.AuditLog;
import com.richard.paymentgateway.model.Transaction;
import com.richard.paymentgateway.model.WebhookEvent;
import com.richard.paymentgateway.repository.AuditLogRepository;
import com.richard.paymentgateway.repository.TransactionRepository;
import com.richard.paymentgateway.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final IdempotencyService idempotencyService;
    private final FraudDetectionService fraudDetectionService;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";
    private static final String PAYMENT_RESULTS_TOPIC = "payment-results";
    
    /**
     * Initiate a new payment transaction
     * @param request The payment request
     * @return PaymentResponse with transaction details
     */
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("Initiating payment for user: {}, amount: {} {}", 
                request.getUserId(), request.getAmount(), request.getCurrency());
        
        // Check idempotency
        String idempotencyKey = request.getIdempotencyKey();
        if (idempotencyKey == null) {
            idempotencyKey = idempotencyService.generateIdempotencyKey();
        }
        
        Optional<Transaction> existingTransaction = idempotencyService.checkIdempotency(idempotencyKey);
        if (existingTransaction.isPresent()) {
            log.info("Duplicate payment request detected for idempotency key: {}", idempotencyKey);
            return mapToPaymentResponse(existingTransaction.get());
        }
        
        // Validate payment request
        validatePaymentRequest(request);
        
        // Create transaction
        Transaction transaction = createTransaction(request, idempotencyKey);
        
        // Reserve idempotency key
        if (!idempotencyService.reserveIdempotencyKey(idempotencyKey, transaction.getId())) {
            throw new RuntimeException("Failed to reserve idempotency key: " + idempotencyKey);
        }
        
        // Perform fraud check
        BigDecimal fraudScore = fraudDetectionService.calculateFraudScore(request);
        transaction.setFraudScore(fraudScore);
        
        // Save transaction
        transaction = transactionRepository.save(transaction);
        
        // Log audit event
        logAuditEvent(transaction, AuditLog.EventType.PAYMENT_INITIATED, null);
        
        // Publish to Kafka for async processing
        publishPaymentEvent(transaction, PaymentEvent.EventType.PAYMENT_INITIATED);
        
        log.info("Payment initiated successfully. Transaction ID: {}, Reference: {}", 
                transaction.getId(), transaction.getReferenceId());
        
        return mapToPaymentResponse(transaction);
    }
    
    /**
     * Process a payment transaction asynchronously
     * @param transactionId The transaction ID to process
     */
    @Transactional
    public void processPayment(UUID transactionId) {
        log.info("Processing payment for transaction: {}", transactionId);
        
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        
        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            log.warn("Transaction {} is not in PENDING status: {}", transactionId, transaction.getStatus());
            return;
        }
        
        try {
            // Update status to PROCESSING
            transaction.setStatus(Transaction.TransactionStatus.PROCESSING);
            transaction = transactionRepository.save(transaction);
            
            // Log audit event
            logAuditEvent(transaction, AuditLog.EventType.PAYMENT_PROCESSED, null);
            
            // Simulate payment processing (replace with actual payment provider integration)
            boolean paymentSuccess = processPaymentWithProvider(transaction);
            
            if (paymentSuccess) {
                transaction.setStatus(Transaction.TransactionStatus.SUCCESS);
                logAuditEvent(transaction, AuditLog.EventType.PAYMENT_SUCCESS, null);
                publishPaymentEvent(transaction, PaymentEvent.EventType.PAYMENT_SUCCESS);
            } else {
                transaction.setStatus(Transaction.TransactionStatus.FAILED);
                transaction.setErrorCode("PAYMENT_FAILED");
                transaction.setErrorMessage("Payment processing failed");
                logAuditEvent(transaction, AuditLog.EventType.PAYMENT_FAILED, null);
                publishPaymentEvent(transaction, PaymentEvent.EventType.PAYMENT_FAILED);
            }
            
            transaction = transactionRepository.save(transaction);
            
            // Send webhook if configured
            if (transaction.getWebhookUrl() != null) {
                sendWebhook(transaction);
            }
            
        } catch (Exception e) {
            log.error("Error processing payment for transaction: {}", transactionId, e);
            transaction.setStatus(Transaction.TransactionStatus.FAILED);
            transaction.setErrorCode("PROCESSING_ERROR");
            transaction.setErrorMessage(e.getMessage());
            transaction = transactionRepository.save(transaction);
            logAuditEvent(transaction, AuditLog.EventType.PAYMENT_FAILED, e.getMessage());
            publishPaymentEvent(transaction, PaymentEvent.EventType.PAYMENT_FAILED);
        }
    }
    
    /**
     * Get payment status by reference ID
     * @param referenceId The reference ID
     * @return Optional PaymentResponse
     */
    public Optional<PaymentResponse> getPaymentStatus(String referenceId) {
        return transactionRepository.findByReferenceId(referenceId)
                .map(this::mapToPaymentResponse);
    }
    
    /**
     * Get payment status by transaction ID
     * @param transactionId The transaction ID
     * @return Optional PaymentResponse
     */
    public Optional<PaymentResponse> getPaymentStatus(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .map(this::mapToPaymentResponse);
    }
    
    private void validatePaymentRequest(PaymentRequest request) {
        // Basic validation (additional validation can be added here)
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        
        // Validate payment method specific fields
        switch (request.getPaymentMethod()) {
            case CARD:
                if (request.getCardNumber() == null || request.getCvv() == null) {
                    throw new IllegalArgumentException("Card number and CVV are required for card payments");
                }
                break;
            case BANK:
                if (request.getAccountNumber() == null || request.getRoutingNumber() == null) {
                    throw new IllegalArgumentException("Account number and routing number are required for bank transfers");
                }
                break;
            case WALLET:
                if (request.getWalletId() == null) {
                    throw new IllegalArgumentException("Wallet ID is required for wallet payments");
                }
                break;
        }
    }
    
    private Transaction createTransaction(PaymentRequest request, String idempotencyKey) {
        String referenceId = generateReferenceId();
        
        return Transaction.builder()
                .referenceId(referenceId)
                .userId(request.getUserId())
                .merchantId(request.getMerchantId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .paymentProvider(request.getPaymentProvider())
                .description(request.getDescription())
                .idempotencyKey(idempotencyKey)
                .webhookUrl(request.getWebhookUrl())
                .metadata(serializeMetadata(request.getMetadata()))
                .status(Transaction.TransactionStatus.PENDING)
                .build();
    }
    
    private String generateReferenceId() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
    }
    
    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize metadata", e);
            return null;
        }
    }
    
    private boolean processPaymentWithProvider(Transaction transaction) {
        // Simulate payment processing with external provider
        // In a real implementation, this would integrate with Stripe, PayPal, etc.
        log.info("Processing payment with provider for transaction: {}", transaction.getId());
        
        // Simulate processing delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate 95% success rate
        return Math.random() > 0.05;
    }
    
    private void sendWebhook(Transaction transaction) {
        WebhookEvent webhookEvent = WebhookEvent.builder()
                .transaction(transaction)
                .webhookUrl(transaction.getWebhookUrl())
                .payload(createWebhookPayload(transaction))
                .build();
        
        webhookEventRepository.save(webhookEvent);
        log.info("Webhook event created for transaction: {}", transaction.getId());
    }
    
    private String createWebhookPayload(Transaction transaction) {
        try {
            Map<String, Object> payload = Map.of(
                "transaction_id", transaction.getId().toString(),
                "reference_id", transaction.getReferenceId(),
                "status", transaction.getStatus().toString(),
                "amount", transaction.getAmount(),
                "currency", transaction.getCurrency(),
                "timestamp", LocalDateTime.now().toString()
            );
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to create webhook payload", e);
            return "{}";
        }
    }
    
    private void publishPaymentEvent(Transaction transaction, PaymentEvent.EventType eventType) {
        PaymentEvent event = PaymentEvent.builder()
                .transactionId(transaction.getId())
                .referenceId(transaction.getReferenceId())
                .userId(transaction.getUserId())
                .merchantId(transaction.getMerchantId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .paymentMethod(transaction.getPaymentMethod())
                .paymentProvider(transaction.getPaymentProvider())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .idempotencyKey(transaction.getIdempotencyKey())
                .fraudScore(transaction.getFraudScore())
                .errorCode(transaction.getErrorCode())
                .errorMessage(transaction.getErrorMessage())
                .webhookUrl(transaction.getWebhookUrl())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .eventType(eventType)
                .eventTimestamp(LocalDateTime.now())
                .build();
        
        kafkaTemplate.send(PAYMENT_EVENTS_TOPIC, transaction.getId().toString(), event);
        log.info("Published payment event: {} for transaction: {}", eventType, transaction.getId());
    }
    
    private void logAuditEvent(Transaction transaction, AuditLog.EventType eventType, String eventData) {
        AuditLog auditLog = AuditLog.builder()
                .transaction(transaction)
                .eventType(eventType.toString())
                .eventData(eventData)
                .userId(transaction.getUserId())
                .build();
        
        auditLogRepository.save(auditLog);
    }
    
    private PaymentResponse mapToPaymentResponse(Transaction transaction) {
        return PaymentResponse.builder()
                .id(transaction.getId())
                .referenceId(transaction.getReferenceId())
                .userId(transaction.getUserId())
                .merchantId(transaction.getMerchantId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .paymentMethod(transaction.getPaymentMethod())
                .paymentProvider(transaction.getPaymentProvider())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .errorCode(transaction.getErrorCode())
                .errorMessage(transaction.getErrorMessage())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
} 