package com.medco.HealthConnectProvider.services.integration;

import com.medco.HealthConnectProvider.dto.integration.ExternalClaimRejectionResponse;
import com.medco.HealthConnectProvider.entity.integration.DispensingRejection;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface DispensingRejectionService {

    /**
     * Process rejection response from external system
     */
    void processExternalRejectionResponse(ExternalClaimRejectionResponse rejectionResponse);

    /**
     * Get all rejections for a specific dispensing UUID
     */
    List<DispensingRejection> getRejectionsByDispensingUuid(String dispensingUuid);

    /**
     * Get all rejections for a specific claim UUID
     */
    List<DispensingRejection> getRejectionsByClaimUuid(String claimUuid);

    /**
     * Get all rejections for a specific batch code
     */
    List<DispensingRejection> getRejectionsByBatchCode(String batchCode);

    /**
     * Get all rejections for a specific provider
     */
    List<DispensingRejection> getRejectionsByProvider(String providerUuid);

    /**
     * Get rejections by status
     */
    List<DispensingRejection> getRejectionsByStatus(String status);

    /**
     * Get rejections by category
     */
    List<DispensingRejection> getRejectionsByCategory(String category);

    /**
     * Get rejections that can be resubmitted
     */
    List<DispensingRejection> getResubmittableRejections();

    /**
     * Get rejections within date range
     */
    List<DispensingRejection> getRejectionsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Check if a dispensing has been rejected
     */
    boolean isDispensingRejected(String dispensingUuid);

    /**
     * Mark rejection as resolved
     */
    void markRejectionAsResolved(String rejectionUuid, String resolvedBy, String resolutionComments);

    /**
     * Mark rejection as resubmitted
     */
    void markRejectionAsResubmitted(String rejectionUuid, String resubmittedBy, String resubmissionComments);

    /**
     * Appeal a rejection
     */
    void appealRejection(String rejectionUuid, String appealedBy, String appealComments);

    /**
     * Get rejection statistics
     */
    RejectionStatistics getRejectionStatistics();

    /**
     * Get rejection statistics by provider
     */
    Map<String, Long> getRejectionStatsByProvider(String providerUuid, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Update dispensing status based on rejection
     */
    void updateDispensingStatusForRejection(String dispensingUuid, String rejectionStatus);

    /**
     * Bulk process rejections for a batch
     */
    void processBatchRejections(String batchCode, List<ExternalClaimRejectionResponse.DispensingRejectionDetail> rejections);

    /**
     * Get rejection details by UUID
     */
    DispensingRejection getRejectionByUuid(String rejectionUuid);

    /**
     * Rejection statistics inner class
     */
    class RejectionStatistics {
        private final long totalRejections;
        private final long activeRejections;
        private final long resolvedRejections;
        private final long resubmittedRejections;
        private final long appealedRejections;
        private final Map<String, Long> rejectionsByCategory;

        public RejectionStatistics(long totalRejections, long activeRejections, long resolvedRejections, 
                                 long resubmittedRejections, long appealedRejections, Map<String, Long> rejectionsByCategory) {
            this.totalRejections = totalRejections;
            this.activeRejections = activeRejections;
            this.resolvedRejections = resolvedRejections;
            this.resubmittedRejections = resubmittedRejections;
            this.appealedRejections = appealedRejections;
            this.rejectionsByCategory = rejectionsByCategory;
        }

        public long getTotalRejections() { return totalRejections; }
        public long getActiveRejections() { return activeRejections; }
        public long getResolvedRejections() { return resolvedRejections; }
        public long getResubmittedRejections() { return resubmittedRejections; }
        public long getAppealedRejections() { return appealedRejections; }
        public Map<String, Long> getRejectionsByCategory() { return rejectionsByCategory; }
    }
}
