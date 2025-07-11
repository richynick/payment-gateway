package com.richard.paymentgateway.service;

import com.richard.paymentgateway.dto.PaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {
    
    @Value("${payment.fraud.enabled:true}")
    private boolean fraudDetectionEnabled;
    
    @Value("${payment.fraud.score-threshold:0.7}")
    private BigDecimal fraudScoreThreshold;
    
    private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("^[0-9]{13,19}$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^[0-9]{3,4}$");
    
    /**
     * Calculate fraud score for a payment request
     * @param request The payment request
     * @return Fraud score between 0.0 (low risk) and 1.0 (high risk)
     */
    public BigDecimal calculateFraudScore(PaymentRequest request) {
        if (!fraudDetectionEnabled) {
            return BigDecimal.ZERO;
        }
        
        log.info("Calculating fraud score for payment request: {}", request.getReferenceId());
        
        BigDecimal score = BigDecimal.ZERO;
        
        // Check amount-based risk
        score = score.add(calculateAmountRisk(request.getAmount()));
        
        // Check payment method risk
        score = score.add(calculatePaymentMethodRisk(request.getPaymentMethod()));
        
        // Check card validation (if applicable)
        if (request.getPaymentMethod() == com.richard.paymentgateway.model.Transaction.PaymentMethod.CARD) {
            score = score.add(validateCardDetails(request));
        }
        
        // Check for suspicious patterns
        score = score.add(checkSuspiciousPatterns(request));
        
        // Normalize score to 0.0-1.0 range
        score = score.min(BigDecimal.ONE).max(BigDecimal.ZERO);
        
        log.info("Fraud score calculated: {} for payment request: {}", score, request.getReferenceId());
        
        return score;
    }
    
    /**
     * Check if a payment should be blocked based on fraud score
     * @param fraudScore The calculated fraud score
     * @return true if payment should be blocked
     */
    public boolean shouldBlockPayment(BigDecimal fraudScore) {
        return fraudScore.compareTo(fraudScoreThreshold) >= 0;
    }
    
    private BigDecimal calculateAmountRisk(BigDecimal amount) {
        // Higher amounts have higher risk
        if (amount.compareTo(new BigDecimal("10000")) >= 0) {
            return new BigDecimal("0.4");
        } else if (amount.compareTo(new BigDecimal("1000")) >= 0) {
            return new BigDecimal("0.2");
        } else if (amount.compareTo(new BigDecimal("100")) >= 0) {
            return new BigDecimal("0.1");
        }
        return BigDecimal.ZERO;
    }
    
    private BigDecimal calculatePaymentMethodRisk(com.richard.paymentgateway.model.Transaction.PaymentMethod paymentMethod) {
        switch (paymentMethod) {
            case CARD:
                return new BigDecimal("0.1"); // Medium risk
            case WALLET:
                return new BigDecimal("0.05"); // Low risk
            case BANK:
                return new BigDecimal("0.15"); // Higher risk
            default:
                return new BigDecimal("0.2"); // Unknown method, high risk
        }
    }
    
    private BigDecimal validateCardDetails(PaymentRequest request) {
        BigDecimal score = BigDecimal.ZERO;
        
        // Validate card number format
        if (request.getCardNumber() != null && !CARD_NUMBER_PATTERN.matcher(request.getCardNumber()).matches()) {
            score = score.add(new BigDecimal("0.3"));
            log.warn("Invalid card number format detected");
        }
        
        // Validate CVV format
        if (request.getCvv() != null && !CVV_PATTERN.matcher(request.getCvv()).matches()) {
            score = score.add(new BigDecimal("0.2"));
            log.warn("Invalid CVV format detected");
        }
        
        // Check for test card numbers
        if (isTestCardNumber(request.getCardNumber())) {
            score = score.add(new BigDecimal("0.1"));
            log.info("Test card number detected");
        }
        
        return score;
    }
    
    private boolean isTestCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return false;
        }
        
        // Common test card numbers
        String[] testNumbers = {
            "4242424242424242", // Stripe test card
            "4000056655665556", // Stripe test card
            "5555555555554444", // Stripe test card
            "2223003122003222", // Stripe test card
            "4000002500003155"  // Stripe test card
        };
        
        for (String testNumber : testNumbers) {
            if (cardNumber.replaceAll("\\s", "").equals(testNumber)) {
                return true;
            }
        }
        
        return false;
    }
    
    private BigDecimal checkSuspiciousPatterns(PaymentRequest request) {
        BigDecimal score = BigDecimal.ZERO;
        
        // Check for round amounts (suspicious)
        if (request.getAmount().remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            score = score.add(new BigDecimal("0.05"));
        }
        
        // Check for very small amounts (potential testing)
        if (request.getAmount().compareTo(new BigDecimal("1")) <= 0) {
            score = score.add(new BigDecimal("0.1"));
        }
        
        // Check for very large amounts
        if (request.getAmount().compareTo(new BigDecimal("50000")) >= 0) {
            score = score.add(new BigDecimal("0.3"));
        }
        
        return score;
    }
    
    /**
     * Perform additional fraud checks (can be extended with ML models, external services, etc.)
     * @param request The payment request
     * @return Additional fraud score
     */
    public BigDecimal performAdvancedFraudChecks(PaymentRequest request) {
        // This method can be extended to integrate with:
        // - Machine learning models
        // - External fraud detection services (Sift, MaxMind, etc.)
        // - Device fingerprinting
        // - IP geolocation checks
        // - Velocity checks (multiple transactions from same user/IP)
        
        BigDecimal score = BigDecimal.ZERO;
        
        // Example: Check for rapid successive transactions
        // This would typically query the database for recent transactions
        
        // Example: Check IP geolocation
        // This would typically call an external service
        
        // Example: Check device fingerprint
        // This would typically analyze device characteristics
        
        return score;
    }
} 