package com.nushadlabs.payment_reconcilation.batch;

import com.nushadlabs.payment_reconcilation.model.ReconPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ReconItemWriter implements ItemWriter<ReconPayload> {

    @Override
    public void write(Chunk<? extends ReconPayload> chunk) {
        chunk.forEach(payload ->
                log.info("[recon] transactionId={} orderId={} amount={} currency={} status={} ts={}",
                        payload.transactionId(), payload.orderId(), payload.amount(),
                        payload.currency(), payload.originalStatus(), payload.reconTimestamp())
        );
    }
}