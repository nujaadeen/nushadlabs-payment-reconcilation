package com.nushadlabs.payment_reconcilation.model;

import java.math.BigDecimal;

public record CsvTransaction(
        String transactionId,
        String orderId,
        String customerId,
        BigDecimal amount,
        String currency,
        String status,
        String createdAt,
        String merchantId,
        String paymentMethod,
        String channel
) {}