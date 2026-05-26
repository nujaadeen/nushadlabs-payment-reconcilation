package com.nushadlabs.payment_reconcilation.batch;

import com.nushadlabs.payment_reconcilation.model.CsvTransaction;
import com.nushadlabs.payment_reconcilation.model.ReconPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class ReconJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ReconItemProcessor processor;
    private final ReconItemWriter writer;

    @Value("${recon.thread-pool-size:8}")
    private int threadPoolSize;

    @Bean
    public Job reconJob(Step masterStep, ReconJobExecutionListener jobExecutionListener) {
        return new JobBuilder("reconJob", jobRepository)
                .listener(jobExecutionListener)
                .start(masterStep)
                .build();
    }

    @Bean
    public Step masterStep(Partitioner csvLineRangePartitioner, Step workerStep,
                           PartitionHandler partitionHandler) {
        return new StepBuilder("masterStep", jobRepository)
                .partitioner("workerStep", csvLineRangePartitioner)
                .partitionHandler(partitionHandler)
                .build();
    }

    @Bean
    public Step workerStep(FlatFileItemReader<CsvTransaction> csvReader) {
        return new StepBuilder("workerStep", jobRepository)
                .<CsvTransaction, ReconPayload>chunk(500, transactionManager)
                .reader(csvReader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    // Step-scoped so #{jobParameters['filePath']} resolves at execution time
    @Bean
    @StepScope
    public Partitioner csvLineRangePartitioner(
            @Value("#{jobParameters['filePath']}") String filePath) {
        return new CsvLineRangePartitioner(filePath, threadPoolSize);
    }

    @Bean
    public ThreadPoolTaskExecutor reconTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threadPoolSize);
        executor.setMaxPoolSize(threadPoolSize);
        executor.setThreadNamePrefix("recon-worker-");
        executor.initialize();
        return executor;
    }

    @Bean
    public PartitionHandler partitionHandler(Step workerStep,
                                             ThreadPoolTaskExecutor reconTaskExecutor) {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setTaskExecutor(reconTaskExecutor);
        handler.setStep(workerStep);
        handler.setGridSize(threadPoolSize);
        return handler;
    }

    // Async launcher so POST /recon/start returns immediately while the job runs in background
    @Bean
    public JobLauncher asyncJobLauncher() throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(new SimpleAsyncTaskExecutor("recon-launcher-"));
        launcher.afterPropertiesSet();
        return launcher;
    }
}