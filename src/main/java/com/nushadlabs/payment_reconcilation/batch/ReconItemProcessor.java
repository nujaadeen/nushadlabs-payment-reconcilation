package com.nushadlabs.payment_reconcilation.batch;

import com.nushadlabs.payment_reconcilation.model.CsvTransaction;
import com.nushadlabs.payment_reconcilation.model.ReconPayload;
import com.nushadlabs.payment_reconcilation.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReconItemProcessor implements ItemProcessor<CsvTransaction, ReconPayload> {

    private final TransactionRepository transactionRepository;

    @Override
    public ReconPayload process(CsvTransaction csv) {
        log.debug("Thread: {} processing transactionId: {}",
                Thread.currentThread().getName(),
                csv.transactionId());
        return transactionRepository.findByTransactionId(csv.transactionId())
                .filter(tx -> "COMPLETED".equals(csv.status())
                        && !"COMPLETED".equals(tx.getStatus()))
                .map(tx -> new ReconPayload(
                        tx.getTransactionId(),
                        tx.getOrderId(),
                        tx.getAmount(),
                        tx.getCurrency(),
                        tx.getStatus(),
                        "recon",
                        Instant.now()
                ))
                .orElse(null);
    }
}