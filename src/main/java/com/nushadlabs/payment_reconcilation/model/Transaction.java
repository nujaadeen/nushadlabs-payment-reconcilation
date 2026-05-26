package com.nushadlabs.payment_reconcilation.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;

@Data
@Document("transactions")
public class Transaction {

    @Id
    private String id;

    @Field("transaction_id")
    private String transactionId;

    @Field("order_id")
    private String orderId;

    @Field("customer_id")
    private String customerId;

    private BigDecimal amount;
    private String currency;
    private String status;

    @Field("created_at")
    private String createdAt;

    @Field("merchant_id")
    private String merchantId;

    @Field("payment_method")
    private String paymentMethod;

    private String channel;
}