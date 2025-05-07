package com.middleware.listener.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CleanupService {
    private static final Logger log = LoggerFactory.getLogger(CleanupService.class);

    @Value("${cleanup.retention-days:30}")
    private int retentionDays;

    @Value("${cleanup.archive-directory:archive}")
    private String archiveDirectory;

    private final MeterRegistry meterRegistry;
    private final AtomicInteger filesDeleted = new AtomicInteger(0);
    private final AtomicInteger filesArchived = new AtomicInteger(0);

    public CleanupService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        // Register metrics
        meterRegistry.gauge("cleanup.files.deleted", filesDeleted);
        meterRegistry.gauge("cleanup.files.archived", filesArchived);
    }

    @Scheduled(cron = "${cleanup.schedule:0 0 1 * * ?}") // Default: 1 AM daily
    public void cleanupProcessedFiles() {
        log.info("Starting cleanup of processed files");
        
        cleanupDirectory("processed", true);
        cleanupDirectory("error", false);
        
        log.info("Cleanup completed. Deleted: {}, Archived: {}", 
            filesDeleted.get(), filesArchived.get());
    }

    private void cleanupDirectory(String directory, boolean archive) {
        File dir = new File(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("Directory {} does not exist or is not a directory", directory);
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            log.warn("Unable to list files in directory {}", directory);
            return;
        }

        Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        Arrays.stream(files).forEach(file -> {
            try {
                BasicFileAttributes attrs = Files.readAttributes(
                    file.toPath(), BasicFileAttributes.class);

                if (attrs.creationTime().toInstant().isBefore(cutoffDate)) {
                    if (archive) {
                        archiveFile(file);
                    } else {
                        deleteFile(file);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing file {}: {}", file.getName(), e.getMessage());
                meterRegistry.counter("cleanup.errors").increment();
            }
        });
    }

    private void archiveFile(File file) {
        try {
            File archiveDir = new File(archiveDirectory);
            if (!archiveDir.exists() && !archiveDir.mkdirs()) {
                log.error("Failed to create archive directory");
                return;
            }

            Path targetPath = archiveDir.toPath().resolve(file.getName());
            Files.move(file.toPath(), targetPath);
            filesArchived.incrementAndGet();
            log.debug("Archived file: {}", file.getName());
        } catch (Exception e) {
            log.error("Error archiving file {}: {}", file.getName(), e.getMessage());
            meterRegistry.counter("cleanup.archive.errors").increment();
        }
    }

    private void deleteFile(File file) {
        try {
            Files.delete(file.toPath());
            filesDeleted.incrementAndGet();
            log.debug("Deleted file: {}", file.getName());
        } catch (Exception e) {
            log.error("Error deleting file {}: {}", file.getName(), e.getMessage());
            meterRegistry.counter("cleanup.delete.errors").increment();
        }
    }
} 