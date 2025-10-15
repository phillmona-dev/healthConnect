package com.medco.HealthConnectProvider.services.impl.integration;

import com.medco.HealthConnectProvider.config.ExternalApiConfig;
import com.medco.HealthConnectProvider.dto.integration.ExternalDispensingRequest;
import com.medco.HealthConnectProvider.entity.integration.FailedExternalDispensingLog;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensingItem;
import com.medco.HealthConnectProvider.repository.integration.FailedExternalDispensingLogRepository;
import com.medco.HealthConnectProvider.services.integration.FailedExternalDispensingService;
import com.medco.HealthConnectProvider.utils.ErrorMessageFormatter;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FailedExternalDispensingServiceImpl implements FailedExternalDispensingService {

    private final FailedExternalDispensingLogRepository failedLogRepository;
    private final RestTemplate restTemplate;
    private final ExternalApiConfig externalApiConfig;

    // External insurance system endpoint for dispensing
    private static final String DISPENSING_ENDPOINT = "/api/payer/claimconnect/service-provided/synchronizeServiceProvided";

    @Override
    @Transactional
    public void logFailedDispensing(MedicationDispensingItem item,
                                  String packageUuid,
                                  String serviceId,
                                  String dispensingUuid,
                                  String contractHeaderUuid,
                                  String externalApiUrl,
                                  String errorMessage,
                                  String lastResponse) {
        
        log.info("Logging failed external dispensing for item: {}", item.getItemUuid());

        // Convert technical error to user-friendly message
        String userFriendlyError = ErrorMessageFormatter.formatErrorMessage(errorMessage, null);
        log.debug("Original error: {}", errorMessage);
        log.debug("User-friendly error: {}", userFriendlyError);

        FailedExternalDispensingLog failedLog = FailedExternalDispensingLog.builder()
                .dispensingItemUuid(item.getItemUuid())
                .dispensingUuid(dispensingUuid)
                .contractHeaderUuid(contractHeaderUuid)
                .serviceId(serviceId)
                .insuredUuid(getInsuredUuid(item))
                .packageUuid(packageUuid)
                .quantity(item.getQuantity() != null ? item.getQuantity().intValue() : 1)
                .totalPrice(item.getTotalPrice() != null ? item.getTotalPrice() : 0.0)
                .providedDate(item.getDispensing().getDispensingDate() != null ?
                             item.getDispensing().getDispensingDate().toString() : "")
                .providerUuid(item.getDispensing().getProviderUuid())
                .externalApiUrl(externalApiUrl)
                .errorMessage(userFriendlyError)  // Use user-friendly error
                .lastResponse(lastResponse)
                .retryCount(0)
                .maxRetries(5)
                .nextRetryAt(calculateNextRetryTime(0))
                .firstFailedAt(LocalDateTime.now())
                .status(Status.ACTIVE)
                .build();
        
        failedLogRepository.save(failedLog);
        log.info("Failed dispensing logged with UUID: {}", failedLog.getLogUuid());
    }

    @Override
    @Transactional
    public void processPendingRetries() {
        log.info("Processing pending retries at: {}", LocalDateTime.now());
        
        List<FailedExternalDispensingLog> pendingRetries = failedLogRepository.findPendingRetries(
                Status.ACTIVE, LocalDateTime.now());
        
        log.info("Found {} pending retries to process", pendingRetries.size());
        
        for (FailedExternalDispensingLog failedLog : pendingRetries) {
            try {
                processRetry(failedLog);
            } catch (Exception e) {
                log.error("Error processing retry for log: {}", failedLog.getLogUuid(), e);
            }
        }
        
        // Mark logs that have reached max retries
        markMaxRetriesReached();
    }

    private void processRetry(FailedExternalDispensingLog failedLog) {
        log.info("Processing retry for log: {}, attempt: {}",
                failedLog.getLogUuid(), failedLog.getRetryCount() + 1);

        try {
            // Build the JSON payload for retry
            ExternalDispensingRequest payload = buildRetryPayload(failedLog);
            String url = externalApiConfig.getExternalApiBaseUrl() + DISPENSING_ENDPOINT + "/" + failedLog.getContractHeaderUuid();

            log.info("Sending POST request to external insurance system: {}", url);
            log.debug("Retry payload: {}", payload);

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", externalApiConfig.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ExternalDispensingRequest> request = new HttpEntity<>(payload, headers);

            // Make the API call
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                // If successful, mark as completed
                failedLog.setStatus(Status.COMPLETED);
                failedLog.setSucceededAt(LocalDateTime.now());
                failedLog.setLastAttemptAt(LocalDateTime.now());
                failedLog.setLastResponse(response.getBody());

                log.info("Retry successful for log: {}", failedLog.getLogUuid());
            } else {
                throw new RuntimeException("HTTP " + response.getStatusCode() + ": " + response.getBody());
            }

        } catch (Exception e) {
            // Retry failed, increment count and schedule next retry
            failedLog.setRetryCount(failedLog.getRetryCount() + 1);
            failedLog.setLastAttemptAt(LocalDateTime.now());

            // Convert technical error to user-friendly message
            String userFriendlyError = ErrorMessageFormatter.formatHttpError(e);
            failedLog.setErrorMessage(userFriendlyError);

            if (failedLog.getRetryCount() < failedLog.getMaxRetries()) {
                failedLog.setNextRetryAt(calculateNextRetryTime(failedLog.getRetryCount()));
                log.warn("Retry {} failed for log: {}. Next retry at: {}",
                        failedLog.getRetryCount(), failedLog.getLogUuid(), failedLog.getNextRetryAt());
            } else {
                log.error("Max retries reached for log: {}", failedLog.getLogUuid());
            }
        }

        failedLogRepository.save(failedLog);
    }

    /**
     * Build external dispensing request payload from failed log for retry
     */
    private ExternalDispensingRequest buildRetryPayload(FailedExternalDispensingLog failedLog) {

        // Create a single item from the failed log
        ExternalDispensingRequest.ExternalDispensingItem item = ExternalDispensingRequest.ExternalDispensingItem.builder()
                .serviceId(failedLog.getServiceId())
                .serviceName("Retry Service") // We don't have service name in failed log
                .serviceCode("") // We don't have service code in failed log
                .qty(failedLog.getQuantity())
                .totalPrice(failedLog.getTotalPrice())
                .recordNumber(failedLog.getDispensingUuid())
                .packageUuid(failedLog.getPackageUuid())
                .build();

        return ExternalDispensingRequest.builder()
                .totalPrice(failedLog.getTotalPrice())
                .providedDate(failedLog.getProvidedDate())
                .serviceProvidedUuid(failedLog.getDispensingUuid())
                .insuredUuid(failedLog.getInsuredUuid())
                .dependentUuid("") // We don't store dependent UUID separately in failed log
                .items(java.util.Arrays.asList(item))
                .build();
    }

    private String getInsuredUuid(MedicationDispensingItem item) {
        if (item.getDispensing().getInsured() != null) {
            return item.getDispensing().getInsured().getInsuredUuid();
        } else if (item.getDispensing().getDependant() != null) {
            return item.getDispensing().getDependant().getDependantUuid();
        } else if (item.getDispensing().getInsuredUuid() != null) {
            return item.getDispensing().getInsuredUuid();
        }
        return "unknown";
    }

    private LocalDateTime calculateNextRetryTime(int retryCount) {
        // Exponential backoff: 1 hour, 2 hours, 4 hours, 8 hours, 16 hours
        int hoursToAdd = (int) Math.pow(2, retryCount);
        return LocalDateTime.now().plusHours(Math.min(hoursToAdd, 24)); // Max 24 hours
    }

    @Override
    public List<FailedExternalDispensingLog> getPendingRetries() {
        return failedLogRepository.findPendingRetries(Status.ACTIVE, LocalDateTime.now());
    }

    @Override
    public List<FailedExternalDispensingLog> getFailedLogsByStatus(String status) {
        return failedLogRepository.findByStatus(Status.valueOf(status.toUpperCase()));
    }

    @Override
    @Transactional
    public void markMaxRetriesReached() {
        List<FailedExternalDispensingLog> maxRetriesLogs = failedLogRepository.findMaxRetriesReached();
        
        for (FailedExternalDispensingLog log : maxRetriesLogs) {
            log.setStatus(Status.INACTIVE); // Mark as inactive (max retries reached)
            failedLogRepository.save(log);
        }
        
        if (!maxRetriesLogs.isEmpty()) {
            log.warn("Marked {} logs as max retries reached", maxRetriesLogs.size());
        }
    }

    @Override
    public RetryStatistics getRetryStatistics() {
        long pendingRetries = failedLogRepository.countByStatus(Status.ACTIVE);
        long maxRetriesReached = failedLogRepository.findMaxRetriesReached().size();
        long successfulRetries = failedLogRepository.countByStatus(Status.INACTIVE);
        long totalFailed = pendingRetries + maxRetriesReached + successfulRetries;

        return new RetryStatistics(pendingRetries, maxRetriesReached, successfulRetries, totalFailed);
    }

    @Override
    public boolean isDispensingUuidAlreadySentSuccessfully(String dispensingUuid) {
        if (dispensingUuid == null || dispensingUuid.trim().isEmpty()) {
            return false;
        }
        return failedLogRepository.existsByDispensingUuidAndStatusCompleted(dispensingUuid);
    }

    @Override
    public List<FailedExternalDispensingLog> getFailedLogsByDispensingUuid(String dispensingUuid) {
        if (dispensingUuid == null || dispensingUuid.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return failedLogRepository.findByDispensingUuid(dispensingUuid);
    }

    @Override
    @Transactional
    public void logSuccessfulDispensing(MedicationDispensingItem item,
                                      String packageUuid,
                                      String serviceId,
                                      String dispensingUuid,
                                      String contractHeaderUuid,
                                      String externalApiUrl,
                                      String successResponse) {

        log.info("Logging successful dispensing send for item: {}, dispensingUuid: {}",
                item.getItemUuid(), dispensingUuid);

        FailedExternalDispensingLog successLog = FailedExternalDispensingLog.builder()
                .dispensingItemUuid(item.getItemUuid())
                .dispensingUuid(dispensingUuid)
                .contractHeaderUuid(contractHeaderUuid)
                .serviceId(serviceId)
                .insuredUuid(getInsuredUuid(item))
                .packageUuid(packageUuid)
                .quantity(item.getQuantity() != null ? item.getQuantity().intValue() : 1)
                .totalPrice(item.getTotalPrice() != null ? item.getTotalPrice() : 0.0)
                .providedDate(item.getDispensing().getDispensingDate() != null ?
                             item.getDispensing().getDispensingDate().toString() : "")
                .providerUuid(item.getDispensing().getProviderUuid())
                .externalApiUrl(externalApiUrl)
                .errorMessage(null) // No error for successful send
                .lastResponse(successResponse)
                .retryCount(0)
                .maxRetries(5)
                .nextRetryAt(LocalDateTime.now()) // Not needed for completed
                .firstFailedAt(LocalDateTime.now()) // Set to current time
                .lastAttemptAt(LocalDateTime.now())
                .succeededAt(LocalDateTime.now()) // Mark as succeeded
                .status(Status.COMPLETED) // Mark as COMPLETED
                .build();

        failedLogRepository.save(successLog);
        log.info("Successfully logged completed dispensing send for dispensingUuid: {}", dispensingUuid);
    }

    @Override
    @Transactional
    public boolean manualRetry(String logUuid) {
        try {
            FailedExternalDispensingLog failedLog = failedLogRepository.findByLogUuid(logUuid)
                    .orElseThrow(() -> new RuntimeException("Failed log not found: " + logUuid));
            
            if (failedLog.getStatus() != Status.ACTIVE) {
                log.warn("Cannot retry log {} - status is not ACTIVE", logUuid);
                return false;
            }
            
            processRetry(failedLog);
            return true;
            
        } catch (Exception e) {
            log.error("Error in manual retry for log: {}", logUuid, e);
            return false;
        }
    }
}
