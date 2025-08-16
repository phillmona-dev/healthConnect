package com.medco.HealthConnectProvider.controller.integration;

import com.medco.HealthConnectProvider.entity.integration.FailedExternalDispensingLog;
import com.medco.HealthConnectProvider.services.integration.FailedExternalDispensingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/healthConnect/failed-external-dispensing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Failed External Dispensing", description = "APIs for managing failed external dispensing retries")
public class FailedExternalDispensingController {

    private final FailedExternalDispensingService failedDispensingService;

    @GetMapping("/pending-retries")
    @Operation(summary = "Get pending retries", 
               description = "Get all failed dispensing logs that are pending retry")
    public ResponseEntity<List<FailedExternalDispensingLog>> getPendingRetries() {
        log.info("Getting pending retries");
        List<FailedExternalDispensingLog> pendingRetries = failedDispensingService.getPendingRetries();
        return ResponseEntity.ok(pendingRetries);
    }

    @GetMapping("/by-status/{status}")
    @Operation(summary = "Get failed logs by status", 
               description = "Get failed dispensing logs by status (ACTIVE, INACTIVE)")
    public ResponseEntity<List<FailedExternalDispensingLog>> getFailedLogsByStatus(
            @Parameter(description = "Status: ACTIVE or INACTIVE") @PathVariable String status) {
        log.info("Getting failed logs by status: {}", status);
        List<FailedExternalDispensingLog> logs = failedDispensingService.getFailedLogsByStatus(status);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get retry statistics", 
               description = "Get statistics about retry attempts")
    public ResponseEntity<FailedExternalDispensingService.RetryStatistics> getRetryStatistics() {
        log.info("Getting retry statistics");
        FailedExternalDispensingService.RetryStatistics stats = failedDispensingService.getRetryStatistics();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/manual-retry/{logUuid}")
    @Operation(summary = "Manual retry", 
               description = "Manually trigger retry for a specific failed log")
    public ResponseEntity<String> manualRetry(
            @Parameter(description = "Log UUID") @PathVariable String logUuid) {
        log.info("Manual retry requested for log: {}", logUuid);
        
        boolean success = failedDispensingService.manualRetry(logUuid);
        
        if (success) {
            return ResponseEntity.ok("Manual retry initiated successfully");
        } else {
            return ResponseEntity.badRequest().body("Failed to initiate manual retry");
        }
    }

    @PostMapping("/process-retries")
    @Operation(summary = "Process all pending retries", 
               description = "Manually trigger processing of all pending retries")
    public ResponseEntity<String> processAllRetries() {
        log.info("Manual processing of all pending retries requested");
        
        try {
            failedDispensingService.processPendingRetries();
            return ResponseEntity.ok("Retry processing completed successfully");
        } catch (Exception e) {
            log.error("Error processing retries", e);
            return ResponseEntity.internalServerError().body("Error processing retries: " + e.getMessage());
        }
    }

    @PostMapping("/mark-max-retries")
    @Operation(summary = "Mark max retries reached", 
               description = "Mark logs that have reached maximum retries as inactive")
    public ResponseEntity<String> markMaxRetriesReached() {
        log.info("Marking max retries reached");
        
        try {
            failedDispensingService.markMaxRetriesReached();
            return ResponseEntity.ok("Max retries marked successfully");
        } catch (Exception e) {
            log.error("Error marking max retries", e);
            return ResponseEntity.internalServerError().body("Error marking max retries: " + e.getMessage());
        }
    }
}
