package com.medco.HealthConnectProvider.services.integration;

import com.medco.HealthConnectProvider.entity.integration.FailedExternalDispensingLog;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensingItem;

import java.util.List;

public interface FailedExternalDispensingService {

    /**
     * Log a failed external dispensing attempt
     */
    void logFailedDispensing(MedicationDispensingItem item,
                           String packageUuid,
                           String serviceId,
                           String dispensingUuid,
                           String contractHeaderUuid,
                           String externalApiUrl,
                           String errorMessage,
                           String lastResponse);

    /**
     * Process pending retries (called by scheduler)
     */
    void processPendingRetries();

    /**
     * Get all pending retries
     */
    List<FailedExternalDispensingLog> getPendingRetries();

    /**
     * Get failed logs by status
     */
    List<FailedExternalDispensingLog> getFailedLogsByStatus(String status);

    /**
     * Mark logs that have reached max retries as inactive
     */
    void markMaxRetriesReached();

    /**
     * Get retry statistics
     */
    RetryStatistics getRetryStatistics();

    /**
     * Manually retry a specific failed log
     */
    boolean manualRetry(String logUuid);

    /**
     * Statistics for retry monitoring
     */
    class RetryStatistics {
        public long pendingRetries;
        public long maxRetriesReached;
        public long successfulRetries;
        public long totalFailed;

        public RetryStatistics(long pendingRetries, long maxRetriesReached, long successfulRetries, long totalFailed) {
            this.pendingRetries = pendingRetries;
            this.maxRetriesReached = maxRetriesReached;
            this.successfulRetries = successfulRetries;
            this.totalFailed = totalFailed;
        }
    }
}
