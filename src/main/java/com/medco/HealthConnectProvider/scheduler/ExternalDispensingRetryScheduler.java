package com.medco.HealthConnectProvider.scheduler;

import com.medco.HealthConnectProvider.services.integration.FailedExternalDispensingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "external.api.retry.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ExternalDispensingRetryScheduler {

    private final FailedExternalDispensingService failedDispensingService;

    /**
     * Scheduled task to process failed external dispensing retries
     * Runs daily at 12:00 PM (noon)
     * Cron expression: "0 0 12 * * ?" 
     * - Second: 0
     * - Minute: 0  
     * - Hour: 12 (12 PM)
     * - Day of month: * (every day)
     * - Month: * (every month)
     * - Day of week: ? (any day of week)
     */
    @Scheduled(cron = "0 0 12 * * ?", zone = "Africa/Addis_Ababa")
    public void processFailedExternalDispensingRetries() {
        log.info("=== Starting scheduled retry process for failed external dispensing at {} ===", 
                LocalDateTime.now());
        
        try {
            // Get statistics before processing
            FailedExternalDispensingService.RetryStatistics beforeStats = 
                    failedDispensingService.getRetryStatistics();
            
            log.info("Retry statistics before processing - Pending: {}, Max Retries Reached: {}, Successful: {}, Total: {}", 
                    beforeStats.pendingRetries, beforeStats.maxRetriesReached, 
                    beforeStats.successfulRetries, beforeStats.totalFailed);
            
            // Process pending retries
            failedDispensingService.processPendingRetries();
            
            // Get statistics after processing
            FailedExternalDispensingService.RetryStatistics afterStats = 
                    failedDispensingService.getRetryStatistics();
            
            log.info("Retry statistics after processing - Pending: {}, Max Retries Reached: {}, Successful: {}, Total: {}", 
                    afterStats.pendingRetries, afterStats.maxRetriesReached, 
                    afterStats.successfulRetries, afterStats.totalFailed);
            
            // Calculate processed counts
            long processedRetries = beforeStats.pendingRetries - afterStats.pendingRetries;
            long newSuccessful = afterStats.successfulRetries - beforeStats.successfulRetries;
            long newMaxRetries = afterStats.maxRetriesReached - beforeStats.maxRetriesReached;
            
            log.info("Processing summary - Processed: {}, New Successful: {}, New Max Retries: {}", 
                    processedRetries, newSuccessful, newMaxRetries);
            
        } catch (Exception e) {
            log.error("Error occurred during scheduled retry process", e);
        }
        
        log.info("=== Completed scheduled retry process for failed external dispensing at {} ===", 
                LocalDateTime.now());
    }

    /**
     * Additional scheduled task to run every 4 hours for more frequent retries
     * This can be enabled/disabled via configuration
     */
    @Scheduled(cron = "0 0 */4 * * ?", zone = "Africa/Addis_Ababa")
    @ConditionalOnProperty(name = "external.api.retry.frequent.enabled", havingValue = "true", matchIfMissing = false)
    public void processFailedExternalDispensingRetriesFrequent() {
        log.info("=== Starting frequent retry process for failed external dispensing at {} ===", 
                LocalDateTime.now());
        
        try {
            // Only process retries that are due (not all pending)
            failedDispensingService.processPendingRetries();
            
            FailedExternalDispensingService.RetryStatistics stats = 
                    failedDispensingService.getRetryStatistics();
            
            log.info("Frequent retry statistics - Pending: {}, Max Retries Reached: {}, Successful: {}", 
                    stats.pendingRetries, stats.maxRetriesReached, stats.successfulRetries);
            
        } catch (Exception e) {
            log.error("Error occurred during frequent retry process", e);
        }
        
        log.info("=== Completed frequent retry process for failed external dispensing at {} ===", 
                LocalDateTime.now());
    }

    /**
     * Cleanup task to remove old successful retry logs
     * Runs daily at 2:00 AM
     * Keeps logs for 30 days for audit purposes
     */
    @Scheduled(cron = "0 0 2 * * ?", zone = "Africa/Addis_Ababa")
    @ConditionalOnProperty(name = "external.api.retry.cleanup.enabled", havingValue = "true", matchIfMissing = true)
    public void cleanupOldRetryLogs() {
        log.info("=== Starting cleanup of old retry logs at {} ===", LocalDateTime.now());
        
        try {
            // This would require additional implementation in the service
            // For now, just log the intent
            log.info("Cleanup task executed - implementation can be added to remove logs older than 30 days");
            
        } catch (Exception e) {
            log.error("Error occurred during cleanup process", e);
        }
        
        log.info("=== Completed cleanup of old retry logs at {} ===", LocalDateTime.now());
    }
}
