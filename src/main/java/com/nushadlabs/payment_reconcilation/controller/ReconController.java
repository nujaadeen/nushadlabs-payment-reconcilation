package com.nushadlabs.payment_reconcilation.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recon")
@Slf4j
public class ReconController {

    private final Job reconJob;
    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;

    public ReconController(Job reconJob,
                           @Qualifier("asyncJobLauncher") JobLauncher jobLauncher,
                           JobExplorer jobExplorer) {
        this.reconJob = reconJob;
        this.jobLauncher = jobLauncher;
        this.jobExplorer = jobExplorer;
    }

    @PostMapping("/start")
    public ResponseEntity<ReconStartResponse> start(
            @RequestParam("filePath") String filePath) throws Exception {

        log.info("Recon job triggered for filePath={}", filePath);

        JobParameters params = new JobParametersBuilder()
                .addString("filePath", filePath)
                .addString("runId", UUID.randomUUID().toString())
                .addLong("timestamp", Instant.now().toEpochMilli())
                .toJobParameters();

        JobExecution execution = jobLauncher.run(reconJob, params);

        return ResponseEntity.accepted().body(new ReconStartResponse(
                execution.getJobInstance().getInstanceId(),
                execution.getStatus().name()
        ));
    }

    @GetMapping("/status/{jobInstanceId}")
    public ResponseEntity<ReconStatusResponse> status(@PathVariable Long jobInstanceId) {
        JobInstance instance = jobExplorer.getJobInstance(jobInstanceId);
        if (instance == null) {
            return ResponseEntity.notFound().build();
        }

        return jobExplorer.getJobExecutions(instance).stream()
                .max(Comparator.comparing(JobExecution::getCreateTime))
                .map(exec -> ResponseEntity.ok(new ReconStatusResponse(
                        jobInstanceId,
                        exec.getId(),
                        exec.getStatus().name(),
                        exec.getExitStatus().getExitCode(),
                        exec.getCreateTime(),
                        exec.getEndTime()
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record ReconStartResponse(Long jobInstanceId, String status) {}

    public record ReconStatusResponse(
            Long jobInstanceId,
            Long jobExecutionId,
            String status,
            String exitCode,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {}
}