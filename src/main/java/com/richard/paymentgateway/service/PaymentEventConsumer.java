package com.richard.paymentgateway.service;

import com.richard.paymentgateway.dto.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {
    
    private final PaymentService paymentService;
    private final WebhookService webhookService;
    
    /**
     * Consume payment events from Kafka and process them asynchronously
     * @param event The payment event
     * @param key The Kafka message key (transaction ID)
     * @param partition The Kafka partition
     * @param offset The Kafka offset
     */
    @KafkaListener(
        topics = "payment-events",
        groupId = "payment-gateway-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentEvent(
            @Payload PaymentEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.info("Received payment event: {} for transaction: {} from partition: {} offset: {}", 
                event.getEventType(), event.getTransactionId(), partition, offset);
        
        try {
            switch (event.getEventType()) {
                case PAYMENT_INITIATED:
                    handlePaymentInitiated(event);
                    break;
                case PAYMENT_PROCESSED:
                    handlePaymentProcessed(event);
                    break;
                case PAYMENT_SUCCESS:
                    handlePaymentSuccess(event);
                    break;
                case PAYMENT_FAILED:
                    handlePaymentFailed(event);
                    break;
                case WEBHOOK_SENT:
                    handleWebhookSent(event);
                    break;
                case WEBHOOK_FAILED:
                    handleWebhookFailed(event);
                    break;
                default:
                    log.warn("Unknown event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing payment event: {} for transaction: {}", 
                     event.getEventType(), event.getTransactionId(), e);
            // In a production system, you might want to send failed events to a dead letter queue
        }
    }
    
    /**
     * Handle payment initiated events
     * @param event The payment event
     */
    private void handlePaymentInitiated(PaymentEvent event) {
        log.info("Processing payment initiated event for transaction: {}", event.getTransactionId());
        
        // Process the payment asynchronously
        paymentService.processPayment(event.getTransactionId());
    }
    
    /**
     * Handle payment processed events
     * @param event The payment event
     */
    private void handlePaymentProcessed(PaymentEvent event) {
        log.info("Payment processed event received for transaction: {}", event.getTransactionId());
        
        // Additional processing logic can be added here
        // For example, updating analytics, sending notifications, etc.
    }
    
    /**
     * Handle payment success events
     * @param event The payment event
     */
    private void handlePaymentSuccess(PaymentEvent event) {
        log.info("Payment success event received for transaction: {}", event.getTransactionId());
        
        // Send webhook if configured
        if (event.getWebhookUrl() != null) {
            webhookService.sendWebhook(event);
        }
        
        // Additional success processing
        // - Send confirmation emails
        // - Update user account balance
        // - Trigger downstream services
        // - Update analytics
    }
    
    /**
     * Handle payment failed events
     * @param event The payment event
     */
    private void handlePaymentFailed(PaymentEvent event) {
        log.info("Payment failed event received for transaction: {}", event.getTransactionId());
        
        // Send webhook if configured
        if (event.getWebhookUrl() != null) {
            webhookService.sendWebhook(event);
        }
        
        // Additional failure processing
        // - Send failure notifications
        // - Update fraud detection models
        // - Trigger manual review if needed
    }
    
    /**
     * Handle webhook sent events
     * @param event The payment event
     */
    private void handleWebhookSent(PaymentEvent event) {
        log.info("Webhook sent event received for transaction: {}", event.getTransactionId());
        
        // Webhook delivery confirmation processing
        // - Update webhook delivery status
        // - Log successful delivery
    }
    
    /**
     * Handle webhook failed events
     * @param event The payment event
     */
    private void handleWebhookFailed(PaymentEvent event) {
        log.info("Webhook failed event received for transaction: {}", event.getTransactionId());
        
        // Webhook failure processing
        // - Schedule retry
        // - Send to dead letter queue if max retries exceeded
        // - Alert operations team
    }
    
    /**
     * Consume payment results from the results topic
     * @param event The payment result event
     */
    @KafkaListener(
        topics = "payment-results",
        groupId = "payment-gateway-results-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentResult(
            @Payload PaymentEvent event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.info("Received payment result: {} for transaction: {} from partition: {} offset: {}", 
                event.getEventType(), event.getTransactionId(), partition, offset);
        
        try {
            // Process payment results
            // This could include updating final transaction status,
            // triggering downstream processes, etc.
            
            log.info("Payment result processed successfully for transaction: {}", event.getTransactionId());
            
        } catch (Exception e) {
            log.error("Error processing payment result for transaction: {}", event.getTransactionId(), e);
        }
    }
} 