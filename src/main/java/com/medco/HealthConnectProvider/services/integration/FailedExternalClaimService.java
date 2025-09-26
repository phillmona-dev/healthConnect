package com.medco.HealthConnectProvider.services.integration;

import com.medco.HealthConnectProvider.dto.ClaimPaySyncRequest;
import com.medco.HealthConnectProvider.entity.integration.FailedExternalClaimLog;

import java.util.List;

public interface FailedExternalClaimService {

    /**
     * Log a failed claim sync attempt
     */
    void logFailedClaim(ClaimPaySyncRequest request, String claimUuid, String batchCode,
                       String externalApiUrl, String errorMessage, String requestPayload);

    /**
     * Log a successful claim sync
     */
    void logSuccessfulClaim(ClaimPaySyncRequest request, String claimUuid, String batchCode,
                           String externalApiUrl, String requestPayload, String response);

    /**
     * Get all failed claim logs
     */
    List<FailedExternalClaimLog> getAllFailedClaims();

    /**
     * Get failed claim logs by status
     */
    List<FailedExternalClaimLog> getFailedClaimsByStatus(String status);

    /**
     * Get failed claim logs by claim UUID
     */
    List<FailedExternalClaimLog> getFailedClaimsByClaimUuid(String claimUuid);

    /**
     * Get failed claim logs by batch code
     */
    List<FailedExternalClaimLog> getFailedClaimsByBatchCode(String batchCode);

    /**
     * Get pending retries
     */
    List<FailedExternalClaimLog> getPendingRetries();

    /**
     * Retry all pending failed claims
     */
    void retryPendingClaims();

    /**
     * Retry a specific failed claim by log UUID
     */
    boolean retrySpecificClaim(String logUuid);

    /**
     * Manually retry a specific failed log
     */
    boolean manualRetry(String logUuid);

    /**
     * Check if a claim has been successfully sent to external system
     */
    boolean isClaimAlreadySentSuccessfully(String claimUuid);

    /**
     * Check if a batch has been successfully sent to external system
     */
    boolean isBatchAlreadySentSuccessfully(String batchCode);

    /**
     * Get retry statistics
     */
    RetryStatistics getRetryStatistics();

    /**
     * Delete old completed/failed logs (cleanup)
     */
    void cleanupOldLogs(int daysOld);

    /**
     * Retry statistics inner class
     */
    class RetryStatistics {
        private final long pendingRetries;
        private final long maxRetriesReached;
        private final long successfulRetries;
        private final long totalFailed;

        public RetryStatistics(long pendingRetries, long maxRetriesReached, long successfulRetries, long totalFailed) {
            this.pendingRetries = pendingRetries;
            this.maxRetriesReached = maxRetriesReached;
            this.successfulRetries = successfulRetries;
            this.totalFailed = totalFailed;
        }

        public long getPendingRetries() { return pendingRetries; }
        public long getMaxRetriesReached() { return maxRetriesReached; }
        public long getSuccessfulRetries() { return successfulRetries; }
        public long getTotalFailed() { return totalFailed; }
    }
}
