package com.medco.HealthConnectProvider.services.impl.integration;

import com.medco.HealthConnectProvider.services.integration.FailedExternalClaimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "external.api.retry.claim-scheduler.enabled", 
    havingValue = "true", 
    matchIfMissing = false
)
public class ClaimRetrySchedulerService {

    private final FailedExternalClaimService failedClaimService;

    /**
     * Scheduled task to retry failed claims every hour
     * Runs every hour at minute 0
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at minute 0
    @ConditionalOnProperty(
        value = "external.api.retry.claim-scheduler.frequent.enabled", 
        havingValue = "true", 
        matchIfMissing = true
    )
    public void retryFailedClaimsHourly() {
        log.info("Starting hourly failed claim retry process");
        
        try {
            FailedExternalClaimService.RetryStatistics statsBefore = failedClaimService.getRetryStatistics();
            log.info("Before retry - Pending: {}, Max retries reached: {}, Total failed: {}", 
                    statsBefore.getPendingRetries(), 
                    statsBefore.getMaxRetriesReached(), 
                    statsBefore.getTotalFailed());

            failedClaimService.retryPendingClaims();

            FailedExternalClaimService.RetryStatistics statsAfter = failedClaimService.getRetryStatistics();
            log.info("After retry - Pending: {}, Max retries reached: {}, Total failed: {}", 
                    statsAfter.getPendingRetries(), 
                    statsAfter.getMaxRetriesReached(), 
                    statsAfter.getTotalFailed());

            log.info("Completed hourly failed claim retry process");
            
        } catch (Exception e) {
            log.error("Error during hourly failed claim retry process", e);
        }
    }

    /**
     * Scheduled task to retry failed claims every 6 hours
     * Runs at 00:00, 06:00, 12:00, 18:00
     */
    @Scheduled(cron = "0 0 0,6,12,18 * * *") // Every 6 hours
    @ConditionalOnProperty(
        value = "external.api.retry.claim-scheduler.frequent.enabled", 
        havingValue = "false", 
        matchIfMissing = false
    )
    public void retryFailedClaimsSixHourly() {
        log.info("Starting 6-hourly failed claim retry process");
        
        try {
            FailedExternalClaimService.RetryStatistics statsBefore = failedClaimService.getRetryStatistics();
            log.info("Before retry - Pending: {}, Max retries reached: {}, Total failed: {}", 
                    statsBefore.getPendingRetries(), 
                    statsBefore.getMaxRetriesReached(), 
                    statsBefore.getTotalFailed());

            failedClaimService.retryPendingClaims();

            FailedExternalClaimService.RetryStatistics statsAfter = failedClaimService.getRetryStatistics();
            log.info("After retry - Pending: {}, Max retries reached: {}, Total failed: {}", 
                    statsAfter.getPendingRetries(), 
                    statsAfter.getMaxRetriesReached(), 
                    statsAfter.getTotalFailed());

            log.info("Completed 6-hourly failed claim retry process");
            
        } catch (Exception e) {
            log.error("Error during 6-hourly failed claim retry process", e);
        }
    }

    /**
     * Scheduled task to cleanup old logs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @ConditionalOnProperty(
        value = "external.api.retry.claim-cleanup.enabled", 
        havingValue = "true", 
        matchIfMissing = true
    )
    public void cleanupOldClaimLogs() {
        log.info("Starting daily claim logs cleanup process");
        
        try {
            // Cleanup logs older than 30 days
            failedClaimService.cleanupOldLogs(30);
            log.info("Completed daily claim logs cleanup process");
            
        } catch (Exception e) {
            log.error("Error during daily claim logs cleanup process", e);
        }
    }

    /**
     * Scheduled task to log retry statistics every 30 minutes
     */
    @Scheduled(fixedRate = 1800000) // Every 30 minutes (30 * 60 * 1000 ms)
    @ConditionalOnProperty(
        value = "external.api.retry.claim-scheduler.stats.enabled", 
        havingValue = "true", 
        matchIfMissing = false
    )
    public void logRetryStatistics() {
        try {
            FailedExternalClaimService.RetryStatistics stats = failedClaimService.getRetryStatistics();
            
            if (stats.getTotalFailed() > 0) {
                log.info("Claim Retry Statistics - Pending: {}, Max retries reached: {}, Successful retries: {}, Total failed: {}", 
                        stats.getPendingRetries(), 
                        stats.getMaxRetriesReached(), 
                        stats.getSuccessfulRetries(), 
                        stats.getTotalFailed());
            }
            
        } catch (Exception e) {
            log.error("Error logging claim retry statistics", e);
        }
    }
}
