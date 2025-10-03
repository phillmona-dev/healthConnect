package com.medco.HealthConnectProvider.controller.integration;

import com.medco.HealthConnectProvider.annotation.RequiresApiKey;
import com.medco.HealthConnectProvider.dto.integration.ExternalClaimRejectionResponse;
import com.medco.HealthConnectProvider.entity.integration.DispensingRejection;
import com.medco.HealthConnectProvider.services.integration.DispensingRejectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/healthConnect/integration/dispensing-rejections")
@RequiredArgsConstructor
@Slf4j
public class DispensingRejectionController {

    private final DispensingRejectionService rejectionService;

    /**
     * Webhook endpoint for external system to send rejection notifications
     */
    @RequiresApiKey
    @PostMapping("/webhook/external-rejection")
    public ResponseEntity<Map<String, String>> processExternalRejection(
            @RequestBody ExternalClaimRejectionResponse rejectionResponse) {

        log.info("Received external rejection webhook for batch: {}", rejectionResponse.getBatchCode());
        log.debug("Rejected dispensing count: {}",
                rejectionResponse.getRejectedDispensing() != null ? rejectionResponse.getRejectedDispensing().size() : 0);

        try {
            // Validate that at least batch code is provided
            if (rejectionResponse.getBatchCode() == null || rejectionResponse.getBatchCode().isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Batch code is required");
                response.put("status", "error");
                return ResponseEntity.badRequest().body(response);
            }

            rejectionService.processExternalRejectionResponse(rejectionResponse);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Rejection processed successfully");
            response.put("status", "success");
            response.put("batchCode", rejectionResponse.getBatchCode());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing external rejection for batch: {}", rejectionResponse.getBatchCode(), e);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to process rejection: " + e.getMessage());
            response.put("status", "error");
            response.put("batchCode", rejectionResponse.getBatchCode());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get rejections by dispensing UUID
     */
    @GetMapping("/dispensing/{dispensingUuid}")
    public ResponseEntity<List<DispensingRejection>> getRejectionsByDispensingUuid(
            @PathVariable String dispensingUuid) {
        
        List<DispensingRejection> rejections = rejectionService.getRejectionsByDispensingUuid(dispensingUuid);
        return ResponseEntity.ok(rejections);
    }

    /**
     * Get rejections by claim UUID
     */
    @GetMapping("/claim/{claimUuid}")
    public ResponseEntity<List<DispensingRejection>> getRejectionsByClaimUuid(
            @PathVariable String claimUuid) {
        
        List<DispensingRejection> rejections = rejectionService.getRejectionsByClaimUuid(claimUuid);
        return ResponseEntity.ok(rejections);
    }

    /**
     * Get rejections by batch code
     */
    @GetMapping("/batch/{batchCode}")
    public ResponseEntity<List<DispensingRejection>> getRejectionsByBatchCode(
            @PathVariable String batchCode) {
        
        List<DispensingRejection> rejections = rejectionService.getRejectionsByBatchCode(batchCode);
        return ResponseEntity.ok(rejections);
    }

    /**
     * Get rejections by provider
     */
    @GetMapping("/provider/{providerUuid}")
    public ResponseEntity<Page<DispensingRejection>> getRejectionsByProvider(
            @PathVariable String providerUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<DispensingRejection> allRejections = rejectionService.getRejectionsByProvider(providerUuid);
        Pageable pageable = PageRequest.of(page, size);
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allRejections.size());
        
        Page<DispensingRejection> pageResult = new PageImpl<>(
                allRejections.subList(start, end), pageable, allRejections.size());
        
        return ResponseEntity.ok(pageResult);
    }

    /**
     * Get rejections by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<DispensingRejection>> getRejectionsByStatus(
            @PathVariable String status) {
        
        List<DispensingRejection> rejections = rejectionService.getRejectionsByStatus(status);
        return ResponseEntity.ok(rejections);
    }

    /**
     * Get rejections by category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<DispensingRejection>> getRejectionsByCategory(
            @PathVariable String category) {
        
        List<DispensingRejection> rejections = rejectionService.getRejectionsByCategory(category);
        return ResponseEntity.ok(rejections);
    }

    /**
     * Get resubmittable rejections
     */
    @GetMapping("/resubmittable")
    public ResponseEntity<List<DispensingRejection>> getResubmittableRejections() {
        List<DispensingRejection> rejections = rejectionService.getResubmittableRejections();
        return ResponseEntity.ok(rejections);
    }

