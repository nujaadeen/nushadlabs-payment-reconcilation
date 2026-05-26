package com.nushadlabs.payment_reconcilation.batch;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class CsvLineRangePartitioner implements Partitioner {

    private final String filePath;
    private final int threadPoolSize;

    public CsvLineRangePartitioner(String filePath, int threadPoolSize) {
        this.filePath = filePath;
        this.threadPoolSize = threadPoolSize;
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        long totalDataLines;
        try (var lines = Files.lines(Path.of(filePath))) {
            totalDataLines = lines.count() - 1; // subtract CSV header
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read file for partitioning: " + filePath, e);
        }

        int partitions = (int) Math.min(gridSize, totalDataLines);
        long baseSize = totalDataLines / partitions;
        long remainder = totalDataLines % partitions;

        Map<String, ExecutionContext> map = new HashMap<>();
        long lineOffset = 0;

        for (int i = 0; i < partitions; i++) {
            long partitionLines = baseSize + (i < remainder ? 1 : 0);
            ExecutionContext ctx = new ExecutionContext();
            ctx.putLong("linesToSkip", lineOffset + 1); // +1 to account for CSV header
            ctx.putLong("maxItemCount", partitionLines);
            map.put("partition" + i, ctx);
            lineOffset += partitionLines;
        }

        return map;
    }
}