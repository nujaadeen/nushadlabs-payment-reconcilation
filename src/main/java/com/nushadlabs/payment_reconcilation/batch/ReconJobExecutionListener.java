package com.nushadlabs.payment_reconcilation.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;

@Component
@Slf4j
public class ReconJobExecutionListener implements JobExecutionListener {

    private static final String SEP = "=".repeat(72);

    @Value("${recon.thread-pool-size:8}")
    private int threadPoolSize;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String filePath = jobExecution.getJobParameters().getString("filePath");
        log.info(SEP);
        log.info("[RECON] JOB STARTED");
        log.info("[RECON]   Job ID       : {}", jobExecution.getJobId());
        log.info("[RECON]   File Path    : {}", filePath);
        log.info("[RECON]   Start Time   : {}", jobExecution.getStartTime());
        log.info("[RECON]   Thread Pool  : {}", threadPoolSize);
        log.info(SEP);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String filePath = jobExecution.getJobParameters().getString("filePath");
        boolean failed = jobExecution.getStatus().isUnsuccessful();

        LocalDateTime start = jobExecution.getStartTime();
        LocalDateTime end   = jobExecution.getEndTime();
        String elapsed = (start != null && end != null)
                ? formatDuration(Duration.between(start, end))
                : "unknown";

        log.info(SEP);
        log.info("[RECON] JOB {}", jobExecution.getStatus());
        log.info("[RECON]   Job ID       : {}", jobExecution.getJobId());
        log.info("[RECON]   File Path    : {}", filePath);
        log.info("[RECON]   Exit Code    : {}", jobExecution.getExitStatus().getExitCode());
        log.info("[RECON]   Elapsed      : {}", elapsed);
        log.info("[RECON]");
        log.info("[RECON]   Step Breakdown:");

        jobExecution.getStepExecutions().stream()
                .sorted(Comparator.comparing(
                        se -> se.getStartTime() == null ? LocalDateTime.MIN : se.getStartTime()))
                .forEach(se -> logStep(se));

        if (failed) {
            log.error("[RECON]");
            log.error("[RECON]   FAILURE DETAILS:");
            String exitDesc = jobExecution.getExitStatus().getExitDescription();
            if (exitDesc != null && !exitDesc.isBlank()) {
                // Exit description often contains the full stack trace — log first line only;
                // the exception log below includes the full trace via SLF4J Throwable support.
                String firstLine = exitDesc.lines().findFirst().orElse(exitDesc);
                log.error("[RECON]   Exit Description : {}", firstLine);
            }
            jobExecution.getAllFailureExceptions().forEach(ex ->
                    log.error("[RECON]   Exception        : {} — {}",
                            ex.getClass().getSimpleName(), ex.getMessage(), ex));
        }

        log.info(SEP);
    }

    private void logStep(StepExecution se) {
        Duration stepDuration = (se.getStartTime() != null && se.getEndTime() != null)
                ? Duration.between(se.getStartTime(), se.getEndTime())
                : Duration.ZERO;

        log.info("[RECON]     {}", String.format(
                "%-42s  read=%7d  write=%7d  skip=%5d  duration=%s",
                se.getStepName(),
                se.getReadCount(),
                se.getWriteCount(),
                se.getSkipCount(),
                formatDuration(stepDuration)));
    }

    private String formatDuration(Duration d) {
        return String.format("%dm %ds %dms", d.toMinutes(), d.toSecondsPart(), d.toMillisPart());
    }
}