    /**
     * Get rejections by date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<DispensingRejection>> getRejectionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<DispensingRejection> rejections = rejectionService.getRejectionsByDateRange(startDate, endDate);
        return ResponseEntity.ok(rejections);
    }

    /**
     * Check if dispensing is rejected
     */
    @GetMapping("/check-rejected/{dispensingUuid}")
    public ResponseEntity<Map<String, Object>> checkDispensingRejected(
            @PathVariable String dispensingUuid) {
        
        boolean isRejected = rejectionService.isDispensingRejected(dispensingUuid);
        
        Map<String, Object> response = new HashMap<>();
        response.put("dispensingUuid", dispensingUuid);
        response.put("isRejected", isRejected);
        response.put("status", isRejected ? "REJECTED" : "NOT_REJECTED");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Mark rejection as resolved
     */
    @PostMapping("/{rejectionUuid}/resolve")
    public ResponseEntity<Map<String, String>> markRejectionAsResolved(
            @PathVariable String rejectionUuid,
            @RequestParam String resolvedBy,
            @RequestParam(required = false) String resolutionComments) {
        
        try {
            rejectionService.markRejectionAsResolved(rejectionUuid, resolvedBy, resolutionComments);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Rejection marked as resolved successfully");
            response.put("status", "success");
            response.put("rejectionUuid", rejectionUuid);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error marking rejection as resolved: {}", rejectionUuid, e);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to mark rejection as resolved: " + e.getMessage());
            response.put("status", "error");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Mark rejection as resubmitted
     */
    @PostMapping("/{rejectionUuid}/resubmit")
    public ResponseEntity<Map<String, String>> markRejectionAsResubmitted(
            @PathVariable String rejectionUuid,
            @RequestParam String resubmittedBy,
            @RequestParam(required = false) String resubmissionComments) {
        
        try {
            rejectionService.markRejectionAsResubmitted(rejectionUuid, resubmittedBy, resubmissionComments);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Rejection marked as resubmitted successfully");
            response.put("status", "success");
            response.put("rejectionUuid", rejectionUuid);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error marking rejection as resubmitted: {}", rejectionUuid, e);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to mark rejection as resubmitted: " + e.getMessage());
            response.put("status", "error");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Appeal a rejection
     */
    @PostMapping("/{rejectionUuid}/appeal")
    public ResponseEntity<Map<String, String>> appealRejection(
            @PathVariable String rejectionUuid,
            @RequestParam String appealedBy,
            @RequestParam String appealComments) {
        
        try {
            rejectionService.appealRejection(rejectionUuid, appealedBy, appealComments);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Rejection appealed successfully");
            response.put("status", "success");
            response.put("rejectionUuid", rejectionUuid);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error appealing rejection: {}", rejectionUuid, e);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to appeal rejection: " + e.getMessage());
            response.put("status", "error");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get rejection statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<DispensingRejectionService.RejectionStatistics> getRejectionStatistics() {
        DispensingRejectionService.RejectionStatistics stats = rejectionService.getRejectionStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get rejection statistics by provider
     */
    @GetMapping("/statistics/provider/{providerUuid}")
    public ResponseEntity<Map<String, Long>> getRejectionStatsByProvider(
            @PathVariable String providerUuid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        Map<String, Long> stats = rejectionService.getRejectionStatsByProvider(providerUuid, startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get rejection details by UUID
     */
    @GetMapping("/{rejectionUuid}")
    public ResponseEntity<DispensingRejection> getRejectionByUuid(@PathVariable String rejectionUuid) {
        DispensingRejection rejection = rejectionService.getRejectionByUuid(rejectionUuid);
        
        if (rejection != null) {
            return ResponseEntity.ok(rejection);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
