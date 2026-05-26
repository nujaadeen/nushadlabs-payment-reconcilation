package com.nushadlabs.payment_reconcilation.batch;

import com.nushadlabs.payment_reconcilation.model.CsvTransaction;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
public class ReconItemReader {

    @Bean
    @StepScope
    public FlatFileItemReader<CsvTransaction> csvReader(
            @Value("#{jobParameters['filePath']}") String filePath,
            @Value("#{stepExecutionContext['linesToSkip']}") long linesToSkip,
            @Value("#{stepExecutionContext['maxItemCount']}") long maxItemCount) {

        return new FlatFileItemReaderBuilder<CsvTransaction>()
                .name("reconCsvReader")
                .resource(new FileSystemResource(filePath))
                .linesToSkip((int) linesToSkip)
                .maxItemCount((int) maxItemCount)
                .delimited()
                .names("transactionId", "orderId", "customerId", "amount",
                        "currency", "status", "createdAt", "merchantId",
                        "paymentMethod", "channel")
                .fieldSetMapper(fs -> new CsvTransaction(
                        fs.readString("transactionId"),
                        fs.readString("orderId"),
                        fs.readString("customerId"),
                        fs.readBigDecimal("amount"),
                        fs.readString("currency"),
                        fs.readString("status"),
                        fs.readString("createdAt"),
                        fs.readString("merchantId"),
                        fs.readString("paymentMethod"),
                        fs.readString("channel")
                ))
                .build();
    }
}