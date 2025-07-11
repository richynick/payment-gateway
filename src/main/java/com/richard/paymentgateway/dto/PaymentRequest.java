package com.richard.paymentgateway.dto;

import com.richard.paymentgateway.model.Transaction;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Merchant ID is required")
    private UUID merchantId;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Amount must have at most 15 digits and 4 decimal places")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
    private String currency;
    
    @NotNull(message = "Payment method is required")
    private Transaction.PaymentMethod paymentMethod;
    
    @Size(max = 50, message = "Payment provider must not exceed 50 characters")
    private String paymentProvider;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    @Size(max = 255, message = "Idempotency key must not exceed 255 characters")
    private String idempotencyKey;
    
    @Size(max = 500, message = "Webhook URL must not exceed 500 characters")
    @Pattern(regexp = "^(https?://).*", message = "Webhook URL must be a valid HTTP/HTTPS URL")
    private String webhookUrl;
    
    private Map<String, Object> metadata;
    
    // Card payment specific fields
    @Size(max = 19, message = "Card number must not exceed 19 characters")
    private String cardNumber;
    
    @Size(min = 2, max = 2, message = "Expiry month must be 2 digits")
    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Expiry month must be between 01 and 12")
    private String expiryMonth;
    
    @Size(min = 2, max = 2, message = "Expiry year must be 2 digits")
    @Pattern(regexp = "^[0-9]{2}$", message = "Expiry year must be 2 digits")
    private String expiryYear;
    
    @Size(min = 3, max = 4, message = "CVV must be 3 or 4 digits")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV must contain only digits")
    private String cvv;
    
    // Bank transfer specific fields
    @Size(max = 50, message = "Account number must not exceed 50 characters")
    private String accountNumber;
    
    @Size(max = 50, message = "Routing number must not exceed 50 characters")
    private String routingNumber;
    
    @Size(max = 100, message = "Account holder name must not exceed 100 characters")
    private String accountHolderName;
    
    // Wallet specific fields
    @Size(max = 100, message = "Wallet ID must not exceed 100 characters")
    private String walletId;
} 