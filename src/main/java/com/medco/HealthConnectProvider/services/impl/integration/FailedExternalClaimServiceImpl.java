package com.medco.HealthConnectProvider.services.impl.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medco.HealthConnectProvider.config.ExternalApiConfig;
import com.medco.HealthConnectProvider.dto.ClaimPaySyncRequest;
import com.medco.HealthConnectProvider.entity.integration.FailedExternalClaimLog;
import com.medco.HealthConnectProvider.repository.integration.FailedExternalClaimLogRepository;
import com.medco.HealthConnectProvider.services.integration.FailedExternalClaimService;
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
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FailedExternalClaimServiceImpl implements FailedExternalClaimService {

    private final FailedExternalClaimLogRepository failedClaimRepository;
    private final RestTemplate restTemplate;
    private final ExternalApiConfig externalApiConfig;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void logFailedClaim(ClaimPaySyncRequest request, String claimUuid, String batchCode,
                              String externalApiUrl, String errorMessage, String requestPayload) {
        try {
            FailedExternalClaimLog failedLog = FailedExternalClaimLog.builder()
                    .claimUuid(claimUuid)
                    .batchCode(batchCode)
                    .contractUuid(request.getContractUuid())
                    .providerUuid(request.getProviderUuid())
                    .claimFromDate(request.getClaimFromDate().toString())
                    .claimToDate(request.getClaimToDate().toString())
                    .totalAmount(request.getTotalAmount())
                    .serviceProvidedUuids(objectMapper.writeValueAsString(request.getServiceProvidedUuid()))
                    .externalApiUrl(externalApiUrl)
                    .requestPayload(requestPayload)
                    .errorMessage(errorMessage)
                    .status(Status.ACTIVE)
                    .retryCount(0)
                    .maxRetries(5)
                    .firstFailedAt(LocalDateTime.now())
                    .nextRetryAt(LocalDateTime.now().plusHours(1))
                    .build();

            failedClaimRepository.save(failedLog);
            log.info("Logged failed claim sync for batch: {} with error: {}", batchCode, errorMessage);

        } catch (Exception e) {
            log.error("Failed to log failed claim sync for batch: {}", batchCode, e);
        }
    }

    @Override
    @Transactional
    public void logSuccessfulClaim(ClaimPaySyncRequest request, String claimUuid, String batchCode,
                                  String externalApiUrl, String requestPayload, String response) {
        try {
            FailedExternalClaimLog successLog = FailedExternalClaimLog.builder()
                    .claimUuid(claimUuid)
                    .batchCode(batchCode)
                    .contractUuid(request.getContractUuid())
                    .providerUuid(request.getProviderUuid())
                    .claimFromDate(request.getClaimFromDate().toString())
                    .claimToDate(request.getClaimToDate().toString())
                    .totalAmount(request.getTotalAmount())
                    .serviceProvidedUuids(objectMapper.writeValueAsString(request.getServiceProvidedUuid()))
                    .externalApiUrl(externalApiUrl)
                    .requestPayload(requestPayload)
                    .lastResponse(response)
                    .status(Status.COMPLETED)
                    .retryCount(0)
                    .firstFailedAt(LocalDateTime.now())
                    .succeededAt(LocalDateTime.now())
                    .build();

            failedClaimRepository.save(successLog);
            log.info("Logged successful claim sync for batch: {}", batchCode);

        } catch (Exception e) {
            log.error("Failed to log successful claim sync for batch: {}", batchCode, e);
        }
    }

    @Override
    public List<FailedExternalClaimLog> getAllFailedClaims() {
        return failedClaimRepository.findAll();
    }

    @Override
    public List<FailedExternalClaimLog> getFailedClaimsByStatus(String status) {
        try {
            Status statusEnum = Status.valueOf(status.toUpperCase());
            return failedClaimRepository.findByStatus(statusEnum);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", status);
            return List.of();
        }
    }

    @Override
    public List<FailedExternalClaimLog> getFailedClaimsByClaimUuid(String claimUuid) {
        return failedClaimRepository.findByClaimUuid(claimUuid);
    }

    @Override
    public List<FailedExternalClaimLog> getFailedClaimsByBatchCode(String batchCode) {
        return failedClaimRepository.findByBatchCode(batchCode);
    }

    @Override
    public List<FailedExternalClaimLog> getPendingRetries() {
        return failedClaimRepository.findPendingRetries(Status.ACTIVE, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void retryPendingClaims() {
        List<FailedExternalClaimLog> pendingRetries = getPendingRetries();
        log.info("Found {} pending claim retries", pendingRetries.size());

        for (FailedExternalClaimLog failedLog : pendingRetries) {
            retryClaimSync(failedLog);
        }
    }

    @Override
    @Transactional
    public boolean retrySpecificClaim(String logUuid) {
        return failedClaimRepository.findByLogUuid(logUuid)
                .map(this::retryClaimSync)
                .orElse(false);
    }

    @Override
    @Transactional
    public boolean manualRetry(String logUuid) {
        return retrySpecificClaim(logUuid);
    }

    @Override
    public boolean isClaimAlreadySentSuccessfully(String claimUuid) {
        if (claimUuid == null || claimUuid.trim().isEmpty()) {
            return false;
        }
        return failedClaimRepository.existsByClaimUuidAndStatusCompleted(claimUuid);
    }

    @Override
    public boolean isBatchAlreadySentSuccessfully(String batchCode) {
        if (batchCode == null || batchCode.trim().isEmpty()) {
            return false;
        }
        return failedClaimRepository.existsByBatchCodeAndStatusCompleted(batchCode);
    }

    @Override
    public RetryStatistics getRetryStatistics() {
        long pendingRetries = failedClaimRepository.countByStatus(Status.ACTIVE);
        long maxRetriesReached = failedClaimRepository.findMaxRetriesReached().size();
        long successfulRetries = failedClaimRepository.countByStatus(Status.INACTIVE);
        long totalFailed = pendingRetries + maxRetriesReached + successfulRetries;

        return new RetryStatistics(pendingRetries, maxRetriesReached, successfulRetries, totalFailed);
    }

    @Override
    @Transactional
    public void cleanupOldLogs(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<FailedExternalClaimLog> oldLogs = failedClaimRepository.findByDateRange(
                LocalDateTime.now().minusYears(10), cutoffDate);

        for (FailedExternalClaimLog log : oldLogs) {
            if (log.getStatus() == Status.COMPLETED || log.getStatus() == Status.INACTIVE) {
                log.setDeleted(true);
                failedClaimRepository.save(log);
            }
        }
        log.info("Cleaned up {} old claim logs", oldLogs.size());
    }

    private boolean retryClaimSync(FailedExternalClaimLog failedLog) {
        try {
            log.info("Retrying claim sync for batch: {} (attempt {})", 
                    failedLog.getBatchCode(), failedLog.getRetryCount() + 1);

            // Reconstruct the request from stored data
            ClaimPaySyncRequest request = reconstructRequest(failedLog);
            
            // Make the API call
            String url = failedLog.getExternalApiUrl();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", externalApiConfig.getApiKey());

            HttpEntity<ClaimPaySyncRequest> httpEntity = new HttpEntity<>(request, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, httpEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                // Success - update log
                failedLog.setStatus(Status.COMPLETED);
                failedLog.setSucceededAt(LocalDateTime.now());
                failedLog.setLastResponse(response.getBody());
                failedLog.setLastAttemptAt(LocalDateTime.now());
                
                failedClaimRepository.save(failedLog);
                log.info("Successfully retried claim sync for batch: {}", failedLog.getBatchCode());
                return true;
            } else {
                // Failed - increment retry count
                handleRetryFailure(failedLog, "HTTP " + response.getStatusCode() + ": " + response.getBody());
                return false;
            }

        } catch (Exception e) {
            handleRetryFailure(failedLog, e.getMessage());
            return false;
        }
    }

    private void handleRetryFailure(FailedExternalClaimLog failedLog, String errorMessage) {
        failedLog.setRetryCount(failedLog.getRetryCount() + 1);
        failedLog.setLastAttemptAt(LocalDateTime.now());
        failedLog.setErrorMessage(errorMessage);

        if (failedLog.getRetryCount() >= failedLog.getMaxRetries()) {
            failedLog.setStatus(Status.INACTIVE);
            log.warn("Max retries reached for claim batch: {}", failedLog.getBatchCode());
        } else {
            // Calculate next retry time with exponential backoff
            long delayHours = (long) Math.pow(2, failedLog.getRetryCount());
            failedLog.setNextRetryAt(LocalDateTime.now().plusHours(delayHours));
        }

        failedClaimRepository.save(failedLog);
    }

    private ClaimPaySyncRequest reconstructRequest(FailedExternalClaimLog failedLog) throws Exception {
        ClaimPaySyncRequest request = new ClaimPaySyncRequest();
        request.setContractUuid(failedLog.getContractUuid());
        request.setProviderUuid(failedLog.getProviderUuid());
        request.setClaimFromDate(java.sql.Date.valueOf(failedLog.getClaimFromDate()));
        request.setClaimToDate(java.sql.Date.valueOf(failedLog.getClaimToDate()));
        request.setTotalAmount(failedLog.getTotalAmount());
        request.setBatchCode(failedLog.getBatchCode());
        
        // Parse service provided UUIDs from JSON
        String[] uuids = objectMapper.readValue(failedLog.getServiceProvidedUuids(), String[].class);
        request.setServiceProvidedUuid(List.of(uuids));
        
        return request;
    }
}
