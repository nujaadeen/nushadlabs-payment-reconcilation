package com.nushadlabs.payment_reconcilation.model;

import java.math.BigDecimal;
import java.time.Instant;

public record ReconPayload(
        String transactionId,
        String orderId,
        BigDecimal amount,
        String currency,
        String originalStatus,
        String source,
        Instant reconTimestamp
) {}