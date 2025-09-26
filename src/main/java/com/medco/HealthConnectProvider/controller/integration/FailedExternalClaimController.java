package com.medco.HealthConnectProvider.controller.integration;

import com.medco.HealthConnectProvider.entity.integration.FailedExternalClaimLog;
import com.medco.HealthConnectProvider.services.integration.FailedExternalClaimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/healthConnect/integration/failed-claims")
@RequiredArgsConstructor
@Slf4j
public class FailedExternalClaimController {

    private final FailedExternalClaimService failedClaimService;

    /**
     * Get all failed claim logs with pagination
     */
    @GetMapping
    public ResponseEntity<Page<FailedExternalClaimLog>> getAllFailedClaims(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<FailedExternalClaimLog> allLogs = failedClaimService.getAllFailedClaims();
        Pageable pageable = PageRequest.of(page, size);
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allLogs.size());
        
        Page<FailedExternalClaimLog> pageResult = new PageImpl<>(
                allLogs.subList(start, end), pageable, allLogs.size());
        
        return ResponseEntity.ok(pageResult);
    }

    /**
     * Get failed claim logs by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<FailedExternalClaimLog>> getFailedClaimsByStatus(
            @PathVariable String status) {
        
        List<FailedExternalClaimLog> logs = failedClaimService.getFailedClaimsByStatus(status);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get failed claim logs by claim UUID
     */
    @GetMapping("/claim/{claimUuid}")
    public ResponseEntity<List<FailedExternalClaimLog>> getFailedClaimsByClaimUuid(
            @PathVariable String claimUuid) {
        
        List<FailedExternalClaimLog> logs = failedClaimService.getFailedClaimsByClaimUuid(claimUuid);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get failed claim logs by batch code
     */
    @GetMapping("/batch/{batchCode}")
    public ResponseEntity<List<FailedExternalClaimLog>> getFailedClaimsByBatchCode(
            @PathVariable String batchCode) {
        
        List<FailedExternalClaimLog> logs = failedClaimService.getFailedClaimsByBatchCode(batchCode);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get pending retries
     */
    @GetMapping("/pending-retries")
    public ResponseEntity<List<FailedExternalClaimLog>> getPendingRetries() {
        List<FailedExternalClaimLog> pendingRetries = failedClaimService.getPendingRetries();
        return ResponseEntity.ok(pendingRetries);
    }

    /**
     * Retry all pending failed claims
     */
    @PostMapping("/retry-all")
    public ResponseEntity<Map<String, String>> retryAllPendingClaims() {
        try {
            failedClaimService.retryPendingClaims();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Retry process initiated for all pending claims");
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrying all pending claims", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to retry pending claims: " + e.getMessage());
            response.put("status", "error");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Retry a specific failed claim by log UUID
     */
    @PostMapping("/retry/{logUuid}")
    public ResponseEntity<Map<String, String>> retrySpecificClaim(@PathVariable String logUuid) {
        try {
            boolean success = failedClaimService.retrySpecificClaim(logUuid);
            
            Map<String, String> response = new HashMap<>();
            if (success) {
                response.put("message", "Claim retry successful");
                response.put("status", "success");
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "Claim retry failed");
                response.put("status", "failed");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Error retrying specific claim: {}", logUuid, e);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to retry claim: " + e.getMessage());
            response.put("status", "error");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Manual retry for a specific failed claim
     */
    @PostMapping("/manual-retry/{logUuid}")
    public ResponseEntity<Map<String, String>> manualRetry(@PathVariable String logUuid) {
        try {
            boolean success = failedClaimService.manualRetry(logUuid);
            
            Map<String, String> response = new HashMap<>();
            if (success) {
                response.put("message", "Manual retry successful");
                response.put("status", "success");
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "Manual retry failed");
                response.put("status", "failed");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Error in manual retry for claim: {}", logUuid, e);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to manually retry claim: " + e.getMessage());
            response.put("status", "error");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Check if a claim has been successfully sent
     */
    @GetMapping("/check-success/claim/{claimUuid}")
    public ResponseEntity<Map<String, Object>> checkClaimSuccess(@PathVariable String claimUuid) {
        boolean isSuccessful = failedClaimService.isClaimAlreadySentSuccessfully(claimUuid);
        
        Map<String, Object> response = new HashMap<>();
        response.put("claimUuid", claimUuid);
        response.put("isSuccessful", isSuccessful);
        response.put("status", isSuccessful ? "COMPLETED" : "NOT_COMPLETED");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Check if a batch has been successfully sent
     */
    @GetMapping("/check-success/batch/{batchCode}")
    public ResponseEntity<Map<String, Object>> checkBatchSuccess(@PathVariable String batchCode) {
        boolean isSuccessful = failedClaimService.isBatchAlreadySentSuccessfully(batchCode);
        
        Map<String, Object> response = new HashMap<>();
        response.put("batchCode", batchCode);
        response.put("isSuccessful", isSuccessful);
        response.put("status", isSuccessful ? "COMPLETED" : "NOT_COMPLETED");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get retry statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<FailedExternalClaimService.RetryStatistics> getRetryStatistics() {
        FailedExternalClaimService.RetryStatistics stats = failedClaimService.getRetryStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Cleanup old logs
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, String>> cleanupOldLogs(
            @RequestParam(defaultValue = "30") int daysOld) {
        try {
            failedClaimService.cleanupOldLogs(daysOld);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Cleanup completed for logs older than " + daysOld + " days");
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error cleaning up old logs", e);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to cleanup old logs: " + e.getMessage());
            response.put("status", "error");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
