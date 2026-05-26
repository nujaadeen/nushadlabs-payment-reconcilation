package com.nushadlabs.payment_reconcilation.repository;

import com.nushadlabs.payment_reconcilation.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    Optional<Transaction> findByTransactionId(String transactionId);
}